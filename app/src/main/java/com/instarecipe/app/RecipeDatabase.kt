package com.instarecipe.app

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

@Entity(tableName = "recipes", primaryKeys = ["id"])
data class RecipeEntity(
    val id: Long,
    val title: String,
    val sourceUrl: String,
    val normalizedSourceUrl: String,
    val creator: String,
    val category: String,
    val tagsJson: String,
    val ingredientsJson: String,
    val stepsJson: String,
    val notes: String,
    val favorite: Boolean,
    val cooked: Boolean,
    val status: String,
    val savedDate: String
)

@Dao
interface RecipeDao {
    @Query("SELECT * FROM recipes ORDER BY id DESC")
    fun observeAll(): Flow<List<RecipeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(recipe: RecipeEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(recipes: List<RecipeEntity>)

    @Query("DELETE FROM recipes WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT COUNT(*) FROM recipes")
    suspend fun count(): Int
}

@Database(entities = [RecipeEntity::class], version = 1, exportSchema = false)
abstract class InstaRecipeDatabase : RoomDatabase() {
    abstract fun recipeDao(): RecipeDao

    companion object {
        @Volatile private var instance: InstaRecipeDatabase? = null

        fun get(context: Context): InstaRecipeDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                InstaRecipeDatabase::class.java,
                "instarecipe.db"
            ).build().also { instance = it }
        }
    }
}

class RecipeRepository private constructor(private val dao: RecipeDao) {
    val recipes: Flow<List<Recipe>> = dao.observeAll().map { entities -> entities.map(RecipeEntity::toRecipe) }

    suspend fun upsert(recipe: Recipe) = dao.upsert(recipe.toEntity())
    suspend fun delete(id: Long) = dao.delete(id)

    suspend fun migrateLegacyPreferences(context: Context) {
        val migrationPrefs = context.getSharedPreferences("insta_recipe_migrations", Context.MODE_PRIVATE)
        if (migrationPrefs.getBoolean("room_v1_complete", false)) return

        val legacyPrefs = context.getSharedPreferences("insta_recipe_store", Context.MODE_PRIVATE)
        val raw = legacyPrefs.getString("recipes", null)
        if (dao.count() == 0 && !raw.isNullOrBlank()) {
            val parsed = runCatching {
                val array = JSONArray(raw)
                List(array.length()) { index -> array.getJSONObject(index).toLegacyRecipe().toEntity() }
            }
            if (parsed.isFailure) return
            val migrated = parsed.getOrThrow()
            if (migrated.isNotEmpty()) dao.insertAll(migrated)
        }
        migrationPrefs.edit().putBoolean("room_v1_complete", true).apply()
    }

    companion object {
        @Volatile private var instance: RecipeRepository? = null
        fun get(context: Context): RecipeRepository = instance ?: synchronized(this) {
            instance ?: RecipeRepository(InstaRecipeDatabase.get(context).recipeDao()).also { instance = it }
        }
    }
}

private fun Recipe.toEntity() = RecipeEntity(
    id = id,
    title = title,
    sourceUrl = sourceUrl,
    normalizedSourceUrl = InstagramResolver.extractInstagramUrl(sourceUrl).orEmpty().ifBlank { sourceUrl.trim() },
    creator = creator,
    category = category,
    tagsJson = JSONArray(TagNormalizer.normalizeAll(tags)).toString(),
    ingredientsJson = JSONArray(ingredients).toString(),
    stepsJson = JSONArray(steps).toString(),
    notes = notes,
    favorite = favorite,
    cooked = cooked,
    status = status.name,
    savedDate = savedDate
)

private fun RecipeEntity.toRecipe() = Recipe(
    id, title, sourceUrl, creator, category,
    jsonStrings(tagsJson).let(TagNormalizer::normalizeAll),
    jsonStrings(ingredientsJson), jsonStrings(stepsJson), notes, favorite, cooked,
    runCatching { RecipeStatus.valueOf(status) }.getOrDefault(RecipeStatus.Draft), savedDate
)

private fun JSONObject.toLegacyRecipe() = Recipe(
    id = optLong("id"),
    title = optString("title"),
    sourceUrl = optString("sourceUrl"),
    creator = optString("creator"),
    category = optString("category", "Saved to try"),
    tags = jsonStrings(optJSONArray("tags")?.toString().orEmpty()).let(TagNormalizer::normalizeAll),
    ingredients = jsonStrings(optJSONArray("ingredients")?.toString().orEmpty()),
    steps = jsonStrings(optJSONArray("steps")?.toString().orEmpty()),
    notes = optString("notes"),
    favorite = optBoolean("favorite"),
    cooked = optBoolean("cooked"),
    status = runCatching { RecipeStatus.valueOf(optString("status")) }.getOrDefault(RecipeStatus.Draft),
    savedDate = optString("savedDate")
)

private fun jsonStrings(json: String): List<String> = runCatching {
    val array = JSONArray(json)
    List(array.length()) { array.optString(it) }.filter(String::isNotBlank)
}.getOrDefault(emptyList())

package com.instarecipe.app

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

@Entity(
    tableName = "recipes",
    indices = [Index(value = ["normalizedSourceUrl"], unique = true)]
)
data class RecipeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val sourceUrl: String,
    val normalizedSourceUrl: String?,
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

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(recipe: RecipeEntity): Long

    @Update
    suspend fun update(recipe: RecipeEntity): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(recipes: List<RecipeEntity>)

    @Query("DELETE FROM recipes WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT COUNT(*) FROM recipes")
    suspend fun count(): Int

    @Query("SELECT * FROM recipes WHERE normalizedSourceUrl = :sourceUrl LIMIT 1")
    suspend fun findByNormalizedSourceUrl(sourceUrl: String): RecipeEntity?

    @Query("SELECT * FROM recipes WHERE id = :id LIMIT 1")
    suspend fun findById(id: Long): RecipeEntity?

    @Transaction
    suspend fun insertOrGetBySourceUrl(recipe: RecipeEntity): RecipeEntity {
        val normalizedUrl = recipe.normalizedSourceUrl
        if (normalizedUrl != null) {
            findByNormalizedSourceUrl(normalizedUrl)?.let { return it }
        }

        return try {
            recipe.copy(id = insert(recipe.copy(id = 0)))
        } catch (constraint: SQLiteConstraintException) {
            normalizedUrl?.let { findByNormalizedSourceUrl(it) } ?: throw constraint
        }
    }
}

@Database(entities = [RecipeEntity::class], version = 2, exportSchema = true)
abstract class InstaRecipeDatabase : RoomDatabase() {
    abstract fun recipeDao(): RecipeDao

    companion object {
        @Volatile private var instance: InstaRecipeDatabase? = null

        fun get(context: Context): InstaRecipeDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                InstaRecipeDatabase::class.java,
                "instarecipe.db"
            ).addMigrations(MIGRATION_1_2)
                .build()
                .also { instance = it }
        }

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `recipes_new` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `title` TEXT NOT NULL,
                        `sourceUrl` TEXT NOT NULL,
                        `normalizedSourceUrl` TEXT,
                        `creator` TEXT NOT NULL,
                        `category` TEXT NOT NULL,
                        `tagsJson` TEXT NOT NULL,
                        `ingredientsJson` TEXT NOT NULL,
                        `stepsJson` TEXT NOT NULL,
                        `notes` TEXT NOT NULL,
                        `favorite` INTEGER NOT NULL,
                        `cooked` INTEGER NOT NULL,
                        `status` TEXT NOT NULL,
                        `savedDate` TEXT NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO `recipes_new` (
                        `id`, `title`, `sourceUrl`, `normalizedSourceUrl`, `creator`, `category`,
                        `tagsJson`, `ingredientsJson`, `stepsJson`, `notes`, `favorite`, `cooked`,
                        `status`, `savedDate`
                    )
                    SELECT
                        `id`, `title`, `sourceUrl`, NULLIF(TRIM(`normalizedSourceUrl`), ''),
                        `creator`, `category`, `tagsJson`, `ingredientsJson`, `stepsJson`, `notes`,
                        `favorite`, `cooked`, `status`, `savedDate`
                    FROM `recipes`
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    UPDATE `recipes_new`
                    SET `normalizedSourceUrl` = NULL
                    WHERE `normalizedSourceUrl` IS NOT NULL
                      AND `id` NOT IN (
                          SELECT MIN(`id`)
                          FROM `recipes_new`
                          WHERE `normalizedSourceUrl` IS NOT NULL
                          GROUP BY `normalizedSourceUrl`
                      )
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE `recipes`")
                db.execSQL("ALTER TABLE `recipes_new` RENAME TO `recipes`")
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_recipes_normalizedSourceUrl` " +
                        "ON `recipes` (`normalizedSourceUrl`)"
                )
            }
        }
    }
}

interface RecipeStore {
    val recipes: Flow<List<Recipe>>
    suspend fun upsert(recipe: Recipe): Recipe
    suspend fun update(id: Long, transform: Recipe.() -> Recipe)
    suspend fun delete(id: Long)
    suspend fun findBySourceUrl(sourceUrl: String): Recipe?
    suspend fun createOrGetBySourceUrl(recipe: Recipe): Recipe
    suspend fun migrateLegacyPreferences(context: Context)
}

class RecipeRepository private constructor(private val dao: RecipeDao) : RecipeStore {
    override val recipes: Flow<List<Recipe>> = dao.observeAll().map { entities -> entities.map(RecipeEntity::toRecipe) }

    override suspend fun upsert(recipe: Recipe): Recipe {
        val entity = recipe.toEntity()
        val persistedId = if (entity.id == 0L) {
            dao.insert(entity)
        } else if (dao.update(entity) > 0) {
            entity.id
        } else {
            dao.insert(entity)
        }
        return recipe.copy(id = persistedId)
    }

    override suspend fun createOrGetBySourceUrl(recipe: Recipe): Recipe =
        dao.insertOrGetBySourceUrl(recipe.toEntity()).toRecipe()

    override suspend fun update(id: Long, transform: Recipe.() -> Recipe) {
        dao.findById(id)?.toRecipe()?.let { upsert(it.transform()) }
    }

    override suspend fun delete(id: Long) = dao.delete(id)
    override suspend fun findBySourceUrl(sourceUrl: String): Recipe? {
        val normalized = normalizedSourceUrl(sourceUrl) ?: return null
        return dao.findByNormalizedSourceUrl(normalized)?.toRecipe()
    }

    override suspend fun migrateLegacyPreferences(context: Context) {
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
    normalizedSourceUrl = normalizedSourceUrl(sourceUrl),
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

private fun normalizedSourceUrl(sourceUrl: String): String? =
    InstagramResolver.extractInstagramUrl(sourceUrl)
        ?.trim()
        ?.takeIf(String::isNotBlank)

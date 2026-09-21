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
    val savedDate: String,
    val totalTimeMinutes: Int?,
    val activeTimeMinutes: Int?,
    val yield: String,
    val skillLevel: String,
    val cookPhotoPath: String,
    val cookedAt: String?,
    val dietType: String = DietType.Unknown.name
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

    @Transaction
    suspend fun replaceInstagramRetryIfUnchanged(
        id: Long,
        expectedFingerprint: String,
        replacement: RecipeEntity
    ): Boolean {
        val current = findById(id)?.toRecipe() ?: return false
        if (!current.canExtractFromLinkAgain()) return false
        if (replacement.status != RecipeStatus.Draft.name) return false
        if (current.persistedFingerprint() != expectedFingerprint) return false
        if (replacement.id != id || normalizedSourceUrl(replacement.sourceUrl) != normalizedSourceUrl(current.sourceUrl)) {
            return false
        }
        return update(replacement) == 1
    }
}

@Database(entities = [RecipeEntity::class], version = 5, exportSchema = true)
abstract class InstaRecipeDatabase : RoomDatabase() {
    abstract fun recipeDao(): RecipeDao

    companion object {
        @Volatile private var instance: InstaRecipeDatabase? = null

        fun get(context: Context): InstaRecipeDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                InstaRecipeDatabase::class.java,
                "instarecipe.db"
            ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
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

        /** Adds only optional, source-backed editorial recipe facts; existing recipes stay intact. */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE recipes ADD COLUMN totalTimeMinutes INTEGER")
                db.execSQL("ALTER TABLE recipes ADD COLUMN activeTimeMinutes INTEGER")
                db.execSQL("ALTER TABLE recipes ADD COLUMN `yield` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE recipes ADD COLUMN skillLevel TEXT NOT NULL DEFAULT ''")
            }
        }

        /** Cook photos are private local files; pre-existing recipes have no photo or cook date. */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE recipes ADD COLUMN cookPhotoPath TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE recipes ADD COLUMN cookedAt TEXT")
            }
        }

        /** Stores the explicit Veg/Non-veg classification used by cookbook filters. */
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE recipes ADD COLUMN dietType TEXT NOT NULL DEFAULT 'Unknown'")
            }
        }
    }
}

interface RecipeStore {
    val recipes: Flow<List<Recipe>>
    suspend fun upsert(recipe: Recipe): Recipe
    suspend fun update(id: Long, transform: Recipe.() -> Recipe)
    suspend fun delete(id: Long)
    suspend fun findById(id: Long): Recipe?
    suspend fun findBySourceUrl(sourceUrl: String): Recipe?
    suspend fun createOrGetBySourceUrl(recipe: Recipe): Recipe
    suspend fun completeInstagramRetry(
        id: Long,
        expectedFingerprint: String,
        replacement: Recipe
    ): Boolean
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
    override suspend fun findById(id: Long): Recipe? = dao.findById(id)?.toRecipe()
    override suspend fun findBySourceUrl(sourceUrl: String): Recipe? {
        val normalized = normalizedSourceUrl(sourceUrl) ?: return null
        return dao.findByNormalizedSourceUrl(normalized)?.toRecipe()
    }

    override suspend fun completeInstagramRetry(
        id: Long,
        expectedFingerprint: String,
        replacement: Recipe
    ): Boolean = dao.replaceInstagramRetryIfUnchanged(
        id = id,
        expectedFingerprint = expectedFingerprint,
        replacement = replacement.copy(id = id).toEntity()
    )

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

internal fun Recipe.toEntity() = RecipeEntity(
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
    savedDate = savedDate,
    totalTimeMinutes = totalTimeMinutes,
    activeTimeMinutes = activeTimeMinutes,
    yield = yield,
    skillLevel = skillLevel,
    cookPhotoPath = cookPhotoPath,
    cookedAt = cookedAt,
    dietType = dietType.name
)

internal fun RecipeEntity.toRecipe() = Recipe(
    id = id,
    title = title,
    sourceUrl = sourceUrl,
    creator = creator,
    category = category,
    tags = jsonStrings(tagsJson).let(TagNormalizer::normalizeAll),
    ingredients = jsonStrings(ingredientsJson),
    steps = jsonStrings(stepsJson),
    notes = notes,
    favorite = favorite,
    cooked = cooked,
    status = runCatching { RecipeStatus.valueOf(status) }.getOrDefault(RecipeStatus.Draft),
    savedDate = savedDate,
    totalTimeMinutes = totalTimeMinutes,
    activeTimeMinutes = activeTimeMinutes,
    yield = yield,
    skillLevel = skillLevel,
    cookPhotoPath = cookPhotoPath,
    cookedAt = cookedAt,
    dietType = runCatching { DietType.valueOf(dietType) }.getOrDefault(DietType.Unknown)
)

private fun JSONObject.toLegacyRecipe() = Recipe(
    id = optLong("id"),
    title = optString("title"),
    sourceUrl = optString("sourceUrl"),
    creator = optString("creator"),
    category = optString("category", "Other"),
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

internal fun normalizedSourceUrl(sourceUrl: String): String? =
    InstagramResolver.extractInstagramUrl(sourceUrl)
        ?.trim()
        ?.takeIf(String::isNotBlank)

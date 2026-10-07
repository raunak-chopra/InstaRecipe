package com.instarecipe.app

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.FtsOptions
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.Update
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import java.util.Locale

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
    @ColumnInfo(name = "tagsJson") val tags: List<String>,
    @ColumnInfo(name = "ingredientsJson") val ingredients: List<String>,
    @ColumnInfo(name = "stepsJson") val steps: List<String>,
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

/** Full-text index kept in sync with [RecipeEntity] by Room-generated triggers. */
@Fts4(contentEntity = RecipeEntity::class, tokenizer = FtsOptions.TOKENIZER_UNICODE61)
@Entity(tableName = "recipes_fts")
data class RecipeFtsEntity(
    val title: String,
    val creator: String,
    val category: String,
    val tagsJson: String,
    val ingredientsJson: String,
    val stepsJson: String,
    val notes: String
)

/** Stores string lists as JSON arrays; unreadable legacy values decode to an empty list. */
class StringListConverter {
    @TypeConverter
    fun fromList(values: List<String>): String = RecipeListJson.encodeToString(values)

    @TypeConverter
    fun toList(json: String): List<String> = runCatching {
        RecipeListJson.parseToJsonElement(json).jsonArray.mapNotNull { element ->
            (element as? JsonPrimitive)?.takeUnless { it is JsonNull }?.content
        }.filter(String::isNotBlank)
    }.getOrDefault(emptyList())
}

private val RecipeListJson = Json

@Dao
interface RecipeDao {
    @Query("SELECT * FROM recipes ORDER BY id DESC")
    fun observeAll(): Flow<List<RecipeEntity>>

    @Query("SELECT rowid FROM recipes_fts WHERE recipes_fts MATCH :match")
    fun observeSearchIds(match: String): Flow<List<Long>>

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

@Database(entities = [RecipeEntity::class, RecipeFtsEntity::class], version = 6, exportSchema = true)
@TypeConverters(StringListConverter::class)
abstract class InstaRecipeDatabase : RoomDatabase() {
    abstract fun recipeDao(): RecipeDao

    companion object {
        @Volatile private var instance: InstaRecipeDatabase? = null

        fun get(context: Context): InstaRecipeDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                InstaRecipeDatabase::class.java,
                "instarecipe.db"
            ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
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

        /** Adds the full-text search index over existing recipes; recipe rows are untouched. */
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                FTS_SETUP_SQL.forEach(db::execSQL)
                db.execSQL("INSERT INTO `recipes_fts`(`recipes_fts`) VALUES ('rebuild')")
            }
        }

        // Copied verbatim from the exported v6 schema so the migrated database validates.
        private val FTS_SETUP_SQL = listOf(
            "CREATE VIRTUAL TABLE IF NOT EXISTS `recipes_fts` USING FTS4(`title` TEXT NOT NULL, " +
                "`creator` TEXT NOT NULL, `category` TEXT NOT NULL, `tagsJson` TEXT NOT NULL, " +
                "`ingredientsJson` TEXT NOT NULL, `stepsJson` TEXT NOT NULL, `notes` TEXT NOT NULL, " +
                "tokenize=unicode61, content=`recipes`)",
            "CREATE TRIGGER IF NOT EXISTS room_fts_content_sync_recipes_fts_BEFORE_UPDATE BEFORE UPDATE ON `recipes` " +
                "BEGIN DELETE FROM `recipes_fts` WHERE `docid`=OLD.`rowid`; END",
            "CREATE TRIGGER IF NOT EXISTS room_fts_content_sync_recipes_fts_BEFORE_DELETE BEFORE DELETE ON `recipes` " +
                "BEGIN DELETE FROM `recipes_fts` WHERE `docid`=OLD.`rowid`; END",
            "CREATE TRIGGER IF NOT EXISTS room_fts_content_sync_recipes_fts_AFTER_UPDATE AFTER UPDATE ON `recipes` " +
                "BEGIN INSERT INTO `recipes_fts`(`docid`, `title`, `creator`, `category`, `tagsJson`, " +
                "`ingredientsJson`, `stepsJson`, `notes`) VALUES (NEW.`rowid`, NEW.`title`, NEW.`creator`, " +
                "NEW.`category`, NEW.`tagsJson`, NEW.`ingredientsJson`, NEW.`stepsJson`, NEW.`notes`); END",
            "CREATE TRIGGER IF NOT EXISTS room_fts_content_sync_recipes_fts_AFTER_INSERT AFTER INSERT ON `recipes` " +
                "BEGIN INSERT INTO `recipes_fts`(`docid`, `title`, `creator`, `category`, `tagsJson`, " +
                "`ingredientsJson`, `stepsJson`, `notes`) VALUES (NEW.`rowid`, NEW.`title`, NEW.`creator`, " +
                "NEW.`category`, NEW.`tagsJson`, NEW.`ingredientsJson`, NEW.`stepsJson`, NEW.`notes`); END"
        )
    }
}

interface RecipeStore {
    val recipes: Flow<List<Recipe>>
    /** Ids of recipes matching every term of [query] as a word prefix; empty for a blank query. */
    fun searchIds(query: String): Flow<Set<Long>>
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

    override fun searchIds(query: String): Flow<Set<Long>> {
        val match = ftsMatchQuery(query) ?: return flowOf(emptySet())
        return dao.observeSearchIds(match).map(List<Long>::toSet)
    }

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
                RecipeListJson.parseToJsonElement(raw).jsonArray.map { it.jsonObject.toLegacyRecipe().toEntity() }
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
    tags = TagNormalizer.normalizeAll(tags),
    ingredients = ingredients,
    steps = steps,
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
    tags = TagNormalizer.normalizeAll(tags),
    ingredients = ingredients.filter(String::isNotBlank),
    steps = steps.filter(String::isNotBlank),
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

private fun JsonObject.toLegacyRecipe(): Recipe {
    fun string(key: String, default: String = "") =
        (get(key) as? JsonPrimitive)?.takeUnless { it is JsonNull }?.content ?: default
    fun strings(key: String) = (get(key) as? JsonArray).orEmpty()
        .mapNotNull { (it as? JsonPrimitive)?.takeUnless { value -> value is JsonNull }?.content }
        .filter(String::isNotBlank)
    return Recipe(
        id = string("id").toLongOrNull() ?: 0L,
        title = string("title"),
        sourceUrl = string("sourceUrl"),
        creator = string("creator"),
        category = string("category", "Other"),
        tags = TagNormalizer.normalizeAll(strings("tags")),
        ingredients = strings("ingredients"),
        steps = strings("steps"),
        notes = string("notes"),
        favorite = string("favorite").toBooleanStrictOrNull() ?: false,
        cooked = string("cooked").toBooleanStrictOrNull() ?: false,
        status = runCatching { RecipeStatus.valueOf(string("status")) }.getOrDefault(RecipeStatus.Draft),
        savedDate = string("savedDate")
    )
}

/**
 * Builds an FTS4 MATCH expression requiring every word of [query] as a prefix, or null when the
 * query has no searchable words. Terms are quoted so user input can never inject FTS operators.
 */
internal fun ftsMatchQuery(query: String): String? = query
    .lowercase(Locale.ROOT)
    // Keep combining marks (e.g. Devanagari vowel signs) inside words.
    .split(Regex("""[^\p{L}\p{M}\p{N}]+"""))
    .filter(String::isNotBlank)
    .takeIf(List<String>::isNotEmpty)
    ?.joinToString(" ") { "\"$it\"*" }

internal fun normalizedSourceUrl(sourceUrl: String): String? =
    InstagramResolver.extractInstagramUrl(sourceUrl)
        ?.trim()
        ?.takeIf(String::isNotBlank)

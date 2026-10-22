package com.instarecipe.app

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RecipeDatabaseMigrationTest {
    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        InstaRecipeDatabase::class.java
    )

    @Test
    fun migrate1To4PreservesRowsAndMakesDuplicateUrlsNonDestructive() {
        helper.createDatabase(TEST_DATABASE, 1).apply {
            insertRecipe(1, "First", "https://www.instagram.com/reel/same/")
            insertRecipe(2, "Second", "https://www.instagram.com/reel/same/")
            close()
        }

        helper.runMigrationsAndValidate(
            TEST_DATABASE,
            4,
            true,
            InstaRecipeDatabase.MIGRATION_1_2,
            InstaRecipeDatabase.MIGRATION_2_3,
            InstaRecipeDatabase.MIGRATION_3_4
        ).use { database ->
            database.query("SELECT id, title, normalizedSourceUrl FROM recipes ORDER BY id").use { cursor ->
                assertEquals(2, cursor.count)
                cursor.moveToFirst()
                assertEquals(1L, cursor.getLong(0))
                assertEquals("https://www.instagram.com/reel/same/", cursor.getString(2))
                cursor.moveToNext()
                assertEquals(2L, cursor.getLong(0))
                assertEquals(true, cursor.isNull(2))
            }
        }
    }

    @Test
    fun migrate2To3AddsOptionalEditorialFactsWithoutChangingExistingRows() {
        helper.createDatabase("migration-v2-v3", 2).apply {
            insertRecipe(7, "Sunday supper", "https://www.instagram.com/reel/supper/")
            close()
        }

        helper.runMigrationsAndValidate(
            "migration-v2-v3",
            3,
            true,
            InstaRecipeDatabase.MIGRATION_2_3
        ).use { database ->
            database.query("SELECT title, totalTimeMinutes, activeTimeMinutes, `yield`, skillLevel FROM recipes").use { cursor ->
                cursor.moveToFirst()
                assertEquals("Sunday supper", cursor.getString(0))
                assertEquals(true, cursor.isNull(1))
                assertEquals(true, cursor.isNull(2))
                assertEquals("", cursor.getString(3))
                assertEquals("", cursor.getString(4))
            }
        }
    }

    @Test
    fun migrate3To4AddsPrivateCookPhotoFieldsWithoutChangingEditorialFacts() {
        helper.createDatabase("migration-v3-v4", 3).apply {
            insertRecipe(8, "Weeknight beans", "https://www.instagram.com/reel/beans/")
            close()
        }

        helper.runMigrationsAndValidate(
            "migration-v3-v4",
            4,
            true,
            InstaRecipeDatabase.MIGRATION_3_4
        ).use { database ->
            database.query("SELECT cookPhotoPath, cookedAt FROM recipes").use { cursor ->
                cursor.moveToFirst()
                assertEquals("", cursor.getString(0))
                assertEquals(true, cursor.isNull(1))
            }
        }
    }

    @Test
    fun migrate4To5AddsUnknownDietTypeWithoutChangingExistingRows() {
        helper.createDatabase("migration-v4-v5", 4).apply {
            insertRecipe(9, "Weeknight beans", "https://www.instagram.com/reel/beans/")
            close()
        }

        helper.runMigrationsAndValidate(
            "migration-v4-v5",
            5,
            true,
            InstaRecipeDatabase.MIGRATION_4_5
        ).use { database ->
            database.query("SELECT title, dietType FROM recipes").use { cursor ->
                cursor.moveToFirst()
                assertEquals("Weeknight beans", cursor.getString(0))
                assertEquals("Unknown", cursor.getString(1))
            }
        }
    }

    @Test
    fun migrate5To6IndexesExistingRowsForFullTextSearch() {
        helper.createDatabase("migration-v5-v6", 5).apply {
            insertRecipe(10, "Paneer tikka", "https://www.instagram.com/reel/paneer/")
            execSQL("UPDATE recipes SET ingredientsJson = '[\"yogurt\",\"garam masala\"]' WHERE id = 10")
            insertRecipe(11, "Lemon rice", "https://www.instagram.com/reel/rice/")
            close()
        }

        helper.runMigrationsAndValidate(
            "migration-v5-v6",
            6,
            true,
            InstaRecipeDatabase.MIGRATION_5_6
        ).use { database ->
            database.query("SELECT rowid FROM recipes_fts WHERE recipes_fts MATCH '\"garam\"* \"paneer\"*'").use { cursor ->
                assertEquals(1, cursor.count)
                cursor.moveToFirst()
                assertEquals(10L, cursor.getLong(0))
            }
            // Room's content-sync triggers keep the index current after migration.
            database.execSQL("UPDATE recipes SET title = 'Tamarind rice' WHERE id = 11")
            database.query("SELECT rowid FROM recipes_fts WHERE recipes_fts MATCH '\"tamar\"*'").use { cursor ->
                assertEquals(1, cursor.count)
            }
            database.query("SELECT rowid FROM recipes_fts WHERE recipes_fts MATCH '\"lemon\"*'").use { cursor ->
                assertEquals(0, cursor.count)
            }
        }
    }

    private fun SupportSQLiteDatabase.insertRecipe(id: Long, title: String, normalizedUrl: String) {
        execSQL(
            """
            INSERT INTO recipes (
                id, title, sourceUrl, normalizedSourceUrl, creator, category, tagsJson,
                ingredientsJson, stepsJson, notes, favorite, cooked, status, savedDate
            ) VALUES (?, ?, ?, ?, '', 'Saved to try', '[]', '[]', '[]', '', 0, 0, 'Draft', '2026-09-13')
            """.trimIndent(),
            arrayOf<Any?>(id, title, normalizedUrl, normalizedUrl)
        )
    }

    private companion object {
        const val TEST_DATABASE = "migration-test"
    }
}

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
    fun migrate1To2PreservesRowsAndMakesDuplicateUrlsNonDestructive() {
        helper.createDatabase(TEST_DATABASE, 1).apply {
            insertRecipe(1, "First", "https://www.instagram.com/reel/same/")
            insertRecipe(2, "Second", "https://www.instagram.com/reel/same/")
            close()
        }

        helper.runMigrationsAndValidate(
            TEST_DATABASE,
            2,
            true,
            InstaRecipeDatabase.MIGRATION_1_2
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

package com.instarecipe.app

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class InstagramRetryPersistenceTest {
    private lateinit var database: InstaRecipeDatabase
    private lateinit var dao: RecipeDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, InstaRecipeDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.recipeDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun completionReplacesOnlyUnchangedEligibleTarget() = runTest {
        val original = failedRecipe(id = 0L)
        val id = dao.insert(original.toEntity())
        val persisted = requireNotNull(dao.findById(id)).toRecipe()
        val completed = persisted.copy(
            title = "Extracted soup",
            tags = listOf("Dinner"),
            ingredients = listOf("Tomato"),
            steps = listOf("Simmer"),
            notes = "Serve warm",
            status = RecipeStatus.Draft
        )

        assertTrue(
            dao.replaceInstagramRetryIfUnchanged(
                id = id,
                expectedFingerprint = persisted.persistedFingerprint(),
                replacement = completed.toEntity()
            )
        )
        assertEquals("Extracted soup", dao.findById(id)?.title)
        assertEquals(RecipeStatus.Draft.name, dao.findById(id)?.status)
    }

    @Test
    fun editOrDeletionCausesNoOpAndNeverRecreatesRow() = runTest {
        val id = dao.insert(failedRecipe(id = 0L).toEntity())
        val original = requireNotNull(dao.findById(id)).toRecipe()
        dao.update(original.copy(title = "User edit").toEntity())

        assertFalse(
            dao.replaceInstagramRetryIfUnchanged(
                id = id,
                expectedFingerprint = original.persistedFingerprint(),
                replacement = original.copy(title = "Extracted").toEntity()
            )
        )
        assertEquals("User edit", dao.findById(id)?.title)

        dao.delete(id)
        assertFalse(
            dao.replaceInstagramRetryIfUnchanged(
                id = id,
                expectedFingerprint = original.persistedFingerprint(),
                replacement = original.copy(title = "Extracted").toEntity()
            )
        )
        assertNull(dao.findById(id))
    }

    private fun failedRecipe(id: Long) = Recipe(
        id = id,
        title = "Instagram Recipe Draft",
        sourceUrl = "https://www.instagram.com/reel/test/",
        creator = "",
        category = "Saved to try",
        tags = listOf("Instagram", "Needs review"),
        ingredients = listOf("Keep this"),
        steps = listOf("Keep this step"),
        notes = "Shared caption\n\n---\nImport status: We couldn't create this recipe: Network unavailable",
        favorite = true,
        cooked = false,
        status = RecipeStatus.Saved,
        savedDate = "2026-09-16"
    )
}

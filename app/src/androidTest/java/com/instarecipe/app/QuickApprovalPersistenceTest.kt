package com.instarecipe.app

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class QuickApprovalPersistenceTest {
    private lateinit var database: InstaRecipeDatabase
    private lateinit var dao: RecipeDao

    @Before fun setUp() {
        database = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), InstaRecipeDatabase::class.java)
            .allowMainThreadQueries().build()
        dao = database.recipeDao()
    }

    @After fun tearDown() { database.close() }

    @Test fun approvalPreservesTheSameRowAndCannotRunTwice() = runTest {
        val id = dao.insert(draft().toEntity())
        val expected = requireNotNull(dao.findById(id))
        val approved = requireNotNull(dao.approveDraftIfUnchanged(expected)).toRecipe()
        assertEquals(expected.toRecipe().quickApproved(), approved)
        assertEquals(1, dao.count())
        assertNull(dao.approveDraftIfUnchanged(expected))
        assertEquals(approved, dao.findById(id)?.toRecipe())
    }

    @Test fun staleOrDeletedDraftIsNotOverwrittenOrRecreated() = runTest {
        val id = dao.insert(draft().toEntity())
        val expected = requireNotNull(dao.findById(id))
        val edited = expected.toRecipe().copy(notes = "My newer note", favorite = false).toEntity()
        dao.update(edited)
        assertNull(dao.approveDraftIfUnchanged(expected))
        assertEquals(edited, dao.findById(id))
        dao.delete(id)
        assertNull(dao.approveDraftIfUnchanged(expected))
        assertEquals(0, dao.count())
    }

    @Test fun partialDraftCannotBypassTheQualityGate() = runTest {
        val id = dao.insert(draft().copy(steps = emptyList()).toEntity())
        val expected = requireNotNull(dao.findById(id))
        assertNull(dao.approveDraftIfUnchanged(expected))
        assertEquals(expected, dao.findById(id))
    }

    private fun draft() = Recipe(0, "Tomato soup", "", "Chef", "Dinner", listOf("Needs review", "Soup"),
        listOf("2 tomatoes"), listOf("Simmer the tomatoes."), "Keep chilled.", true, false,
        RecipeStatus.Draft, "2026-10-28", cookPhotoPath = "private-photo.jpg")
}

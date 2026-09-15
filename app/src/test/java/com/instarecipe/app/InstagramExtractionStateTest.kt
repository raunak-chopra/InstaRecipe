package com.instarecipe.app

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class InstagramExtractionStateTest {
    private val sourceUrl = "https://www.instagram.com/reel/test/"

    @Test
    fun successfulBackgroundExtractionMovesRecipeToSavedCookbook() {
        val completed = completedInstagramRecipe(
            pending = recipe(41L, RecipeStatus.Draft, listOf("Instagram", INSTAGRAM_PROCESSING_TAG)),
            extracted = ExtractedRecipeData(
                title = "Soup",
                creator = "Chef",
                category = "Dinner",
                tags = listOf("Quick"),
                ingredients = listOf("Tomato"),
                steps = listOf("Simmer"),
                notes = "Serve warm",
                sourceUrl = sourceUrl
            )
        )

        assertEquals(41L, completed.id)
        assertEquals(RecipeStatus.Saved, completed.status)
        assertEquals(listOf("Quick"), completed.tags)
        assertTrue(INSTAGRAM_PROCESSING_TAG !in completed.tags)
    }

    @Test
    fun failedDraftIsReclaimedWhenLinkIsSharedAgain() = runTest {
        val existing = recipe(8L, RecipeStatus.Draft, listOf("Instagram", "Needs review"))
        val store = ExtractionRecipeStore(existing)

        val pending = claimInstagramExtraction(store, sourceUrl, "shared caption")

        assertEquals(8L, pending?.id)
        assertEquals(RecipeStatus.Draft, pending?.status)
        assertTrue(pending?.tags.orEmpty().contains(INSTAGRAM_PROCESSING_TAG))
        assertEquals("Creating recipe…", pending?.title)
        assertEquals(1, store.upsertCount)
    }

    @Test
    fun savedRecipeIsNotQueuedAgain() = runTest {
        val store = ExtractionRecipeStore(recipe(9L, RecipeStatus.Saved, listOf("Dinner")))

        val pending = claimInstagramExtraction(store, sourceUrl, "shared caption")

        assertNull(pending)
        assertEquals(0, store.upsertCount)
    }

    @Test
    fun failedDraftStillQueuesWhenLinkIsSharedAgain() {
        val failedDraft = recipe(10L, RecipeStatus.Draft, listOf("Instagram", "Needs review"))

        assertTrue(shouldQueueInstagramExtraction(listOf(failedDraft), sourceUrl))
    }

    @Test
    fun savedRecipePreventsDuplicateBackgroundWork() {
        val saved = recipe(11L, RecipeStatus.Saved, listOf("Dinner"))

        assertTrue(!shouldQueueInstagramExtraction(listOf(saved), sourceUrl))
    }

    private fun recipe(id: Long, status: RecipeStatus, tags: List<String>) = Recipe(
        id = id,
        title = "Existing",
        sourceUrl = sourceUrl,
        creator = "",
        category = "Saved to try",
        tags = tags,
        ingredients = emptyList(),
        steps = emptyList(),
        notes = "",
        favorite = false,
        cooked = false,
        status = status,
        savedDate = "2026-09-14"
    )
}

private class ExtractionRecipeStore(initial: Recipe?) : RecipeStore {
    override val recipes: Flow<List<Recipe>> = emptyFlow()
    private var current = initial
    var upsertCount = 0

    override suspend fun upsert(recipe: Recipe): Recipe {
        upsertCount++
        current = recipe
        return recipe
    }

    override suspend fun update(id: Long, transform: Recipe.() -> Recipe) = Unit
    override suspend fun delete(id: Long) = Unit
    override suspend fun findBySourceUrl(sourceUrl: String): Recipe? = current
    override suspend fun createOrGetBySourceUrl(recipe: Recipe): Recipe = recipe.copy(id = 1L).also { current = it }
    override suspend fun migrateLegacyPreferences(context: Context) = Unit
}

package com.instarecipe.app

import org.junit.Assert.*
import org.junit.Test

class RecipePresentationTest {
    private fun draft() = Recipe(1, "Instagram Recipe Draft", "https://www.instagram.com/reel/test/", "", "Other",
        listOf("Instagram", "Needs review"), emptyList(), emptyList(), "", false, false, RecipeStatus.Draft, "2026-10-22")

    @Test fun emptyImportedDraftDoesNotLookReadyOrShowZeroRecipeCounts() {
        val recipe = draft()
        assertEquals(ImportStage.Source, recipe.importStage())
        assertFalse(recipe.importDescription(recipe.importStage()).contains("0 ingredients"))
        assertFalse(recipe.isCookbookReady())
    }

    @Test fun runningImportWinsOverTheMissingConnectionState() {
        assertEquals(ImportStage.Working, draft().copy(tags = listOf("Processing")).importStage(false))
        assertEquals(ImportStage.Working, draft().importStage(false, isRetrying = true))
    }

    @Test fun aMissingKeyHasAnActionableConnectionState() {
        assertEquals(ImportStage.Connection, draft().importStage(false))
    }

    @Test fun extractedContentIsReviewedBeforeItBecomesCookable() {
        val recipe = draft().copy(title = "Tomato pasta", ingredients = listOf("200 g pasta"), steps = listOf("Boil pasta."))
        assertEquals(ImportStage.Review, recipe.importStage())
        assertFalse(recipe.isCookbookReady())
        assertTrue(recipe.copy(status = RecipeStatus.Saved).isCookbookReady())
    }

    @Test fun partialContentRemainsDistinctEvenWhenBothListsHaveSomeItems() {
        val recipe = draft().copy(title = "Pasta", ingredients = listOf("Pasta"), steps = listOf("Cook it."), tags = listOf(PARTIAL_RECIPE_TAG))
        assertEquals(ImportStage.Partial, recipe.importStage())
    }

    @Test fun promotionRemovesOnlyWorkflowTags() {
        assertEquals(listOf("Instagram", "Dinner"), reviewedRecipeTags(listOf("Instagram", "Needs review", "Processing", PARTIAL_RECIPE_TAG, "Dinner")))
    }

    @Test fun importEntryAcceptsRecipeLinksAndRejectsProfilesAndOtherSites() {
        assertEquals("https://www.instagram.com/reel/abc_123/", recipeImportUrl("Try https://www.instagram.com/p/abc_123/?igsh=test"))
        assertNull(recipeImportUrl("https://www.instagram.com/somechef/"))
        assertNull(recipeImportUrl("https://example.com/reel/abc_123/"))
        assertNull(recipeImportUrl("http://www.instagram.com/reel/abc_123/"))
    }

    @Test fun workflowTagsDoNotConsumeTheReviewedRecipesUserTagLimit() {
        val userTags = (1..TagNormalizer.MaxTags).map { "Tag $it" }
        assertEquals(userTags, reviewedRecipeTags(listOf("Processing", "Needs review", PARTIAL_RECIPE_TAG) + userTags))
    }
}

package com.instarecipe.app

import org.junit.Assert.*
import org.junit.Test

class QuickApprovalTest {
    private val draft = Recipe(12, "Tomato soup", "https://www.instagram.com/reel/example/", "Chef", "Dinner",
        listOf("Instagram", "Needs review", "Favorite soup"), listOf("2 tomatoes"), listOf("Simmer the tomatoes."),
        "Keep chilled.", true, false, RecipeStatus.Draft, "2026-10-28", cookPhotoPath = "private-photo.jpg")

    @Test fun completeDraftCanBeApprovedWithoutLosingPersonalFields() {
        val approved = draft.quickApproved()
        assertTrue(approved.isCookbookReady())
        assertEquals(draft.copy(status = RecipeStatus.Saved, tags = listOf("Instagram", "Favorite soup")), approved)
        assertFalse(approved.canQuickApprove())
    }

    @Test fun missingOrPlaceholderEssentialsRequireTheEditor() {
        listOf(draft.copy(title = ""), draft.copy(ingredients = emptyList()), draft.copy(steps = emptyList()),
            draft.copy(title = "Instagram Recipe Draft")).forEach {
            assertFalse(it.canQuickApprove())
            assertThrows(IllegalStateException::class.java) { it.quickApproved() }
        }
    }

    @Test fun partialAndProcessingDraftsCannotBeQuickApprovedEvenWithBothLists() {
        listOf(PARTIAL_RECIPE_TAG, INSTAGRAM_PROCESSING_TAG).forEach {
            assertFalse(draft.copy(tags = draft.tags + it).canQuickApprove())
        }
    }
}

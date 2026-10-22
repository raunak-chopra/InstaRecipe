package com.instarecipe.app

import org.junit.Assert.assertEquals
import org.junit.Test

class GeminiModelTest {
    @Test
    fun applicationUsesOnlyGemini38Flash() {
        assertEquals("gemini-3.8-flash", GeminiRecipeExtractor.GEMINI_MODEL)
    }

    @Test
    fun emptyExtractionIsNotReportedAsUsableRecipeContent() {
        val empty = ExtractedRecipeData(
            title = "Unknown Recipe",
            creator = "Unknown",
            category = "Other",
            tags = emptyList(),
            ingredients = emptyList(),
            steps = emptyList(),
            notes = "",
            sourceUrl = "https://www.instagram.com/reel/test/"
        )

        assertEquals(false, empty.hasUsableRecipeContent())
    }

    @Test
    fun extractionRequiresIngredientsAndSteps() {
        val recipe = ExtractedRecipeData(
            title = "Tomato pasta",
            creator = "Chef",
            category = "Dinner",
            tags = emptyList(),
            ingredients = listOf("Tomatoes"),
            steps = listOf("Simmer until tender"),
            notes = "",
            sourceUrl = "https://www.instagram.com/reel/test/"
        )

        assertEquals(true, recipe.hasUsableRecipeContent())
    }

    @Test
    fun placeholderExtractionIsNotUsable() {
        val recipe = ExtractedRecipeData(
            title = "Untitled Recipe",
            creator = "Unknown",
            category = "Other",
            tags = emptyList(),
            ingredients = listOf("Not provided"),
            steps = listOf("Could not determine"),
            notes = "",
            sourceUrl = "https://www.instagram.com/reel/test/"
        )

        assertEquals(false, recipe.hasUsableRecipeContent())
    }

    @Test
    fun explicitTimePhrasesAreParsedIntoTotalTime() {
        assertEquals(95, parseRecipeTimeMinutes("1 hour 35 minutes"))
        assertEquals(20, parseRecipeTimeMinutes("20 mins"))
        assertEquals(0, parseRecipeTimeMinutes("as needed"))
    }
}

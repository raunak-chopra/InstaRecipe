package com.instarecipe.app

import org.junit.Assert.*
import org.junit.Test

class PartialExtractionTest {
    @Test fun partialExtractionRetainsIngredientsAndNamesWhatIsMissing() {
        val extracted = GeminiRecipeExtractor.parseRecipePayload(
            """{"title":"Tomato pasta","ingredients":["200 g pasta"],"steps":[],"extractionStatus":"partial","missingDetails":["Cooking steps"]}""",
            "https://www.instagram.com/reel/test/", null
        )
        assertEquals(listOf("200 g pasta"), extracted.ingredients)
        assertTrue(extracted.steps.isEmpty())
        assertTrue(PARTIAL_RECIPE_TAG in extracted.tags)
        assertTrue(extracted.notes.contains("Cooking steps"))
        assertFalse(extracted.hasUsableRecipeContent())
    }

    @Test fun aNoRecipeResultStillCannotBePresentedAsExtractedContent() {
        assertThrows(IllegalStateException::class.java) {
            GeminiRecipeExtractor.parseRecipePayload("""{"title":"Unknown","extractionStatus":"no_recipe"}""", "", null)
        }
    }

    @Test fun thePartialMarkerSurvivesTheRecipeTagLimit() {
        val tags = (1..TagNormalizer.MaxTags).joinToString(",") { "\"Tag $it\"" }
        val extracted = GeminiRecipeExtractor.parseRecipePayload(
            """{"title":"Soup","tags":[$tags],"ingredients":["2 carrots"],"steps":["Simmer"],"extractionStatus":"partial","missingDetails":["Cooking time"]}""",
            "", null
        )
        assertTrue(PARTIAL_RECIPE_TAG in extracted.tags)
    }
}

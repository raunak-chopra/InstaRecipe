package com.instarecipe.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TagNormalizerTest {
    @Test
    fun aliasesAndDuplicatesBecomeCanonicalTags() {
        assertEquals(
            listOf("Vegetarian", "High Protein", "Quick"),
            TagNormalizer.normalizeAll(listOf(" veg ", "VEGETARIAN", "protein", "#quick recipe"))
        )
    }

    @Test
    fun parsingSupportsCommasAndLines() {
        assertEquals(
            listOf("Dinner", "Gluten Free", "Vegetarian"),
            TagNormalizer.parse("Dinner, Gluten   Free\nvegetarian")
        )
    }

    @Test
    fun matchingIsCaseInsensitive() {
        assertTrue(TagNormalizer.matches(listOf("High Protein"), "high protein"))
    }
}

package com.instarecipe.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RecipeSearchTest {
    private val pasta = Recipe(
        id = 1,
        title = "Roasted Tomato Pasta",
        sourceUrl = "",
        creator = "Mina",
        category = "Dinner",
        tags = listOf("Vegetarian"),
        ingredients = listOf("garlic", "tomatoes", "rigatoni"),
        steps = listOf("Roast tomatoes", "Toss with pasta"),
        notes = "Prep time: 8 minutes Cook time: 12 minutes",
        favorite = false,
        cooked = false,
        status = RecipeStatus.Saved,
        savedDate = "2026-09-11"
    )

    @Test fun `fts query requires every word as a quoted prefix`() {
        assertEquals("\"tomato\"* \"garlic\"*", ftsMatchQuery("  Tomato, garlic "))
    }

    @Test fun `fts query strips operators so input cannot change the match syntax`() {
        assertEquals("\"near\"* \"or\"* \"x\"*", ftsMatchQuery("NEAR OR \"x*\" -"))
        assertEquals(null, ftsMatchQuery(" *\"- "))
    }

    @Test fun `fts query keeps non latin words`() {
        assertEquals("\"पनीर\"*", ftsMatchQuery("पनीर"))
    }

    @Test fun `blank search keeps list and otherwise preserves list order`() {
        val other = pasta.copy(id = 2)
        val recipes = listOf(other, pasta)
        assertEquals(recipes, recipes.filterBySearch(" ", emptySet()))
        assertEquals(recipes, recipes.filterBySearch("x", setOf(1, 2)))
        assertEquals(listOf(pasta), recipes.filterBySearch("x", setOf(1)))
    }

    @Test fun `quick recipe falls back to times in notes`() {
        assertTrue(isQuickRecipe(pasta))
        assertFalse(isQuickRecipe(pasta.copy(notes = "Cook time: 35 minutes")))
    }

    @Test fun `quick recipe prefers structured total time over notes`() {
        assertFalse(isQuickRecipe(pasta.copy(totalTimeMinutes = 45)))
        assertTrue(isQuickRecipe(pasta.copy(notes = "Cook time: 35 minutes", totalTimeMinutes = 15)))
    }

    @Test fun `home category recognizes meal and diet filters`() {
        assertEquals("dinner", pasta.homeCategoryKey())
        assertTrue(pasta.matchesHomeCategory("category:dinner"))
        assertTrue(pasta.matchesHomeCategory("category:veg"))
        assertFalse(pasta.matchesHomeCategory("category:breakfast"))
    }

    @Test fun `legacy category is displayed as other`() {
        assertEquals("Other", pasta.copy(category = "Saved to try").displayCategory())
    }
}

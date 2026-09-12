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

    @Test fun `search index matches multiple terms across fields`() {
        val results = searchRecipes(buildRecipeSearchIndex(listOf(pasta)), "tomato garlic")
        assertEquals(listOf(pasta), results)
    }

    @Test fun `quick recipe parsing caches one shared regular expression`() {
        assertTrue(isQuickRecipe(pasta))
        assertFalse(isQuickRecipe(pasta.copy(notes = "Cook time: 35 minutes")))
    }
}

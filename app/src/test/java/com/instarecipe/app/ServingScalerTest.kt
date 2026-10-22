package com.instarecipe.app

import com.instarecipe.app.ui.components.ServingScaler
import org.junit.Assert.assertEquals
import org.junit.Test

class ServingScalerTest {
    @Test
    fun scalesMixedFraction() {
        assertEquals("3 cups flour", ServingScaler.scaleIngredient("1 1/2 cups flour", 2f))
    }

    @Test
    fun scalesUnicodeFraction() {
        assertEquals("1.5 cups milk", ServingScaler.scaleIngredient("¾ cups milk", 2f))
    }

    @Test
    fun scalesQuantityRange() {
        assertEquals("2–4 cloves garlic", ServingScaler.scaleIngredient("1-2 cloves garlic", 2f))
    }

    @Test
    fun leavesTextWithoutQuantityUnchanged() {
        assertEquals("salt to taste", ServingScaler.scaleIngredient("salt to taste", 4f))
    }

    @Test
    fun splitsIngredientIntoQuantityUnitAndDescription() {
        val (qty, desc) = ServingScaler.splitIngredientParts("2 tbsp extra-virgin olive oil")
        assertEquals("2 tbsp", qty)
        assertEquals("extra-virgin olive oil", desc)
    }

    @Test
    fun extractsDurationsFromCookingSteps() {
        val mentions = com.instarecipe.app.ui.components.extractStepDurations(
            "Simmer gently for 12 to 15 minutes, then rest for 5 mins before serving."
        )
        assertEquals(2, mentions.size)
        assertEquals("15 min", mentions[0].label)
        assertEquals(900, mentions[0].totalSeconds)
        assertEquals("5 min", mentions[1].label)
        assertEquals(300, mentions[1].totalSeconds)
    }

    @Test
    fun matchesStepIngredientsContextually() {
        val ingredients = listOf(
            "2 tbsp olive oil",
            "4 cloves garlic, minced",
            "400g canned cherry tomatoes"
        )
        val matched = com.instarecipe.app.ui.components.matchIngredientsForStep(
            "Heat the olive oil in a skillet and sauté the garlic until fragrant.",
            ingredients
        )
        assertEquals(listOf("2 tbsp olive oil", "4 cloves garlic, minced"), matched)
    }
}

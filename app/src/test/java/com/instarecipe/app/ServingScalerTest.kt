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
}

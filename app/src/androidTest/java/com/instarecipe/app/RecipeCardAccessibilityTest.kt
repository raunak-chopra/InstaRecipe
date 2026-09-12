package com.instarecipe.app

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import com.instarecipe.app.ui.components.ModernRecipeCard
import com.instarecipe.app.ui.theme.InstaRecipeTheme
import com.instarecipe.app.ui.theme.LocalReducedMotion
import com.instarecipe.app.ui.theme.ThemeMode
import org.junit.Rule
import org.junit.Test

class RecipeCardAccessibilityTest {
    @get:Rule val composeRule = createComposeRule()

    @Test
    fun longContent_remainsAvailableAtTwoHundredPercentFontScale_inDarkReducedMotion() {
        val recipe = Recipe(
            id = 42,
            title = "A deliberately long roasted tomato and caramelized garlic supper",
            sourceUrl = "",
            creator = "Test Kitchen",
            category = "Dinner",
            tags = listOf("Vegetarian", "Weeknight"),
            ingredients = listOf(
                "2 cups very ripe cherry tomatoes, halved lengthwise",
                "6 cloves garlic, thinly sliced"
            ),
            steps = listOf("Roast until deeply caramelized."),
            notes = "Cook time: 20 minutes",
            favorite = false,
            cooked = false,
            status = RecipeStatus.Saved,
            savedDate = "2026-09-11"
        )

        composeRule.setContent {
            InstaRecipeTheme(ThemeMode.Dark) {
                val density = LocalDensity.current
                CompositionLocalProvider(
                    LocalDensity provides Density(density.density, fontScale = 2f),
                    LocalReducedMotion provides true
                ) {
                    ModernRecipeCard(
                        recipe = recipe,
                        onOpen = {},
                        onToggleFavorite = {},
                        onToggleCooked = {},
                        onStartCooking = {}
                    )
                }
            }
        }

        composeRule.onNodeWithText(recipe.title).assertExists()
        composeRule.onNodeWithContentDescription("${recipe.title}. Tap to show main ingredients").performClick()
        composeRule.onNodeWithText(recipe.ingredients.first()).assertExists()
    }
}

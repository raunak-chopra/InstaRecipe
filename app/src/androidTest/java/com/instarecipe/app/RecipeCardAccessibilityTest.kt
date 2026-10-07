package com.instarecipe.app

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.instarecipe.app.ui.components.ModernRecipeCard
import com.instarecipe.app.ui.theme.InstaRecipeTheme
import com.instarecipe.app.ui.theme.LocalReducedMotion
import com.instarecipe.app.ui.theme.ThemeMode
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.atomic.AtomicBoolean

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
        composeRule.onNodeWithContentDescription("${recipe.title}. Open recipe").performClick()
    }

    @Test
    fun retryAction_isVisibleAndInvokableOnImportCard() {
        val retryClicked = AtomicBoolean(false)
        val recipe = Recipe(
            id = 43,
            title = "Instagram Recipe Draft",
            sourceUrl = "https://www.instagram.com/reel/test/",
            creator = "",
            category = "Saved to try",
            tags = listOf("Instagram", "Needs review"),
            ingredients = emptyList(),
            steps = emptyList(),
            notes = "",
            favorite = false,
            cooked = false,
            status = RecipeStatus.Draft,
            savedDate = "2026-09-15"
        )

        composeRule.setContent {
            InstaRecipeTheme(ThemeMode.Light) {
                val density = LocalDensity.current
                CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = 2f)) {
                    ModernRecipeCard(
                        recipe = recipe,
                        onOpen = {},
                        onToggleFavorite = {},
                        onToggleCooked = {},
                        onStartCooking = {},
                        footerActionLabel = "Extract from link again",
                        onFooterAction = { retryClicked.set(true) }
                    )
                }
            }
        }

        composeRule.onNodeWithContentDescription("${recipe.title}. Open recipe").assertExists()
        composeRule.onNode(
            SemanticsMatcher.expectValue(
                SemanticsProperties.StateDescription,
                "Extract from link again"
            ),
            useUnmergedTree = true
        )
            .assertHasClickAction()
            .assertHeightIsAtLeast(48.dp)
            .performClick()
        assertTrue(retryClicked.get())
    }

    @Test
    fun retryProgress_isDisabledAndExposesStateDescription() {
        val recipe = Recipe(
            id = 44,
            title = "Instagram Recipe Draft",
            sourceUrl = "https://www.instagram.com/reel/test/",
            creator = "",
            category = "Saved to try",
            tags = listOf("Instagram", "Needs review"),
            ingredients = emptyList(),
            steps = emptyList(),
            notes = "",
            favorite = false,
            cooked = false,
            status = RecipeStatus.Draft,
            savedDate = "2026-09-16"
        )

        composeRule.setContent {
            InstaRecipeTheme(ThemeMode.Light) {
                ModernRecipeCard(
                    recipe = recipe,
                    onOpen = {},
                    onToggleFavorite = {},
                    onToggleCooked = {},
                    onStartCooking = {},
                    footerActionLabel = "Extracting from link…",
                    onFooterAction = {},
                    footerActionEnabled = false
                )
            }
        }

        composeRule.onNode(
            SemanticsMatcher.expectValue(
                SemanticsProperties.StateDescription,
                "Extracting from link…"
            ),
            useUnmergedTree = true
        )
            .assertExists()
            .assertIsNotEnabled()
    }
}

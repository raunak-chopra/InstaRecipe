package com.instarecipe.app

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.instarecipe.app.ui.screens.RecipeEditor
import com.instarecipe.app.ui.theme.InstaRecipeTheme
import com.instarecipe.app.ui.theme.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.atomic.AtomicReference

class RecipeEditorDraftTest {
    @get:Rule val composeRule = createComposeRule()

    @Test fun leavingAnEditedRecipeOffersToKeepAnIncompleteDraft() {
        val saved = AtomicReference<Recipe?>(null)
        composeRule.setContent {
            InstaRecipeTheme(ThemeMode.Light) {
                RecipeEditor(recipe = null, onCancel = {}, onSave = { saved.set(it) })
            }
        }
        composeRule.onNodeWithText("Title").performTextInput("Weekend soup")
        composeRule.onNodeWithText("Cancel").performClick()
        composeRule.onNodeWithText("Keep your changes?").assertExists()
        composeRule.onNodeWithText("Keep editing").performClick()
        composeRule.onNodeWithText("Weekend soup").assertExists()
        composeRule.onNodeWithText("Save draft").performClick()
        composeRule.waitForIdle()
        assertEquals("Weekend soup", saved.get()?.title)
        assertEquals(RecipeStatus.Draft, saved.get()?.status)
        assertFalse(saved.get()!!.isCookbookReady())
    }

    @Test fun savingAnIncompleteRecipeKeepsEditsAndExplainsTheRecovery() {
        val saved = AtomicReference<Recipe?>(null)
        composeRule.setContent {
            InstaRecipeTheme(ThemeMode.Dark) {
                RecipeEditor(recipe = null, onCancel = {}, onSave = { saved.set(it) })
            }
        }
        composeRule.onNodeWithText("Title").performTextInput("Weekend soup")
        composeRule.onNodeWithText("Save to recipes").performClick()
        composeRule.onNodeWithText("Complete the essentials", substring = true).assertExists()
        composeRule.onNodeWithText("Weekend soup").assertExists()
        assertNull(saved.get())
    }
}

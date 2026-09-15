package com.instarecipe.app

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class VideoExtractionControllerTest {
    @Test
    fun completedRecipeIsRetainedUntilAcknowledged() = runTest {
        val controller = VideoExtractionController(this)
        val persisted = recipe(id = 73L)

        controller.start("Reading video...", "Video extraction failed") { onStatus ->
            onStatus("Uploading video...")
            persisted
        }
        advanceUntilIdle()

        assertFalse(controller.state.value.progress.isExtracting)
        assertEquals(persisted, controller.state.value.completedRecipe)

        controller.acknowledgeCompletedRecipe(73L)
        assertNull(controller.state.value.completedRecipe)
    }

    @Test
    fun duplicateStartIsIgnoredWhileExtractionIsRunning() = runTest {
        val controller = VideoExtractionController(this)
        val release = CompletableDeferred<Unit>()
        var starts = 0

        controller.start("Reading video...", "Video extraction failed") {
            starts++
            release.await()
            recipe(id = 1L)
        }
        advanceUntilIdle()
        controller.start("Reading another video...", "Video extraction failed") {
            starts++
            recipe(id = 2L)
        }

        assertTrue(controller.state.value.progress.isExtracting)
        assertEquals(1, starts)

        release.complete(Unit)
        advanceUntilIdle()
        assertEquals(1L, controller.state.value.completedRecipe?.id)
    }

    @Test
    fun failureIsExposedUntilDismissed() = runTest {
        val controller = VideoExtractionController(this)

        controller.start("Reading video...", "Video analysis failed") {
            error("malformed response")
        }
        advanceUntilIdle()

        assertEquals("Video analysis failed: malformed response", controller.state.value.progress.error)
        controller.dismissError()
        assertNull(controller.state.value.progress.error)
    }

    private fun recipe(id: Long) = Recipe(
        id = id,
        title = "Test",
        sourceUrl = "",
        creator = "",
        category = "Dinner",
        tags = emptyList(),
        ingredients = emptyList(),
        steps = emptyList(),
        notes = "",
        favorite = false,
        cooked = false,
        status = RecipeStatus.Draft,
        savedDate = "2026-09-14"
    )
}

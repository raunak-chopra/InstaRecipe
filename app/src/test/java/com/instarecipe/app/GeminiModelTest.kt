package com.instarecipe.app

import org.junit.Assert.assertEquals
import org.junit.Test

class GeminiModelTest {
    @Test
    fun applicationUsesOnlyGemini38Flash() {
        assertEquals("gemini-3.8-flash", GeminiRecipeExtractor.GEMINI_MODEL)
    }
}

package com.instarecipe.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class InstagramResolverTest {
    @Test
    fun sharedReelUrlIsCanonicalized() {
        assertEquals(
            "https://www.instagram.com/reel/AbC_123-/",
            InstagramResolver.extractInstagramUrl("Try this https://instagram.com/reel/AbC_123-/?igsh=x")
        )
    }

    @Test
    fun unrelatedTextHasNoInstagramUrl() {
        assertNull(InstagramResolver.extractInstagramUrl("A recipe without a social link"))
    }
}

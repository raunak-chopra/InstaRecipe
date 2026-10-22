package com.instarecipe.app

import okhttp3.HttpUrl.Companion.toHttpUrl
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

    @Test
    fun insecureInstagramUrlIsRejected() {
        assertNull(InstagramResolver.extractInstagramUrl("http://instagram.com/reel/AbC_123-/"))
    }

    @Test
    fun publicProfileUrlIsRecognized() {
        assertEquals(true, InstagramResolver.isInstagramProfileUrl("https://www.instagram.com/creator/"))
    }

    @Test
    fun reelUrlIsNotTreatedAsProfile() {
        assertEquals(false, InstagramResolver.isInstagramProfileUrl("https://www.instagram.com/reel/AbC_123-/"))
    }

    @Test
    fun substantiveSharedCaptionUsesFastCaptionOnlyPath() {
        assertEquals(
            true,
            InstagramResolver.shouldPreferCaptionOnly(
                "Mix tomatoes with basil and olive oil, simmer everything for ten minutes, then serve warm with toasted bread."
            )
        )
        assertEquals(false, InstagramResolver.shouldPreferCaptionOnly("Mix tomatoes with basil and serve."))
        assertEquals(false, InstagramResolver.shouldPreferCaptionOnly("https://www.instagram.com/reel/AbC_123-/"))
    }

    @Test
    fun storedImportFailureTextIsNotTreatedAsCaption() {
        val sourceUrl = "https://www.instagram.com/reel/AbC_123-/"
        val notes = "$sourceUrl\n\n---\nImport status: We couldn't create this recipe: Could not connect. Try the link again or open this draft to review it."

        assertNull(InstagramResolver.extractCaptionFromSharedText(notes, sourceUrl))
    }

    @Test
    fun longInstagramUrlDoesNotMakeShortTextQualifyAsCaption() {
        val sharedText = "Looks tasty https://www.instagram.com/reel/AbC_123-/?igsh=abcdefghijklmnopqrstuvwxyz0123456789"

        assertNull(
            InstagramResolver.extractCaptionFromSharedText(
                sharedText,
                "https://www.instagram.com/reel/AbC_123-/"
            )
        )
    }

    @Test
    fun configuredResolverKeepsItsApiPath() {
        val endpoint = "https://resolver.example/api/v1/reels/resolve".toHttpUrl()
        val request = InstagramResolver.configuredResolverRequest(
            endpoint = endpoint,
            targetUrl = "https://www.instagram.com/reel/AbC_123-/",
            profileUrl = "https://www.instagram.com/chef/"
        )

        assertEquals(endpoint, request.url)
        assertEquals("POST", request.method)
        val body = okio.Buffer().also { requireNotNull(request.body).writeTo(it) }.readUtf8()
        assertEquals(true, body.contains("\"url\":\"https://www.instagram.com/reel/AbC_123-/\""))
        assertEquals(true, body.contains("\"profileUrl\":\"https://www.instagram.com/chef/\""))
    }
}

package com.instarecipe.app

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.util.concurrent.TimeUnit

class GeminiApiClientTest {
    private lateinit var server: MockWebServer
    private lateinit var client: GeminiApiClient

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        val httpClient = OkHttpClient.Builder()
            .callTimeout(5, TimeUnit.SECONDS)
            .build()
        client = GeminiApiClient(
            generationClient = httpClient,
            fileClient = httpClient,
            apiRoot = server.url("/"),
            retryDelayMillis = 0,
            filePollDelayMillis = 0,
            maxFilePolls = 3
        )
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun successfulGenerationUsesTypedRequestAndResponse() = runTest {
        server.enqueue(MockResponse().setBody(successResponse("""{"title":"Tomato Soup"}""")))

        val recipe = client.generateRecipe("test-key", "Extract this", null)

        assertEquals("""{"title":"Tomato Soup"}""", recipe)
        val request = server.takeRequest()
        assertEquals("/v1beta/models/gemini-3.8-flash:generateContent", request.path)
        assertEquals("test-key", request.getHeader("x-goog-api-key"))
        val body = request.body.readUtf8()
        assertTrue(body.contains("\"contents\""))
        assertTrue(body.contains("\"responseMimeType\":\"application/json\""))
        assertTrue(body.contains("\"text\":\"Extract this\""))
    }

    @Test
    fun malformedGenerationResponseHasStableError() {
        server.enqueue(MockResponse().setBody("not-json"))

        val error = assertThrows(RuntimeException::class.java) {
            runBlocking { client.generateRecipe("test-key", "Extract", null) }
        }

        assertEquals("Gemini returned malformed generation response JSON.", error.message)
    }

    @Test
    fun httpErrorUsesTypedStatusWithoutLeakingServerMessage() {
        server.enqueue(
            MockResponse().setResponseCode(403).setBody(
                """{"error":{"code":403,"message":"sensitive upstream detail","status":"PERMISSION_DENIED"}}"""
            )
        )

        val error = assertThrows(RuntimeException::class.java) {
            runBlocking { client.generateRecipe("bad-key", "Extract", null) }
        }

        assertEquals("Gemini rejected the API key. Check the key in Settings.", error.message)
        assertFalse(error.message.orEmpty().contains("sensitive"))
        assertEquals(1, server.requestCount)
    }

    @Test
    fun retryableFailureRetriesThenSucceeds() = runTest {
        server.enqueue(MockResponse().setResponseCode(503).setBody("""{"error":{"status":"UNAVAILABLE"}}"""))
        server.enqueue(MockResponse().setBody(successResponse("""{"title":"Recovered"}""")))

        val recipe = client.generateRecipe("test-key", "Extract", null)

        assertEquals("""{"title":"Recovered"}""", recipe)
        assertEquals(2, server.requestCount)
    }

    @Test
    fun rejectedPrimaryKeyFallsBackToBackupKey() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(403)
                .setBody("""{"error":{"status":"PERMISSION_DENIED"}}""")
        )
        server.enqueue(MockResponse().setBody(successResponse("""{"title":"Backup Worked"}""")))
        var fallbackCount = 0

        val recipe = withGeminiKeyFallback(
            apiKeys = listOf("primary-key", "backup-key"),
            onBackupKey = { fallbackCount++ }
        ) { key -> client.generateRecipe(key, "Extract", null) }

        assertEquals("""{"title":"Backup Worked"}""", recipe)
        assertEquals(1, fallbackCount)
        assertEquals("primary-key", server.takeRequest().getHeader("x-goog-api-key"))
        assertEquals("backup-key", server.takeRequest().getHeader("x-goog-api-key"))
    }

    @Test
    fun timedOutFreeKeyFallsBackToPaidKeyWithoutExtraFreeRetries() = runTest {
        val fastTimeoutClient = OkHttpClient.Builder()
            .callTimeout(150, TimeUnit.MILLISECONDS)
            .build()
        val timeoutAwareClient = GeminiApiClient(
            generationClient = fastTimeoutClient,
            fileClient = fastTimeoutClient,
            apiRoot = server.url("/"),
            retryDelayMillis = 0
        )
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE))
        server.enqueue(MockResponse().setBody(successResponse("""{"title":"Paid Key Worked"}""")))

        val recipe = withGeminiKeyFallback(listOf("free-key", "paid-key")) { key ->
            timeoutAwareClient.generateRecipe(
                apiKey = key,
                prompt = "Extract",
                videoFile = null,
                maxGenerationAttempts = 1
            )
        }

        assertEquals("""{"title":"Paid Key Worked"}""", recipe)
        assertEquals(2, server.requestCount)
        assertEquals("free-key", server.takeRequest().getHeader("x-goog-api-key"))
        assertEquals("paid-key", server.takeRequest().getHeader("x-goog-api-key"))
    }

    @Test
    fun videoUploadTimeoutFallsBackToPaidKey() = runTest {
        val fastTimeoutClient = OkHttpClient.Builder()
            .callTimeout(150, TimeUnit.MILLISECONDS)
            .build()
        val timeoutAwareClient = GeminiApiClient(
            generationClient = fastTimeoutClient,
            fileClient = fastTimeoutClient,
            apiRoot = server.url("/"),
            retryDelayMillis = 0,
            filePollDelayMillis = 0
        )
        val video = File.createTempFile("gemini-paid-fallback", ".mp4").apply {
            writeBytes(byteArrayOf(1, 2, 3, 4))
        }
        try {
            server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE))
            server.enqueue(MockResponse().addHeader("X-Goog-Upload-URL", server.url("/paid-upload")))
            server.enqueue(MockResponse().setBody("""{"file":{"name":"files/paid","uri":"gs://paid-video"}}"""))
            server.enqueue(MockResponse().setBody("""{"name":"files/paid","uri":"gs://paid-video","state":"ACTIVE"}"""))
            server.enqueue(MockResponse().setBody(successResponse("""{"title":"Paid Video Worked"}""")))
            server.enqueue(MockResponse().setResponseCode(204))

            val recipe = withGeminiKeyFallback(listOf("free-key", "paid-key")) { key ->
                timeoutAwareClient.generateRecipe(
                    apiKey = key,
                    prompt = "Extract video",
                    videoFile = video,
                    maxGenerationAttempts = 1
                )
            }

            assertEquals("""{"title":"Paid Video Worked"}""", recipe)
            val requests = List(6) { server.takeRequest() }
            assertEquals("free-key", requests[0].getHeader("x-goog-api-key"))
            assertEquals("paid-key", requests[1].getHeader("x-goog-api-key"))
            assertEquals(listOf("POST", "POST", "POST", "GET", "POST", "DELETE"), requests.map { it.method })
        } finally {
            video.delete()
        }
    }

    @Test
    fun invalidRequestDoesNotConsumeBackupKey() {
        server.enqueue(MockResponse().setResponseCode(400).setBody("""{"error":{"status":"INVALID_ARGUMENT"}}"""))

        assertThrows(GeminiApiException::class.java) {
            runBlocking {
                withGeminiKeyFallback(listOf("primary-key", "backup-key")) { key ->
                    client.generateRecipe(key, "Extract", null)
                }
            }
        }

        assertEquals(1, server.requestCount)
    }

    @Test
    fun cancellationCancelsInFlightHttpCall() = runBlocking {
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE))
        val extraction = async(Dispatchers.IO) {
            client.generateRecipe("test-key", "Extract", null)
        }
        assertTrue(server.takeRequest(5, TimeUnit.SECONDS) != null)

        extraction.cancelAndJoin()

        assertTrue(extraction.isCancelled)
    }

    @Test
    fun videoUploadIsPolledUsedAndDeleted() = runTest {
        val video = File.createTempFile("gemini-upload", ".mp4").apply { writeBytes(byteArrayOf(1, 2, 3, 4)) }
        try {
            server.enqueue(
                MockResponse()
                    .addHeader("X-Goog-Upload-URL", server.url("/upload-target"))
            )
            server.enqueue(MockResponse().setBody("""{"file":{"name":"files/test","uri":"gs://video"}}"""))
            server.enqueue(MockResponse().setBody("""{"name":"files/test","uri":"gs://video","state":"ACTIVE"}"""))
            server.enqueue(MockResponse().setBody(successResponse("""{"title":"Video Dish"}""")))
            server.enqueue(MockResponse().setResponseCode(204))
            val statuses = mutableListOf<String>()

            val recipe = client.generateRecipe("test-key", "Extract video", video, statuses::add)

            assertEquals("""{"title":"Video Dish"}""", recipe)
            val requests = List(5) { server.takeRequest() }
            assertEquals(listOf("POST", "POST", "GET", "POST", "DELETE"), requests.map { it.method })
            assertEquals("/upload/v1beta/files", requests[0].path)
            assertEquals("/upload-target", requests[1].path)
            assertEquals("/v1beta/files/test", requests[2].path)
            assertEquals("/v1beta/files/test", requests[4].path)
            assertTrue(requests[3].body.readUtf8().contains("\"fileUri\":\"gs://video\""))
            assertTrue(statuses.first().startsWith("Uploading your video"))
        } finally {
            video.delete()
        }
    }

    @Test
    fun cancellationAfterUploadStillDeletesRemoteFile() = runBlocking {
        val video = File.createTempFile("gemini-cancel-cleanup", ".mp4").apply { writeBytes(byteArrayOf(1)) }
        try {
            server.enqueue(MockResponse().addHeader("X-Goog-Upload-URL", server.url("/upload-target")))
            server.enqueue(MockResponse().setBody("""{"file":{"name":"files/cancel","uri":"gs://video"}}"""))
            server.enqueue(MockResponse().setBody("""{"state":"ACTIVE"}"""))
            server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE))
            server.enqueue(MockResponse().setResponseCode(204))
            val extraction = async(Dispatchers.IO) {
                client.generateRecipe("test-key", "Extract video", video)
            }
            repeat(4) { assertTrue(server.takeRequest(5, TimeUnit.SECONDS) != null) }

            extraction.cancelAndJoin()

            val cleanup = server.takeRequest(5, TimeUnit.SECONDS)
            assertEquals("DELETE", cleanup?.method)
            assertEquals("/v1beta/files/cancel", cleanup?.path)
        } finally {
            video.delete()
        }
    }

    @Test
    fun uploadMissingUriStillDeletesNamedRemoteFile() = runTest {
        val video = File.createTempFile("gemini-missing-uri", ".mp4").apply { writeBytes(byteArrayOf(1)) }
        try {
            server.enqueue(MockResponse().addHeader("X-Goog-Upload-URL", server.url("/upload-target")))
            server.enqueue(MockResponse().setBody("""{"file":{"name":"files/missing-uri"}}"""))
            server.enqueue(MockResponse().setResponseCode(204))

            val error = assertThrows(RuntimeException::class.java) {
                runBlocking { client.generateRecipe("test-key", "Extract video", video) }
            }

            assertEquals("Gemini returned a video upload without a file URI.", error.message)
            val requests = List(3) { server.takeRequest() }
            assertEquals("DELETE", requests.last().method)
            assertEquals("/v1beta/files/missing-uri", requests.last().path)
        } finally {
            video.delete()
        }
    }

    @Test
    fun malformedGenerationAfterUploadStillDeletesRemoteFile() = runTest {
        val video = File.createTempFile("gemini-cleanup", ".mp4").apply { writeBytes(byteArrayOf(1)) }
        try {
            server.enqueue(MockResponse().addHeader("X-Goog-Upload-URL", server.url("/upload-target")))
            server.enqueue(MockResponse().setBody("""{"file":{"name":"files/cleanup","uri":"gs://video"}}"""))
            server.enqueue(MockResponse().setBody("""{"state":"ACTIVE"}"""))
            server.enqueue(MockResponse().setBody("malformed"))
            server.enqueue(MockResponse().setResponseCode(204))

            assertThrows(RuntimeException::class.java) {
                runBlocking { client.generateRecipe("test-key", "Extract video", video) }
            }

            val requests = List(5) { server.takeRequest() }
            assertEquals("DELETE", requests.last().method)
            assertEquals("/v1beta/files/cleanup", requests.last().path)
        } finally {
            video.delete()
        }
    }

    private fun successResponse(recipeJson: String): String {
        val escaped = recipeJson.replace("\\", "\\\\").replace("\"", "\\\"")
        return """{"candidates":[{"content":{"parts":[{"text":"$escaped"}]}}]}"""
    }
}

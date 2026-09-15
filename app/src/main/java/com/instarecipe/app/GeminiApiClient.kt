package com.instarecipe.app

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.IOException
import java.io.InterruptedIOException

internal val GeminiJson = Json {
    ignoreUnknownKeys = true
}

@Serializable
internal data class GenerateContentRequestDto(
    val contents: List<GeminiContentDto>,
    val generationConfig: GenerationConfigDto? = null
)

@Serializable
internal data class GeminiContentDto(
    val parts: List<GeminiPartDto> = emptyList()
)

@Serializable
internal data class GeminiPartDto(
    val text: String? = null,
    val fileData: GeminiFileDataDto? = null
)

@Serializable
internal data class GeminiFileDataDto(
    val mimeType: String,
    val fileUri: String
)

@Serializable
internal data class GenerationConfigDto(
    val responseMimeType: String,
    val temperature: Double
)

@Serializable
internal data class GenerateContentResponseDto(
    val candidates: List<GeminiCandidateDto>? = null,
    @SerialName("output_text") val outputText: String? = null,
    val output: List<LegacyOutputDto>? = null
)

@Serializable
internal data class GeminiCandidateDto(
    val content: GeminiContentDto? = null
)

@Serializable
internal data class LegacyOutputDto(
    val text: String? = null,
    val content: String? = null
)

@Serializable
internal data class RecipePayloadDto(
    val title: String? = null,
    val creator: String? = null,
    val category: String? = null,
    val tags: List<String>? = null,
    val ingredients: List<String>? = null,
    val steps: List<String>? = null,
    val prepTime: String? = null,
    val cookTime: String? = null,
    val notes: String? = null
)

@Serializable
private data class UploadMetadataDto(val file: UploadMetadataFileDto)

@Serializable
private data class UploadMetadataFileDto(val displayName: String)

@Serializable
private data class UploadedFileEnvelopeDto(val file: GeminiFileDto)

@Serializable
private data class GeminiFileDto(
    val name: String? = null,
    val uri: String? = null,
    val state: String? = null
)

@Serializable
private data class GeminiErrorEnvelopeDto(val error: GeminiErrorDto? = null)

@Serializable
private data class GeminiErrorDto(
    val code: Int? = null,
    val message: String? = null,
    val status: String? = null
)

internal class GeminiApiException(
    message: String,
    val mayTryBackupKey: Boolean
) : RuntimeException(message)

internal class GeminiApiClient(
    private val generationClient: OkHttpClient,
    private val fileClient: OkHttpClient,
    private val apiRoot: HttpUrl = GOOGLE_API_ROOT,
    private val retryDelayMillis: Long = 1_000L,
    private val filePollDelayMillis: Long = 1_000L,
    private val maxFilePolls: Int = 90
) {
    private data class UploadedFile(val name: String, val uri: String)

    suspend fun generateRecipe(
        apiKey: String,
        prompt: String,
        videoFile: File?,
        onStatus: (String) -> Unit = {},
        maxGenerationAttempts: Int = DEFAULT_GENERATION_ATTEMPTS
    ): String {
        var uploadedFile: UploadedFile? = null
        try {
            val parts = buildList {
                if (videoFile != null) {
                    val fileSizeMb = videoFile.length() / (1024.0 * 1024.0)
                    onStatus("Uploading your video (${"%.1f".format(fileSizeMb)} MB)…")
                    val uploaded = uploadLargeVideo(apiKey, videoFile)
                    uploadedFile = uploaded
                    add(GeminiPartDto(fileData = GeminiFileDataDto("video/mp4", uploaded.uri)))
                    onStatus("Reading the video, audio, and on-screen instructions…")
                }
                add(GeminiPartDto(text = prompt))
            }
            val requestBody = GenerateContentRequestDto(
                contents = listOf(GeminiContentDto(parts)),
                generationConfig = GenerationConfigDto(
                    responseMimeType = "application/json",
                    temperature = 0.2
                )
            )
            val responseBody = executeGenerationWithRetry(apiKey, requestBody, maxGenerationAttempts)
            return extractGeneratedText(responseBody)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: GeminiApiException) {
            throw failure
        } catch (_: InterruptedIOException) {
            throw GeminiApiException(
                "Video processing took too long.",
                mayTryBackupKey = true
            )
        } catch (_: IOException) {
            throw GeminiApiException(
                "Could not connect. Check your internet connection and try again.",
                mayTryBackupKey = false
            )
        } finally {
            uploadedFile?.let { deleteUploadedFile(apiKey, it.name) }
        }
    }

    suspend fun testConnection(apiKey: String) {
        val body = GenerateContentRequestDto(
            contents = listOf(GeminiContentDto(listOf(GeminiPartDto(text = "Respond with 'OK'"))))
        )
        val request = generationRequest(apiKey, body)
        generationClient.newCall(request).awaitResponse().use { response ->
            val responseBody = response.body.string()
            if (!response.isSuccessful) throw geminiHttpError(response.code, responseBody)
        }
    }

    private suspend fun executeGenerationWithRetry(
        apiKey: String,
        body: GenerateContentRequestDto,
        maxAttempts: Int
    ): String {
        require(maxAttempts > 0) { "At least one generation attempt is required." }
        var lastFailure: RuntimeException? = null
        repeat(maxAttempts) { attempt ->
            try {
                generationClient.newCall(generationRequest(apiKey, body)).awaitResponse().use { response ->
                    val responseBody = response.body.string()
                    if (response.isSuccessful) return responseBody
                    val failure = geminiHttpError(response.code, responseBody)
                    lastFailure = failure
                    if (response.code !in RETRYABLE_HTTP_CODES) throw failure
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: GeminiApiException) {
                throw failure
            } catch (_: InterruptedIOException) {
                lastFailure = GeminiApiException(
                    "Recipe creation took too long.",
                    mayTryBackupKey = true
                )
            } catch (_: IOException) {
                lastFailure = GeminiApiException(
                    "Could not connect. Check your internet connection and try again.",
                    mayTryBackupKey = false
                )
            } catch (_: Exception) {
                lastFailure = RuntimeException("Could not create the recipe. Please try again.")
            }
            if (attempt < maxAttempts - 1) delay(retryDelayMillis * (attempt + 1))
        }
        throw lastFailure ?: RuntimeException("Gemini could not complete the extraction.")
    }
    private fun generationRequest(apiKey: String, body: GenerateContentRequestDto): Request =
        Request.Builder()
            .url(apiUrl("v1beta", "models", "${GeminiRecipeExtractor.GEMINI_MODEL}:generateContent"))
            .addHeader("x-goog-api-key", apiKey)
            .addHeader("Content-Type", "application/json")
            .post(GeminiJson.encodeToString(body).toRequestBody(JSON_MEDIA_TYPE))
            .build()

    private suspend fun uploadLargeVideo(apiKey: String, videoFile: File): UploadedFile {
        val metadata = UploadMetadataDto(UploadMetadataFileDto(videoFile.name))
        val startRequest = Request.Builder()
            .url(apiUrl("upload", "v1beta", "files"))
            .addHeader("x-goog-api-key", apiKey)
            .addHeader("X-Goog-Upload-Protocol", "resumable")
            .addHeader("X-Goog-Upload-Command", "start")
            .addHeader("X-Goog-Upload-Header-Content-Length", videoFile.length().toString())
            .addHeader("X-Goog-Upload-Header-Content-Type", "video/mp4")
            .addHeader("Content-Type", "application/json")
            .post(GeminiJson.encodeToString(metadata).toRequestBody(JSON_MEDIA_TYPE))
            .build()

        val uploadUrl = fileClient.newCall(startRequest).awaitResponse().use { response ->
            val responseBody = response.body.string()
            if (!response.isSuccessful) {
                throw geminiHttpError(response.code, responseBody, "start the video upload")
            }
            response.header("X-Goog-Upload-URL")
                ?: throw RuntimeException("Missing upload URL in Gemini response.")
        }

        val uploadRequest = Request.Builder()
            .url(uploadUrl)
            .addHeader("X-Goog-Upload-Command", "upload, finalize")
            .addHeader("X-Goog-Upload-Offset", "0")
            .addHeader("Content-Length", videoFile.length().toString())
            .post(videoFile.asRequestBody(VIDEO_MEDIA_TYPE))
            .build()

        val uploadedFile = fileClient.newCall(uploadRequest).awaitResponse().use { response ->
            val responseBody = response.body.string()
            if (!response.isSuccessful) {
                throw geminiHttpError(response.code, responseBody, "upload the video")
            }
            decodeOrThrow<UploadedFileEnvelopeDto>(responseBody, "video upload").file
        }
        val uploadedName = uploadedFile.name?.takeIf { it.isNotBlank() }
            ?: throw RuntimeException("Gemini returned a video upload without a file name.")
        val uploaded = try {
            // Validate the server-provided identifier before using it in authenticated requests.
            uploadedFileUrl(uploadedName)
            val uri = uploadedFile.uri?.takeIf { it.isNotBlank() }
                ?: throw RuntimeException("Gemini returned a video upload without a file URI.")
            UploadedFile(uploadedName, uri)
        } catch (cancelled: CancellationException) {
            deleteUploadedFile(apiKey, uploadedName)
            throw cancelled
        } catch (error: Exception) {
            deleteUploadedFile(apiKey, uploadedName)
            throw error
        }

        return try {
            waitUntilFileIsActive(apiKey, uploaded)
        } catch (cancelled: CancellationException) {
            deleteUploadedFile(apiKey, uploaded.name)
            throw cancelled
        } catch (error: Exception) {
            deleteUploadedFile(apiKey, uploaded.name)
            throw error
        }
    }

    private suspend fun waitUntilFileIsActive(apiKey: String, uploaded: UploadedFile): UploadedFile {
        repeat(maxFilePolls) {
            val request = Request.Builder()
                .url(uploadedFileUrl(uploaded.name))
                .addHeader("x-goog-api-key", apiKey)
                .build()
            val state = fileClient.newCall(request).awaitResponse().use { response ->
                val responseBody = response.body.string()
                if (!response.isSuccessful) {
                    throw geminiHttpError(response.code, responseBody, "check uploaded video status")
                }
                decodeOrThrow<GeminiFileDto>(responseBody, "video status").state
            }
            when (state) {
                "ACTIVE" -> return uploaded
                "FAILED" -> throw RuntimeException("Gemini could not process this video.")
            }
            delay(filePollDelayMillis)
        }
        throw GeminiApiException(
            "Video processing took too long. Try a shorter video.",
            mayTryBackupKey = true
        )
    }

    private suspend fun deleteUploadedFile(apiKey: String, name: String) =
        withContext(NonCancellable + Dispatchers.IO) {
            withTimeoutOrNull(CLEANUP_TIMEOUT_MILLIS) {
                try {
                    val request = Request.Builder()
                        .url(uploadedFileUrl(name))
                        .addHeader("x-goog-api-key", apiKey)
                        .delete()
                        .build()
                    fileClient.newCall(request).awaitResponse().close()
                } catch (_: Exception) {
                    // Cleanup is best-effort; response content and credentials are never surfaced.
                }
            }
        }

    private fun apiUrl(vararg pathSegments: String): HttpUrl = apiRoot.newBuilder().apply {
        pathSegments.forEach(::addPathSegment)
    }.build()

    private fun uploadedFileUrl(name: String): HttpUrl {
        val segments = name.split('/').filter { it.isNotBlank() }
        if (segments.size < 2 || segments.first() != "files" || segments.any { it == "." || it == ".." }) {
            throw RuntimeException("Gemini returned an invalid uploaded file name.")
        }
        return apiUrl("v1beta", *segments.toTypedArray())
    }

    private fun extractGeneratedText(rawResponse: String): String {
        val root = decodeOrThrow<GenerateContentResponseDto>(rawResponse, "generation response")
        return when {
            root.candidates != null -> root.candidates.firstOrNull()
                ?.content?.parts?.firstOrNull()?.text.orEmpty()
            root.outputText != null -> root.outputText
            root.output != null -> root.output.firstOrNull()?.let { output ->
                output.text?.takeIf { it.isNotBlank() } ?: output.content.orEmpty()
            }.orEmpty()
            else -> throw RuntimeException("Gemini returned an unsupported response format.")
        }
    }

    private inline fun <reified T> decodeOrThrow(raw: String, label: String): T = try {
        GeminiJson.decodeFromString(raw)
    } catch (_: SerializationException) {
        throw RuntimeException("Gemini returned malformed $label JSON.")
    }

    private fun geminiHttpError(
        statusCode: Int,
        body: String,
        operation: String? = null
    ): GeminiApiException {
        val apiStatus = try {
            GeminiJson.decodeFromString<GeminiErrorEnvelopeDto>(body).error?.status
        } catch (_: SerializationException) {
            null
        }
        val message = when (apiStatus) {
            "UNAUTHENTICATED", "PERMISSION_DENIED" -> "Gemini rejected the API key. Check the key in Settings."
            "RESOURCE_EXHAUSTED" -> "Gemini rate limit reached. Please wait and try again."
            else -> when (statusCode) {
                400 -> "Gemini rejected the extraction request. Try a shorter video or caption."
                401, 403 -> "Gemini rejected the API key. Check the key in Settings."
                404 -> "Gemini 3.8 Flash is not available for this API key."
                408 -> "Gemini timed out while processing the request."
                413 -> "The video is too large for Gemini to process."
                429 -> "Gemini rate limit reached. Please wait and try again."
                in 500..599 -> "Gemini is temporarily unavailable. Please try again."
                else -> operation?.let { "Gemini could not $it (HTTP $statusCode)." }
                    ?: "Gemini request failed (HTTP $statusCode)."
            }
        }
        val mayTryBackup = apiStatus in BACKUP_ELIGIBLE_API_STATUSES ||
            statusCode == 401 || statusCode == 403 || statusCode == 404 || statusCode == 429 || statusCode in 500..599
        return GeminiApiException(message, mayTryBackup)
    }
    private companion object {
        val GOOGLE_API_ROOT = "https://generativelanguage.googleapis.com/".toHttpUrl()
        val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
        val VIDEO_MEDIA_TYPE = "video/mp4".toMediaType()
        const val DEFAULT_GENERATION_ATTEMPTS = 2
        const val CLEANUP_TIMEOUT_MILLIS = 5_000L
        val RETRYABLE_HTTP_CODES = setOf(408, 429, 500, 502, 503, 504)
        val BACKUP_ELIGIBLE_API_STATUSES = setOf(
            "UNAUTHENTICATED", "PERMISSION_DENIED", "RESOURCE_EXHAUSTED", "UNAVAILABLE"
        )
    }
}

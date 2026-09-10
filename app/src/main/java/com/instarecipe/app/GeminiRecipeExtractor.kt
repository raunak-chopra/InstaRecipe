package com.instarecipe.app

import android.content.Context
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

data class ExtractedRecipeData(
    val title: String,
    val creator: String,
    val category: String,
    val tags: List<String>,
    val ingredients: List<String>,
    val steps: List<String>,
    val notes: String,
    val sourceUrl: String
)

object GeminiRecipeExtractor {
    // Primary default model
    const val MODEL_GEMINI_2_5_FLASH = "gemini-2.5-flash"
    const val MODEL_GEMINI_2_0_FLASH = "gemini-2.0-flash"
    const val MODEL_GEMINI_1_5_FLASH = "gemini-1.5-flash"
    const val MODEL_GEMINI_3_1_FLASH_LITE = "gemini-3.1-flash-lite"
    const val MODEL_GEMINI_3_8_FLASH = "gemini-3.8-flash"

    val AVAILABLE_MODELS = listOf(
        MODEL_GEMINI_2_5_FLASH,
        MODEL_GEMINI_2_0_FLASH,
        MODEL_GEMINI_1_5_FLASH,
        MODEL_GEMINI_3_1_FLASH_LITE,
        MODEL_GEMINI_3_8_FLASH
    )

    // Configurable keys in SharedPreferences
    const val PREFS_SETTINGS = "insta_recipe_settings"
    const val KEY_API_KEY = "gemini_api_key"
    const val KEY_SELECTED_MODEL = "gemini_selected_model"
    const val KEY_CUSTOM_RESOLVER = "custom_resolver_url"

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .writeTimeout(90, TimeUnit.SECONDS)
        .build()

    fun getApiKey(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_SETTINGS, Context.MODE_PRIVATE)
        return prefs.getString(KEY_API_KEY, null)?.trim().orEmpty()
    }

    fun setApiKey(context: Context, key: String) {
        context.getSharedPreferences(PREFS_SETTINGS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_API_KEY, key.trim())
            .apply()
    }

    fun getSelectedModel(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_SETTINGS, Context.MODE_PRIVATE)
        return prefs.getString(KEY_SELECTED_MODEL, MODEL_GEMINI_2_5_FLASH)?.trim()
            ?.takeIf { it.isNotBlank() } ?: MODEL_GEMINI_2_5_FLASH
    }

    fun setSelectedModel(context: Context, model: String) {
        context.getSharedPreferences(PREFS_SETTINGS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_SELECTED_MODEL, model.trim())
            .apply()
    }

    fun getCustomResolver(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_SETTINGS, Context.MODE_PRIVATE)
        return prefs.getString(KEY_CUSTOM_RESOLVER, null)?.trim().orEmpty()
    }

    fun setCustomResolver(context: Context, url: String) {
        context.getSharedPreferences(PREFS_SETTINGS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_CUSTOM_RESOLVER, url.trim())
            .apply()
    }

    /**
     * Checks if a string contains actual descriptive recipe text or caption
     * rather than being just a URL or placeholder.
     */
    fun hasSubstantiveRecipeContent(text: String?): Boolean {
        if (text.isNullOrBlank()) return false
        val nonUrlWords = text.lines()
            .flatMap { it.split(Regex("\\s+")) }
            .map { it.trim() }
            .filter { word ->
                word.isNotBlank() &&
                    !word.startsWith("http://", ignoreCase = true) &&
                    !word.startsWith("https://", ignoreCase = true) &&
                    !word.startsWith("www.", ignoreCase = true) &&
                    !word.contains("instagram.com", ignoreCase = true)
            }
        // At least 4 non-URL words and 15 chars of actual description
        return nonUrlWords.size >= 4 && nonUrlWords.joinToString(" ").length >= 15
    }

    /**
     * Extracts structured recipe from a video file or caption text using Gemini AI.
     */
    suspend fun extractRecipe(
        context: Context,
        videoFile: File?,
        textCaption: String?,
        sourceUrl: String,
        creatorName: String?,
        onStatus: (String) -> Unit = {}
    ): ExtractedRecipeData = withContext(Dispatchers.IO) {
        val apiKey = getApiKey(context)
        if (apiKey.isBlank()) {
            throw IllegalStateException("Gemini API key is required. Please enter your free API key in Settings.")
        }

        val hasVideo = videoFile != null && videoFile.exists() && videoFile.length() > 0
        val hasCaption = hasSubstantiveRecipeContent(textCaption)

        // GUARD: Prevent blind hallucination when neither video nor real recipe text is provided
        if (!hasVideo && !hasCaption) {
            throw IllegalStateException(
                "Could not access video stream or caption from Instagram. " +
                "Please attach the saved reel video or paste the post caption to extract the recipe."
            )
        }

        val prompt = buildPrompt(sourceUrl, creatorName, textCaption, hasVideo, hasCaption)
        val partsArray = JSONArray()

        if (hasVideo) {
            val fileSizeMb = videoFile!!.length() / (1024.0 * 1024.0)
            if (fileSizeMb <= 16.0) {
                onStatus("Uploading video directly to Gemini AI (${"%.1f".format(fileSizeMb)} MB)...")
                val bytes = videoFile.readBytes()
                val base64Data = Base64.encodeToString(bytes, Base64.NO_WRAP)

                val inlineData = JSONObject().apply {
                    put("mimeType", "video/mp4")
                    put("data", base64Data)
                }
                partsArray.put(JSONObject().put("inlineData", inlineData))
            } else {
                onStatus("Uploading video (${"%.1f".format(fileSizeMb)} MB) via Gemini File API...")
                val fileUri = uploadLargeVideo(apiKey, videoFile)
                val fileData = JSONObject().apply {
                    put("mimeType", "video/mp4")
                    put("fileUri", fileUri)
                }
                partsArray.put(JSONObject().put("fileData", fileData))
            }
            onStatus("Gemini AI is watching video, listening to audio, and reading on-screen steps...")
        } else {
            onStatus("Gemini AI is reading caption and structuring ingredients & steps...")
        }

        // Add prompt
        partsArray.put(JSONObject().put("text", prompt))

        val preferredModel = getSelectedModel(context)
        val modelsToTry = linkedSetOf(preferredModel).apply { addAll(AVAILABLE_MODELS) }.toList()

        var responseText: String? = null
        var lastErrorMessage = ""

        for (model in modelsToTry) {
            // Standard generateContent endpoint
            val contentPayload = JSONObject().apply {
                put("contents", JSONArray().put(JSONObject().put("parts", partsArray)))
                put("generationConfig", JSONObject().apply {
                    put("responseMimeType", "application/json")
                    put("temperature", 0.2)
                })
            }

            val request = Request.Builder()
                .url("https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey")
                .addHeader("Content-Type", "application/json")
                .post(contentPayload.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
                .build()

            try {
                client.newCall(request).execute().use { response ->
                    val body = response.body?.string().orEmpty()
                    if (response.isSuccessful) {
                        responseText = body
                    } else {
                        lastErrorMessage = try {
                            JSONObject(body).optJSONObject("error")?.optString("message") ?: body
                        } catch (_: Exception) { body }
                    }
                }
            } catch (e: Exception) {
                lastErrorMessage = e.message ?: "Network error"
            }

            if (!responseText.isNullOrBlank()) break
        }

        if (responseText.isNullOrBlank()) {
            throw RuntimeException("Gemini API Error: $lastErrorMessage")
        }

        parseGeminiResponse(responseText!!, sourceUrl, creatorName)
    }

    /**
     * Uploads video file > 16MB via Gemini File API and returns the fileUri.
     */
    private fun uploadLargeVideo(apiKey: String, videoFile: File): String {
        val startUrl = "https://generativelanguage.googleapis.com/upload/v1beta/files?key=$apiKey"
        val metadata = JSONObject().apply {
            put("file", JSONObject().put("displayName", videoFile.name))
        }

        val startRequest = Request.Builder()
            .url(startUrl)
            .addHeader("X-Goog-Upload-Protocol", "resumable")
            .addHeader("X-Goog-Upload-Command", "start")
            .addHeader("X-Goog-Upload-Header-Content-Length", videoFile.length().toString())
            .addHeader("X-Goog-Upload-Header-Content-Type", "video/mp4")
            .addHeader("Content-Type", "application/json")
            .post(metadata.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
            .build()

        val uploadUrl = client.newCall(startRequest).execute().use { res ->
            if (!res.isSuccessful) {
                throw RuntimeException("Failed to initiate video upload: ${res.body?.string()}")
            }
            res.header("X-Goog-Upload-URL") ?: throw RuntimeException("Missing upload URL in Gemini response")
        }

        val uploadRequest = Request.Builder()
            .url(uploadUrl)
            .addHeader("X-Goog-Upload-Command", "upload, finalize")
            .addHeader("X-Goog-Upload-Offset", "0")
            .addHeader("Content-Length", videoFile.length().toString())
            .post(videoFile.asRequestBody("video/mp4".toMediaType()))
            .build()

        return client.newCall(uploadRequest).execute().use { res ->
            val body = res.body?.string().orEmpty()
            if (!res.isSuccessful) {
                throw RuntimeException("Failed to upload video content: $body")
            }
            val json = JSONObject(body)
            json.getJSONObject("file").getString("uri")
        }
    }

    private fun buildPrompt(
        sourceUrl: String,
        creatorName: String?,
        textCaption: String?,
        hasVideo: Boolean,
        hasCaption: Boolean
    ): String {
        return buildString {
            append("You are an expert culinary assistant, recipe extractor, and cookbook organizer.\n")
            if (hasVideo) {
                append("Analyze the attached cooking video thoroughly:\n")
                append("1. Observe the ingredients, quantities, preparations, and cooking techniques shown visually.\n")
                append("2. Listen to the voiceover or spoken audio for instructions, measurements, and chef tips.\n")
                append("3. Read all on-screen text overlays showing ingredient quantities, heat levels, and timings.\n")
                if (hasCaption) {
                    append("4. Corroborate and combine with the post caption provided below.\n\n")
                    append("=== POST CAPTION & NOTES ===\n")
                    append(textCaption?.trim()).append("\n")
                    append("============================\n\n")
                }
            } else {
                append("A video is not attached. You must extract the recipe strictly from the following Instagram post caption and notes:\n\n")
                append("=== POST CAPTION & NOTES ===\n")
                append(textCaption?.trim()).append("\n")
                append("============================\n\n")
                append("CRITICAL INSTRUCTION: Do NOT invent or hallucinate ingredients, measurements, or steps that are not in the caption. ")
                append("Extract the actual dish described. If certain details are omitted by the creator, state what is available rather than making up arbitrary ingredients.\n\n")
            }

            if (!creatorName.isNullOrBlank()) {
                append("Creator: $creatorName\n")
            }
            append("Source link: $sourceUrl\n\n")
            append("Extract the recipe details and return ONLY a valid JSON object matching this schema:\n")
            append("{\n")
            append("  \"title\": \"Appetizing, accurate recipe title\",\n")
            append("  \"creator\": \"${creatorName ?: "Creator name if mentioned in video/caption"}\",\n")
            append("  \"category\": \"Category (e.g. Breakfast, Lunch, Dinner, Snacks, Desserts, Drinks, Quick recipes, Saved to try)\",\n")
            append("  \"tags\": [\"Tag1\", \"Tag2\"],\n")
            append("  \"ingredients\": [\n")
            append("    \"Exact ingredient with quantity and unit (e.g. 200g paneer cubed)\"\n")
            append("  ],\n")
            append("  \"steps\": [\n")
            append("    \"Clear step-by-step cooking instruction\"\n")
            append("  ],\n")
            append("  \"prepTime\": \"e.g. 10 mins\",\n")
            append("  \"cookTime\": \"e.g. 15 mins\",\n")
            append("  \"notes\": \"Chef tips, serving advice, dietary notes\"\n")
            append("}\n")
        }
    }

    private fun parseGeminiResponse(
        rawResponse: String,
        sourceUrl: String,
        fallbackCreator: String?
    ): ExtractedRecipeData {
        val root = JSONObject(rawResponse)
        val textJson = when {
            root.has("candidates") -> {
                val candidates = root.getJSONArray("candidates")
                if (candidates.length() > 0) {
                    val candidate = candidates.getJSONObject(0)
                    val content = candidate.optJSONObject("content")
                    val parts = content?.optJSONArray("parts")
                    if (parts != null && parts.length() > 0) {
                        parts.getJSONObject(0).optString("text")
                    } else ""
                } else ""
            }
            root.has("output_text") -> root.getString("output_text")
            root.has("output") -> {
                val outputArr = root.getJSONArray("output")
                if (outputArr.length() > 0) {
                    val first = outputArr.getJSONObject(0)
                    first.optString("text").ifBlank { first.optString("content") }
                } else ""
            }
            else -> throw RuntimeException("Gemini returned unexpected response format: $rawResponse")
        }

        // Clean any markdown code fences if model wrapped response
        val cleanJson = textJson.trim()
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()

        val recipeJson = JSONObject(cleanJson)

        val title = recipeJson.optString("title", "Instagram Recipe").ifBlank { "Instagram Recipe" }
        val creator = recipeJson.optString("creator", fallbackCreator.orEmpty()).ifBlank { fallbackCreator.orEmpty() }
        val category = recipeJson.optString("category", "Saved to try").ifBlank { "Saved to try" }

        val tagsList = recipeJson.optJSONArray("tags")?.let { arr ->
            List(arr.length()) { arr.optString(it) }.filter { it.isNotBlank() }
        } ?: listOf("Instagram")

        val ingredientsList = recipeJson.optJSONArray("ingredients")?.let { arr ->
            List(arr.length()) { arr.optString(it) }.filter { it.isNotBlank() }
        } ?: emptyList()

        val stepsList = recipeJson.optJSONArray("steps")?.let { arr ->
            List(arr.length()) { arr.optString(it) }.filter { it.isNotBlank() }
        } ?: emptyList()

        val prep = recipeJson.optString("prepTime")
        val cook = recipeJson.optString("cookTime")
        val rawNotes = recipeJson.optString("notes")

        val combinedNotes = buildString {
            if (prep.isNotBlank()) append("Prep Time: $prep\n")
            if (cook.isNotBlank()) append("Cook Time: $cook\n")
            if (rawNotes.isNotBlank()) append(rawNotes)
        }.trim()

        return ExtractedRecipeData(
            title = title,
            creator = creator,
            category = category,
            tags = tagsList,
            ingredients = ingredientsList,
            steps = stepsList,
            notes = combinedNotes,
            sourceUrl = sourceUrl
        )
    }

    /**
     * Quick test to verify if the API key and model are active.
     */
    suspend fun testConnection(apiKey: String, model: String = MODEL_GEMINI_2_5_FLASH): Result<String> = withContext(Dispatchers.IO) {
        try {
            val legacyPayload = JSONObject().apply {
                put("contents", JSONArray().put(JSONObject().put("parts", JSONArray().put(JSONObject().put("text", "Respond with 'OK'")))))
            }
            val request = Request.Builder()
                .url("https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey")
                .addHeader("Content-Type", "application/json")
                .post(legacyPayload.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
                .build()

            client.newCall(request).execute().use { res ->
                val body = res.body?.string().orEmpty()
                if (res.isSuccessful) {
                    Result.success("Connected successfully to $model!")
                } else {
                    val err = runCatching { JSONObject(body).optJSONObject("error")?.optString("message") }.getOrNull() ?: body
                    Result.failure(Exception("Gemini error ($res.code): $err"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

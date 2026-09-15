package com.instarecipe.app

import android.content.Context
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import okhttp3.OkHttpClient
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

internal suspend fun <T> withGeminiKeyFallback(
    apiKeys: List<String>,
    onBackupKey: () -> Unit = {},
    block: suspend (String) -> T
): T {
    require(apiKeys.isNotEmpty()) { "At least one Gemini API key is required." }
    apiKeys.forEachIndexed { index, apiKey ->
        try {
            return block(apiKey)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: GeminiApiException) {
            if (!failure.mayTryBackupKey || index == apiKeys.lastIndex) throw failure
            onBackupKey()
        }
    }
    error("Gemini key fallback exhausted unexpectedly.")
}

object GeminiRecipeExtractor {
    const val GEMINI_MODEL = "gemini-3.8-flash"

    // Configurable keys in SharedPreferences
    const val PREFS_SETTINGS = "insta_recipe_settings"
    const val KEY_API_KEY = "gemini_api_key"
    const val KEY_CUSTOM_RESOLVER = "custom_resolver_url"
    private const val LEGACY_KEY_SELECTED_MODEL = "gemini_selected_model"

    private val generationClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(70, TimeUnit.SECONDS)
        .writeTimeout(45, TimeUnit.SECONDS)
        .callTimeout(75, TimeUnit.SECONDS)
        .build()

    private val connectionTestClient = generationClient.newBuilder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .writeTimeout(8, TimeUnit.SECONDS)
        .callTimeout(12, TimeUnit.SECONDS)
        .build()

    private val fileClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .writeTimeout(3, TimeUnit.MINUTES)
        .callTimeout(5, TimeUnit.MINUTES)
        .build()
    private val apiClient = GeminiApiClient(generationClient, fileClient)
    private val connectionTestApiClient = GeminiApiClient(connectionTestClient, fileClient)

    fun getApiKey(context: Context): String {
        val secureValue = SecurePreferences.getGeminiApiKey(context)
        if (secureValue.isNotBlank()) return secureValue

        // One-time migration from releases that stored the key as plaintext.
        val prefs = context.getSharedPreferences(PREFS_SETTINGS, Context.MODE_PRIVATE)
        if (prefs.contains(LEGACY_KEY_SELECTED_MODEL)) {
            prefs.edit().remove(LEGACY_KEY_SELECTED_MODEL).apply()
        }
        val legacyValue = prefs.getString(KEY_API_KEY, null)?.trim().orEmpty()
        if (legacyValue.isNotBlank()) {
            val migrated = SecurePreferences.setGeminiApiKey(context, legacyValue) &&
                SecurePreferences.getGeminiApiKey(context) == legacyValue
            if (migrated) prefs.edit().remove(KEY_API_KEY).commit()
            return legacyValue.takeIf { migrated }.orEmpty()
        }
        return ""
    }

    fun getBackupApiKey(context: Context): String = SecurePreferences.getGeminiBackupApiKey(context)

    fun getApiKeys(context: Context): List<String> = listOf(getApiKey(context), getBackupApiKey(context))
        .map(String::trim)
        .filter(String::isNotBlank)
        .distinct()

    fun setApiKey(context: Context, key: String): Boolean = SecurePreferences.setGeminiApiKey(context, key)

    fun setApiKeys(context: Context, primary: String, backup: String): Boolean =
        SecurePreferences.setGeminiApiKeys(context, primary, backup)

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
        val apiKeys = getApiKeys(context)
        if (apiKeys.isEmpty()) {
            throw IllegalStateException("A Gemini API key is required. Please enter a primary or backup key in Settings.")
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
        if (!hasVideo) {
            onStatus("Turning the caption into ingredients and steps…")
        }
        val generatedRecipe = withGeminiKeyFallback(
            apiKeys = apiKeys,
            onBackupKey = { onStatus("Free key was unavailable. Trying the paid fallback…") }
        ) { apiKey ->
            apiClient.generateRecipe(
                apiKey = apiKey,
                prompt = prompt,
                videoFile = videoFile.takeIf { hasVideo },
                maxGenerationAttempts = if (apiKeys.size > 1) 1 else 2,
                onStatus = onStatus
            )
        }
        parseRecipePayload(generatedRecipe, sourceUrl, creatorName)
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

    private fun parseRecipePayload(
        rawRecipe: String,
        sourceUrl: String,
        fallbackCreator: String?
    ): ExtractedRecipeData {
        val cleanJson = rawRecipe.trim()
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()
        val recipe = try {
            GeminiJson.decodeFromString<RecipePayloadDto>(cleanJson)
        } catch (_: SerializationException) {
            throw RuntimeException("Gemini returned malformed recipe JSON.")
        }

        val title = recipe.title.orEmpty().ifBlank { "Instagram Recipe" }
        val creator = recipe.creator.orEmpty().ifBlank { fallbackCreator.orEmpty() }
        val category = recipe.category.orEmpty().ifBlank { "Saved to try" }
        val tags = recipe.tags?.filter { it.isNotBlank() } ?: listOf("Instagram")
        val ingredients = recipe.ingredients?.filter { it.isNotBlank() }.orEmpty()
        val steps = recipe.steps?.filter { it.isNotBlank() }.orEmpty()
        val combinedNotes = buildString {
            recipe.prepTime?.takeIf { it.isNotBlank() }?.let { append("Prep Time: $it\n") }
            recipe.cookTime?.takeIf { it.isNotBlank() }?.let { append("Cook Time: $it\n") }
            recipe.notes?.takeIf { it.isNotBlank() }?.let(::append)
        }.trim()

        return ExtractedRecipeData(
            title = title,
            creator = creator,
            category = category,
            tags = TagNormalizer.normalizeAll(tags),
            ingredients = ingredients,
            steps = steps,
            notes = combinedNotes,
            sourceUrl = sourceUrl
        )
    }

    /**
     * Quick test to verify if the API key and model are active.
     */
    suspend fun testConnections(primary: String, backup: String): Result<String> = withContext(Dispatchers.IO) {
        val keyedChecks = listOf(
            "Free key" to primary.trim(),
            "Paid fallback" to backup.trim()
        ).filter { (_, key) -> key.isNotBlank() }.distinctBy { (_, key) -> key }
        if (keyedChecks.isEmpty()) return@withContext Result.failure(IllegalArgumentException("Enter at least one API key."))
        try {
            val results = supervisorScope {
                keyedChecks.map { (label, key) ->
                    async { label to runCatching { connectionTestApiClient.testConnection(key) } }
                }.awaitAll()
            }
            val connected = results.filter { (_, result) -> result.isSuccess }.map { it.first }
            val failed = results.filter { (_, result) -> result.isFailure }
            val message = buildString {
                if (connected.isNotEmpty()) append(connected.joinToString(" and ")).append(" ready.")
                if (failed.isNotEmpty()) {
                    if (isNotEmpty()) append(' ')
                    append(failed.joinToString(" ") { (label, result) ->
                        "$label: ${result.exceptionOrNull()?.message ?: "connection failed"}"
                    })
                }
            }
            if (connected.isNotEmpty()) Result.success(message) else Result.failure(IllegalStateException(message))
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            Result.failure(error)
        }
    }

    suspend fun testConnection(apiKey: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            connectionTestApiClient.testConnection(apiKey)
            Result.success("Key connected successfully.")
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            Result.failure(error)
        }
    }
}

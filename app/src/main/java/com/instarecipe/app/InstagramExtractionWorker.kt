package com.instarecipe.app

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import kotlinx.coroutines.CancellationException
import java.time.LocalDate
import java.util.concurrent.TimeUnit

object InstagramExtractionWork {
    private const val KEY_SOURCE_URL = "source_url"
    private const val KEY_SHARED_TEXT = "shared_text"
    private const val KEY_RECIPE_ID = "recipe_id"

    fun enqueue(context: Context, sourceUrl: String, sharedText: String) {
        val recipeId = System.currentTimeMillis()
        val input = Data.Builder()
            .putString(KEY_SOURCE_URL, sourceUrl)
            .putString(KEY_SHARED_TEXT, sharedText)
            .putLong(KEY_RECIPE_ID, recipeId)
            .build()
        val request = OneTimeWorkRequest.Builder(InstagramExtractionWorker::class.java)
            .setInputData(input)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.SECONDS)
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .build()

        val workName = "instagram-extraction-${sourceUrl.hashCode().toUInt()}"
        WorkManager.getInstance(context.applicationContext)
            .enqueueUniqueWork(workName, ExistingWorkPolicy.KEEP, request)
    }

    internal fun sourceUrl(params: WorkerParameters): String =
        params.inputData.getString(KEY_SOURCE_URL).orEmpty()

    internal fun sharedText(params: WorkerParameters): String =
        params.inputData.getString(KEY_SHARED_TEXT).orEmpty()

    internal fun recipeId(params: WorkerParameters): Long =
        params.inputData.getLong(KEY_RECIPE_ID, System.currentTimeMillis())
}

class InstagramExtractionWorker(
    appContext: Context,
    private val params: WorkerParameters
) : CoroutineWorker(appContext, params) {
    private val repository = RecipeRepository.get(appContext)

    override suspend fun doWork(): Result {
        val sourceUrl = InstagramExtractionWork.sourceUrl(params)
        val sharedText = InstagramExtractionWork.sharedText(params)
        if (sourceUrl.isBlank()) return Result.failure()

        repository.findBySourceUrl(sourceUrl)?.let { existing ->
            if (!existing.tags.contains(PROCESSING_TAG)) return Result.success()
        }

        val recipeId = repository.findBySourceUrl(sourceUrl)?.id
            ?: InstagramExtractionWork.recipeId(params)
        val pending = pendingRecipe(recipeId, sourceUrl, sharedText)
        repository.upsert(pending)

        var videoFile: java.io.File? = null
        try {
            if (GeminiRecipeExtractor.getApiKey(applicationContext).isBlank()) {
                repository.upsert(
                    failedRecipe(
                        pending,
                        sharedText,
                        "Add your Gemini API key in Settings, then open this draft and retry extraction."
                    )
                )
                return Result.failure()
            }

            val customResolver = GeminiRecipeExtractor.getCustomResolver(applicationContext)
                .ifBlank { null }
            val resolution = InstagramResolver.resolveAndDownload(
                context = applicationContext,
                instagramUrl = sourceUrl,
                customResolverUrl = customResolver,
                supplementaryText = sharedText
            )
            videoFile = resolution.videoFile
            val hasVideo = videoFile?.let { it.exists() && it.length() > 0 } == true
            val caption = resolution.caption
                .takeIf(GeminiRecipeExtractor::hasSubstantiveRecipeContent)
                ?: InstagramResolver.extractCaptionFromSharedText(sharedText, sourceUrl)

            if (!hasVideo && caption.isNullOrBlank()) {
                repository.upsert(
                    failedRecipe(
                        pending.copy(creator = resolution.creator.orEmpty()),
                        sharedText,
                        "Instagram did not provide an accessible video or recipe caption. Attach the saved reel video to this draft."
                    )
                )
                return Result.failure()
            }

            val extracted = GeminiRecipeExtractor.extractRecipe(
                context = applicationContext,
                videoFile = videoFile,
                textCaption = caption,
                sourceUrl = sourceUrl,
                creatorName = resolution.creator
            )
            repository.upsert(
                Recipe(
                    id = recipeId,
                    title = extracted.title,
                    sourceUrl = extracted.sourceUrl,
                    creator = extracted.creator,
                    category = extracted.category,
                    tags = extracted.tags,
                    ingredients = extracted.ingredients,
                    steps = extracted.steps,
                    notes = extracted.notes,
                    favorite = false,
                    cooked = false,
                    status = RecipeStatus.Draft,
                    savedDate = pending.savedDate
                )
            )
            return Result.success()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            if (runAttemptCount < 2) return Result.retry()
            repository.upsert(
                failedRecipe(
                    pending,
                    sharedText,
                    "Automatic extraction failed: ${error.message ?: "Unknown error"}. Open this draft to retry."
                )
            )
            return Result.failure()
        } finally {
            videoFile?.delete()
            InstagramResolver.cleanCachedReelVideos(applicationContext)
        }
    }

    private fun pendingRecipe(id: Long, sourceUrl: String, sharedText: String) = Recipe(
        id = id,
        title = "Distilling Instagram reel…",
        sourceUrl = sourceUrl,
        creator = "",
        category = "Saved to try",
        tags = listOf("Instagram", PROCESSING_TAG),
        ingredients = emptyList(),
        steps = emptyList(),
        notes = sharedText,
        favorite = false,
        cooked = false,
        status = RecipeStatus.Draft,
        savedDate = LocalDate.now().toString()
    )

    private fun failedRecipe(recipe: Recipe, sharedText: String, message: String) = recipe.copy(
        title = recipe.creator.takeIf { it.isNotBlank() }
            ?.let { "$it's Recipe Draft" }
            ?: "Instagram Recipe Draft",
        tags = listOf("Instagram", "Needs review"),
        notes = buildString {
            append(sharedText)
            if (sharedText.isNotBlank()) append("\n\n")
            append("[$message]")
        }
    )

    private companion object {
        const val PROCESSING_TAG = "Processing"
    }
}

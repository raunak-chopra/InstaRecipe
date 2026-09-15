package com.instarecipe.app

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.Operation
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import java.security.MessageDigest
import java.time.LocalDate
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

internal const val INSTAGRAM_PROCESSING_TAG = "Processing"

object InstagramExtractionWork {
    private const val KEY_SOURCE_URL = "source_url"
    private const val KEY_SHARED_TEXT = "shared_text"

    suspend fun enqueue(context: Context, sourceUrl: String, sharedText: String) {
        val input = Data.Builder()
            .putString(KEY_SOURCE_URL, sourceUrl)
            .putString(KEY_SHARED_TEXT, sharedText)
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

        val workName = "instagram-extraction-${sourceUrl.sha256()}"
        val operation = WorkManager.getInstance(context.applicationContext)
            .enqueueUniqueWork(workName, ExistingWorkPolicy.KEEP, request)
        operation.awaitPersistence()
    }

    private suspend fun Operation.awaitPersistence() {
        suspendCancellableCoroutine { continuation ->
            val future = result
            future.addListener(
                {
                    try {
                        future.get()
                        if (continuation.isActive) continuation.resume(Unit)
                    } catch (error: ExecutionException) {
                        if (continuation.isActive) {
                            continuation.resumeWithException(error.cause ?: error)
                        }
                    } catch (error: Exception) {
                        if (continuation.isActive) continuation.resumeWithException(error)
                    }
                },
                Executor { command -> command.run() }
            )
        }
    }

    internal fun sourceUrl(params: WorkerParameters): String =
        params.inputData.getString(KEY_SOURCE_URL).orEmpty()

    internal fun sharedText(params: WorkerParameters): String =
        params.inputData.getString(KEY_SHARED_TEXT).orEmpty()

    private fun String.sha256(): String = MessageDigest.getInstance("SHA-256")
        .digest(toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it) }
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

        val pending = claimInstagramExtraction(repository, sourceUrl, sharedText)
            ?: return Result.success()

        var videoFile: java.io.File? = null
        try {
            if (GeminiRecipeExtractor.getApiKeys(applicationContext).isEmpty()) {
                repository.upsert(
                    failedInstagramRecipe(
                        pending,
                        sharedText,
                        "Connect a free or paid recipe-creation key in Settings, then share this link again."
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
                    failedInstagramRecipe(
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
            repository.upsert(completedInstagramRecipe(pending, extracted))
            return Result.success()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            // GeminiApiClient already owns bounded model retries and paid-key fallback.
            // WorkManager retries resolver/network failures only, avoiding repeated uploads and generation charges.
            if (runAttemptCount < 1 && error !is GeminiApiException && error.isRetryable()) return Result.retry()
            repository.upsert(
                failedInstagramRecipe(
                    pending,
                    sharedText,
                    "We couldn't create this recipe: ${error.safeUserMessage()} Share the link again or open this draft to retry."
                )
            )
            return Result.failure()
        } finally {
            videoFile?.delete()
            InstagramResolver.cleanCachedReelVideos(applicationContext)
        }
    }
}

internal suspend fun claimInstagramExtraction(
    repository: RecipeStore,
    sourceUrl: String,
    sharedText: String
): Recipe? {
    val existing = repository.findBySourceUrl(sourceUrl)
    if (existing?.status == RecipeStatus.Saved) return null

    val pending = pendingInstagramRecipe(
        id = existing?.id ?: 0L,
        sourceUrl = sourceUrl,
        sharedText = sharedText,
        savedDate = existing?.savedDate ?: LocalDate.now().toString()
    )
    return if (existing == null) repository.createOrGetBySourceUrl(pending) else repository.upsert(pending)
}

internal fun completedInstagramRecipe(pending: Recipe, extracted: ExtractedRecipeData) = Recipe(
    id = pending.id,
    title = extracted.title,
    sourceUrl = extracted.sourceUrl,
    creator = extracted.creator,
    category = extracted.category,
    tags = extracted.tags,
    ingredients = extracted.ingredients,
    steps = extracted.steps,
    notes = extracted.notes,
    favorite = pending.favorite,
    cooked = pending.cooked,
    status = RecipeStatus.Saved,
    savedDate = pending.savedDate
)

private fun pendingInstagramRecipe(
    id: Long,
    sourceUrl: String,
    sharedText: String,
    savedDate: String
) = Recipe(
    id = id,
    title = "Creating recipe…",
    sourceUrl = sourceUrl,
    creator = "",
    category = "Saved to try",
    tags = listOf("Instagram", INSTAGRAM_PROCESSING_TAG),
    ingredients = emptyList(),
    steps = emptyList(),
    notes = sharedText,
    favorite = false,
    cooked = false,
    status = RecipeStatus.Draft,
    savedDate = savedDate
)

private fun failedInstagramRecipe(recipe: Recipe, sharedText: String, message: String) = recipe.copy(
    title = recipe.creator.takeIf { it.isNotBlank() }
        ?.let { "$it's Recipe Draft" }
        ?: "Instagram Recipe Draft",
    tags = listOf("Instagram", "Needs review"),
    notes = buildString {
        append(sharedText)
        if (sharedText.isNotBlank()) append("\n\n")
        append("[$message]")
    },
    status = RecipeStatus.Draft
)

private fun Throwable.isRetryable(): Boolean {
    val safeMessage = message.orEmpty().lowercase()
    return safeMessage.contains("temporarily") || safeMessage.contains("rate limit") ||
        safeMessage.contains("timed out") || safeMessage.contains("connection") ||
        this is java.io.IOException
}

private fun Throwable.safeUserMessage(): String = when {
    this is IllegalArgumentException || this is IllegalStateException ->
        message?.take(180).orEmpty().ifBlank { "The supplied content could not be processed." }
    isRetryable() -> "The network or AI service was temporarily unavailable."
    else -> "The content could not be processed safely."
}

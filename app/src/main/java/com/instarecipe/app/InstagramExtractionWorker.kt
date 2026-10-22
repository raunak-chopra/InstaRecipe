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
import androidx.work.WorkInfo
import androidx.work.WorkerParameters
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import java.time.LocalDate
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

internal const val INSTAGRAM_PROCESSING_TAG = "Processing"

internal enum class InstagramWorkOutcome { Success, Failure, Conflict, Duplicate }

internal data class InstagramEnqueuedWork(
    val workName: String,
    val workId: UUID,
    val retryTag: String
)

object InstagramExtractionWork {
    private const val KEY_SOURCE_URL = "source_url"
    private const val KEY_SHARED_TEXT = "shared_text"
    private const val KEY_TARGET_ID = "target_id"
    private const val KEY_EXPECTED_FINGERPRINT = "expected_fingerprint"
    internal const val KEY_OUTCOME = "outcome"
    internal const val KEY_MESSAGE = "message"
    private const val FIRST_IMPORT_TAG = "instagram-first-import"
    private const val EXPLICIT_RETRY_TAG_PREFIX = "instagram-explicit-retry-"

    suspend fun enqueue(context: Context, sourceUrl: String, sharedText: String) {
        enqueue(
            context,
            sourceUrl,
            sharedText,
            targetId = 0L,
            expectedFingerprint = "",
            workTag = FIRST_IMPORT_TAG
        )
    }

    internal suspend fun retry(context: Context, recipe: Recipe): InstagramEnqueuedWork {
        val sourceUrl = normalizedSourceUrl(recipe.sourceUrl)
            ?: throw IllegalArgumentException("This import no longer has a valid Instagram link.")
        val expectedFingerprint = recipe.persistedFingerprint()
        val retryTag = explicitRetryTag(recipe.id, expectedFingerprint)
        val workName = uniqueWorkName(sourceUrl)
        val workManager = WorkManager.getInstance(context.applicationContext)
        val retainedWorkId = workManager.findActiveCompatibleWorkId(workName, retryTag)
        val requestId = enqueue(
            context = context,
            sourceUrl = sourceUrl,
            sharedText = instagramRetryText(recipe),
            targetId = recipe.id,
            expectedFingerprint = expectedFingerprint,
            workTag = retryTag
        )
        val trackedWorkId = workManager.findWorkId(workName, requestId) ?: retainedWorkId ?: requestId
        return InstagramEnqueuedWork(workName, trackedWorkId, retryTag)
    }

    private suspend fun enqueue(
        context: Context,
        sourceUrl: String,
        sharedText: String,
        targetId: Long,
        expectedFingerprint: String,
        workTag: String
    ): UUID {
        val input = Data.Builder()
            .putString(KEY_SOURCE_URL, sourceUrl)
            .putString(KEY_SHARED_TEXT, sharedText)
            .putLong(KEY_TARGET_ID, targetId)
            .putString(KEY_EXPECTED_FINGERPRINT, expectedFingerprint)
            .build()
        val request = OneTimeWorkRequest.Builder(InstagramExtractionWorker::class.java)
            .setInputData(input)
            .addTag(workTag)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.SECONDS)
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .build()

        val workName = uniqueWorkName(sourceUrl)
        val operation = WorkManager.getInstance(context.applicationContext)
            .enqueueUniqueWork(workName, ExistingWorkPolicy.KEEP, request)
        operation.awaitPersistence()
        return request.id
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

    private suspend fun WorkManager.findActiveCompatibleWorkId(
        workName: String,
        retryTag: String
    ): UUID? = getUniqueWorkInfos(workName)
        .firstOrNull { !it.state.isFinished && retryTag in it.tags }
        ?.id

    private suspend fun WorkManager.findWorkId(workName: String, workId: UUID): UUID? =
        getUniqueWorkInfos(workName).firstOrNull { it.id == workId }?.id

    private suspend fun WorkManager.getUniqueWorkInfos(workName: String): List<WorkInfo> =
        suspendCancellableCoroutine { continuation ->
            val future = getWorkInfosForUniqueWork(workName)
            future.addListener(
                {
                    try {
                        if (continuation.isActive) continuation.resume(future.get())
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

    internal fun sourceUrl(params: WorkerParameters): String =
        params.inputData.getString(KEY_SOURCE_URL).orEmpty()

    internal fun sharedText(params: WorkerParameters): String =
        params.inputData.getString(KEY_SHARED_TEXT).orEmpty()

    internal fun targetId(params: WorkerParameters): Long = params.inputData.getLong(KEY_TARGET_ID, 0L)

    internal fun expectedFingerprint(params: WorkerParameters): String =
        params.inputData.getString(KEY_EXPECTED_FINGERPRINT).orEmpty()

    internal fun uniqueWorkName(sourceUrl: String): String =
        "instagram-extraction-${sourceUrl.sha256()}"

    internal fun explicitRetryTag(recipeId: Long, fingerprint: String): String =
        "$EXPLICIT_RETRY_TAG_PREFIX$recipeId-$fingerprint"

    internal fun isExplicitRetryTagForRecipe(tag: String, recipeId: Long): Boolean =
        tag.startsWith("$EXPLICIT_RETRY_TAG_PREFIX$recipeId-")

    internal fun output(outcome: InstagramWorkOutcome, message: String = ""): Data = Data.Builder()
        .putString(KEY_OUTCOME, outcome.name)
        .putString(KEY_MESSAGE, message)
        .build()
}

internal sealed interface InstagramExtractionClaim {
    val recipe: Recipe

    data class Draft(override val recipe: Recipe) : InstagramExtractionClaim
    data class Existing(
        override val recipe: Recipe,
        val expectedFingerprint: String
    ) : InstagramExtractionClaim
}

class InstagramExtractionWorker(
    appContext: Context,
    private val params: WorkerParameters
) : CoroutineWorker(appContext, params) {
    private val repository = RecipeRepository.get(appContext)

    override suspend fun doWork(): Result {
        val sourceUrl = InstagramExtractionWork.sourceUrl(params)
        val sharedText = InstagramExtractionWork.sharedText(params)
        if (sourceUrl.isBlank()) {
            return Result.failure(
                InstagramExtractionWork.output(
                    InstagramWorkOutcome.Failure,
                    "The Instagram link is missing."
                )
            )
        }

        val targetId = InstagramExtractionWork.targetId(params)
        val expectedFingerprint = InstagramExtractionWork.expectedFingerprint(params)
        val claim = if (targetId > 0L && expectedFingerprint.isNotBlank()) {
            claimInstagramRetry(repository, targetId, sourceUrl, sharedText, expectedFingerprint)
                ?: return Result.failure(
                    InstagramExtractionWork.output(
                        InstagramWorkOutcome.Conflict,
                        "This recipe changed before extraction could start. Review it, then try again."
                    )
                )
        } else {
            claimInstagramExtraction(repository, sourceUrl, sharedText)
                ?.let(InstagramExtractionClaim::Draft)
                ?: return Result.success(
                    InstagramExtractionWork.output(
                        InstagramWorkOutcome.Duplicate,
                        "This Reel is already in your cookbook."
                    )
                )
        }
        val pending = claim.recipe

        var videoFile: java.io.File? = null
        try {
            if (GeminiRecipeExtractor.getApiKeys(applicationContext).isEmpty()) {
                persistInstagramFailure(
                    repository = repository,
                    claim = claim,
                    sharedText = sharedText,
                    message = "Connect a primary or backup Auth key in Settings, then try this link again."
                )
                return Result.failure(
                    InstagramExtractionWork.output(
                        InstagramWorkOutcome.Failure,
                        "Connect a primary or backup Auth key in Settings, then try again."
                    )
                )
            }

            val customResolver = GeminiRecipeExtractor.getCustomResolver(applicationContext)
                .ifBlank { null }
            val instagramProfile = GeminiRecipeExtractor.getInstagramProfileUrl(applicationContext)
                .ifBlank { null }
            val resolution = InstagramResolver.resolveAndDownload(
                context = applicationContext,
                instagramUrl = sourceUrl,
                customResolverUrl = customResolver,
                preferredProfileUrl = instagramProfile,
                supplementaryText = sharedText
            )
            videoFile = resolution.videoFile
            val hasVideo = videoFile?.let { it.exists() && it.length() > 0 } == true
            val caption = resolution.caption
                .takeIf(GeminiRecipeExtractor::hasSubstantiveRecipeContent)
                ?: InstagramResolver.extractCaptionFromSharedText(sharedText, sourceUrl)

            if (!hasVideo && caption.isNullOrBlank()) {
                val message = if (InstagramSessionManager.isLoggedIn(applicationContext)) {
                    "Instagram did not provide an accessible video or recipe caption. Attach the saved reel video to this draft."
                } else {
                    "Instagram blocked video access. Link Instagram in Settings for direct Reel imports, or attach the saved reel video to this draft."
                }
                persistInstagramFailure(
                    repository = repository,
                    claim = claim.withCreator(resolution.creator.orEmpty()),
                    sharedText = sharedText,
                    message = message
                )
                return Result.failure(
                    InstagramExtractionWork.output(InstagramWorkOutcome.Failure, message)
                )
            }

            val extracted = GeminiRecipeExtractor.extractRecipe(
                context = applicationContext,
                videoFile = videoFile,
                textCaption = caption,
                sourceUrl = sourceUrl,
                creatorName = resolution.creator
            )
            val completed = completedInstagramRecipe(pending, extracted)
            val persisted = when (claim) {
                is InstagramExtractionClaim.Draft -> {
                    repository.upsert(completed)
                    true
                }
                is InstagramExtractionClaim.Existing -> repository.completeInstagramRetry(
                    id = pending.id,
                    expectedFingerprint = claim.expectedFingerprint,
                    replacement = completed
                )
            }
            return if (persisted) {
                Result.success(InstagramExtractionWork.output(InstagramWorkOutcome.Success))
            } else {
                Result.failure(
                    InstagramExtractionWork.output(
                        InstagramWorkOutcome.Conflict,
                        "This recipe changed while extraction was running, so your edits were kept."
                    )
                )
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            // GeminiApiClient already owns bounded model retries and paid-key fallback.
            // WorkManager retries resolver/network failures only, avoiding repeated uploads and generation charges.
            if (runAttemptCount < 1 && error !is GeminiApiException && error.isRetryable()) return Result.retry()
            val message = "We couldn't create this recipe: ${error.safeUserMessage()} Try the link again or open this draft to review it."
            persistInstagramFailure(
                repository = repository,
                claim = claim,
                sharedText = sharedText,
                message = message
            )
            return Result.failure(
                InstagramExtractionWork.output(InstagramWorkOutcome.Failure, message)
            )
        } finally {
            videoFile?.delete()
            InstagramResolver.cleanCachedReelVideos(applicationContext)
        }
    }
}

private fun InstagramExtractionClaim.withCreator(creator: String): InstagramExtractionClaim = when (this) {
    is InstagramExtractionClaim.Draft -> copy(recipe = recipe.copy(creator = creator))
    is InstagramExtractionClaim.Existing -> this
}

private suspend fun persistInstagramFailure(
    repository: RecipeStore,
    claim: InstagramExtractionClaim,
    sharedText: String,
    message: String
) {
    if (claim is InstagramExtractionClaim.Draft) {
        repository.upsert(failedInstagramRecipe(claim.recipe, sharedText, message))
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
        savedDate = existing?.savedDate ?: LocalDate.now().toString(),
        favorite = existing?.favorite ?: false,
        cooked = existing?.cooked ?: false
    )
    return if (existing == null) repository.createOrGetBySourceUrl(pending) else repository.upsert(pending)
}

internal suspend fun claimInstagramRetry(
    repository: RecipeStore,
    targetId: Long,
    sourceUrl: String,
    sharedText: String,
    expectedFingerprint: String
): InstagramExtractionClaim? {
    val existing = repository.findById(targetId) ?: return null
    if (!existing.canExtractFromLinkAgain()) return null
    if (normalizedSourceUrl(existing.sourceUrl) != normalizedSourceUrl(sourceUrl)) return null
    if (existing.persistedFingerprint() != expectedFingerprint) return null

    return InstagramExtractionClaim.Existing(existing, expectedFingerprint)
}

internal fun completedInstagramRecipe(pending: Recipe, extracted: ExtractedRecipeData) = Recipe(
    id = pending.id,
    title = extracted.title.ifBlank { pending.title },
    sourceUrl = extracted.sourceUrl.ifBlank { pending.sourceUrl },
    creator = extracted.creator.ifBlank { pending.creator },
    category = extracted.category,
    tags = TagNormalizer.normalizeAll(listOf("Instagram", "Needs review") + extracted.tags),
    ingredients = extracted.ingredients,
    steps = extracted.steps,
    notes = listOf(
        instagramRetryText(pending),
        extracted.notes.trim()
    ).filter(String::isNotBlank).distinct().joinToString("\n\n"),
    favorite = pending.favorite,
    cooked = pending.cooked,
    status = RecipeStatus.Draft,
    savedDate = pending.savedDate,
    totalTimeMinutes = extracted.totalTimeMinutes ?: pending.totalTimeMinutes,
    activeTimeMinutes = pending.activeTimeMinutes,
    yield = pending.yield,
    skillLevel = pending.skillLevel,
    cookPhotoPath = pending.cookPhotoPath,
    cookedAt = pending.cookedAt,
    dietType = extracted.dietType
)

private fun pendingInstagramRecipe(
    id: Long,
    sourceUrl: String,
    sharedText: String,
    savedDate: String,
    favorite: Boolean = false,
    cooked: Boolean = false
) = Recipe(
    id = id,
    title = "Creating recipe…",
    sourceUrl = sourceUrl,
    creator = "",
    category = "Other",
    tags = listOf("Instagram", INSTAGRAM_PROCESSING_TAG),
    ingredients = emptyList(),
    steps = emptyList(),
    notes = sharedText,
    favorite = favorite,
    cooked = cooked,
    status = RecipeStatus.Draft,
    savedDate = savedDate
)

private fun failedInstagramRecipe(recipe: Recipe, sharedText: String, message: String) = recipe.copy(
    title = recipe.creator.takeIf { it.isNotBlank() }
        ?.let { "$it's Recipe Draft" }
        ?: "Instagram Recipe Draft",
    tags = listOf("Instagram", "Needs review"),
    notes = failedInstagramNotes(sharedText, message),
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

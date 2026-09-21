package com.instarecipe.app

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import androidx.work.WorkManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transformWhile
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDate
import java.util.UUID

internal enum class InstagramRetryPhase { Starting, Running }

internal data class InstagramRetryError(val recipeId: Long, val message: String)

internal data class InstagramRetryWorkSnapshot(
    val id: UUID,
    val isFinished: Boolean,
    val tags: Set<String>
)

internal fun selectInstagramRetryWork(
    work: List<InstagramRetryWorkSnapshot>,
    expectedWorkId: UUID,
    retryTag: String
): InstagramRetryWorkSnapshot? =
    work.firstOrNull { it.id == expectedWorkId } ?:
        work.firstOrNull { !it.isFinished && retryTag in it.tags }

internal sealed interface InstagramRetryEffect {
    data object OpenSettings : InstagramRetryEffect
    data class OpenReview(val recipeId: Long) : InstagramRetryEffect
}

class InstaRecipeViewModel(
    application: Application,
    private val repository: RecipeStore
) : AndroidViewModel(application) {
    private val videoExtractionController = VideoExtractionController(viewModelScope)
    private val workManager = WorkManager.getInstance(application)
    private val retryObserverJobs = mutableMapOf<Long, Job>()
    private val restorationCheckedIds = mutableSetOf<Long>()
    private val mutableInstagramRetryPhases = MutableStateFlow<Map<Long, InstagramRetryPhase>>(emptyMap())
    private val mutableInstagramRetryError = MutableStateFlow<InstagramRetryError?>(null)
    private val instagramRetryEffectChannel = Channel<InstagramRetryEffect>(Channel.BUFFERED)

    val videoExtractionState: StateFlow<VideoExtractionState> = videoExtractionController.state
    internal val instagramRetryPhases: StateFlow<Map<Long, InstagramRetryPhase>> = mutableInstagramRetryPhases.asStateFlow()
    internal val instagramRetryError: StateFlow<InstagramRetryError?> = mutableInstagramRetryError.asStateFlow()
    internal val instagramRetryEffects = instagramRetryEffectChannel.receiveAsFlow()
    val recipes: StateFlow<List<Recipe>> = repository.recipes.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    init {
        viewModelScope.launch {
            repository.migrateLegacyPreferences(application)
            repository.recipes.first()
                .filter { it.status == RecipeStatus.Saved && it.isIncompleteRecipe() }
                .forEach { incomplete ->
                    // Never delete old data, but prevent incomplete saved rows from masquerading
                    // as cookbook recipes. They remain editable in Imports.
                    repository.upsert(incomplete.copy(status = RecipeStatus.Draft))
                }
        }
        viewModelScope.launch {
            repository.recipes.collect { currentRecipes ->
                currentRecipes.forEach { recipe ->
                    if (recipe.id > 0L && normalizedSourceUrl(recipe.sourceUrl) != null && restorationCheckedIds.add(recipe.id)) {
                        restoreActiveInstagramRetry(recipe)
                    }
                }
            }
        }
    }

    fun upsert(recipe: Recipe, onPersisted: (Recipe) -> Unit = {}) {
        viewModelScope.launch { onPersisted(persistRecipe(repository, recipe)) }
    }

    suspend fun upsertAndAwait(recipe: Recipe): Recipe = persistRecipe(repository, recipe)

    fun importGalleryVideo(uri: Uri) {
        importLocalVideo(uri, LocalVideoOrigin.Gallery)
    }

    fun importSharedVideo(uri: Uri) {
        importLocalVideo(uri, LocalVideoOrigin.Shared)
    }

    fun dismissVideoExtractionError() {
        videoExtractionController.dismissError()
    }

    fun acknowledgeCompletedVideoRecipe(recipeId: Long) {
        videoExtractionController.acknowledgeCompletedRecipe(recipeId)
    }

    fun retryInstagramImport(recipe: Recipe) {
        if (recipe.id in mutableInstagramRetryPhases.value) return
        if (!recipe.canExtractFromLinkAgain()) {
            mutableInstagramRetryError.value = InstagramRetryError(
                recipe.id,
                "This recipe is no longer eligible for link extraction."
            )
            return
        }

        val context = getApplication<Application>()
        if (GeminiRecipeExtractor.getApiKeys(context).isEmpty()) {
            mutableInstagramRetryError.value = InstagramRetryError(
                recipe.id,
                "Connect a primary or backup Auth key, then try the import again."
            )
            instagramRetryEffectChannel.trySend(InstagramRetryEffect.OpenSettings)
            return
        }

        mutableInstagramRetryError.value = null
        updateRetryPhase(recipe.id, InstagramRetryPhase.Starting)
        viewModelScope.launch {
            try {
                val enqueued = InstagramExtractionWork.retry(context, recipe)
                observeInstagramRetry(recipe.id, enqueued)
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                removeRetryPhase(recipe.id)
                mutableInstagramRetryError.value = InstagramRetryError(
                    recipe.id,
                    "That extraction could not be started. Check your connection and try again."
                )
            }
        }
    }

    fun dismissInstagramRetryError() {
        mutableInstagramRetryError.value = null
    }

    private suspend fun restoreActiveInstagramRetry(recipe: Recipe) {
        val sourceUrl = normalizedSourceUrl(recipe.sourceUrl) ?: return
        val workName = InstagramExtractionWork.uniqueWorkName(sourceUrl)
        val active = workManager.workInfosForUniqueWorkFlow(workName)
            .first()
            .firstOrNull { info ->
                !info.state.isFinished && info.tags.any {
                    InstagramExtractionWork.isExplicitRetryTagForRecipe(it, recipe.id)
                }
            }
            ?: return
        val retryTag = active.tags.first { InstagramExtractionWork.isExplicitRetryTagForRecipe(it, recipe.id) }
        observeInstagramRetry(
            recipe.id,
            InstagramEnqueuedWork(workName, active.id, retryTag)
        )
    }

    private fun observeInstagramRetry(recipeId: Long, enqueued: InstagramEnqueuedWork) {
        if (retryObserverJobs[recipeId]?.isActive == true) return
        retryObserverJobs[recipeId] = viewModelScope.launch {
            var trackedWorkId: UUID? = null
            try {
                workManager.workInfosForUniqueWorkFlow(enqueued.workName)
                    .transformWhile { workInfos ->
                        if (trackedWorkId == null) {
                            val snapshots = workInfos.map { info ->
                                InstagramRetryWorkSnapshot(info.id, info.state.isFinished, info.tags)
                            }
                            trackedWorkId = selectInstagramRetryWork(
                                snapshots,
                                expectedWorkId = enqueued.workId,
                                retryTag = enqueued.retryTag
                            )?.id
                        }
                        val tracked = trackedWorkId?.let { id -> workInfos.firstOrNull { it.id == id } }
                        if (tracked == null) {
                            mutableInstagramRetryError.value = InstagramRetryError(
                                recipeId,
                                "Another import for this link is already running. Try again when it finishes."
                            )
                            false
                        } else {
                            emit(tracked)
                            !tracked.state.isFinished
                        }
                    }
                    .collect { workInfo ->
                        if (!workInfo.state.isFinished) {
                            updateRetryPhase(recipeId, InstagramRetryPhase.Running)
                        } else {
                            handleTerminalRetry(recipeId, workInfo)
                        }
                    }
            } finally {
                removeRetryPhase(recipeId)
                retryObserverJobs.remove(recipeId)
            }
        }
    }

    private fun handleTerminalRetry(recipeId: Long, workInfo: WorkInfo) {
        val outcome = workInfo.outputData
            .getString(InstagramExtractionWork.KEY_OUTCOME)
            ?.let { value -> runCatching { InstagramWorkOutcome.valueOf(value) }.getOrNull() }
        if (workInfo.state == WorkInfo.State.SUCCEEDED && outcome == InstagramWorkOutcome.Success) {
            mutableInstagramRetryError.value = null
            instagramRetryEffectChannel.trySend(InstagramRetryEffect.OpenReview(recipeId))
            return
        }
        val message = workInfo.outputData.getString(InstagramExtractionWork.KEY_MESSAGE)
        mutableInstagramRetryError.value = InstagramRetryError(
            recipeId,
            message?.takeIf(String::isNotBlank)
                ?: if (outcome == InstagramWorkOutcome.Duplicate) {
                    "This Reel is already in your cookbook."
                } else {
                    "Extraction did not finish. Your existing recipe was kept."
                }
        )
    }

    private fun updateRetryPhase(recipeId: Long, phase: InstagramRetryPhase) {
        mutableInstagramRetryPhases.value = mutableInstagramRetryPhases.value + (recipeId to phase)
    }

    private fun removeRetryPhase(recipeId: Long) {
        mutableInstagramRetryPhases.value = mutableInstagramRetryPhases.value - recipeId
    }

    private fun importLocalVideo(uri: Uri, origin: LocalVideoOrigin) {
        if (videoExtractionState.value.progress.isExtracting) return

        val context = getApplication<Application>()
        if (GeminiRecipeExtractor.getApiKeys(context).isEmpty()) {
            videoExtractionController.reportError(origin.missingApiKeyMessage)
            return
        }

        videoExtractionController.start(
            initialStage = origin.initialStage,
            errorPrefix = origin.errorPrefix
        ) { onStatus ->
            extractAndPersistLocalVideo(
                context = context,
                repository = repository,
                uri = uri,
                sourceLabel = origin.sourceLabel,
                onStatus = onStatus
            )
        }
    }

    fun update(id: Long, transform: Recipe.() -> Recipe) {
        viewModelScope.launch { repository.update(id, transform) }
    }

    fun delete(id: Long) {
        viewModelScope.launch { repository.delete(id) }
    }

    class Factory(private val application: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(InstaRecipeViewModel::class.java))
            return InstaRecipeViewModel(application, RecipeRepository.get(application)) as T
        }
    }
}

private fun WorkManager.workInfosForUniqueWorkFlow(workName: String) = callbackFlow {
    val liveData = getWorkInfosForUniqueWorkLiveData(workName)
    val observer = Observer<List<WorkInfo>> { workInfos -> trySend(workInfos) }
    liveData.observeForever(observer)
    awaitClose { liveData.removeObserver(observer) }
}

internal suspend fun persistRecipe(repository: RecipeStore, recipe: Recipe): Recipe =
    repository.upsert(recipe.copy(tags = TagNormalizer.normalizeAll(recipe.tags)))

private enum class LocalVideoOrigin(
    val initialStage: String,
    val sourceLabel: String,
    val errorPrefix: String,
    val missingApiKeyMessage: String
) {
    Gallery(
        initialStage = "Reading video file from gallery...",
        sourceLabel = "Imported reel video",
        errorPrefix = "Video extraction failed",
        missingApiKeyMessage = "Gemini API key is required to analyze videos. Please add it in Settings."
    ),
    Shared(
        initialStage = "Reading shared video file...",
        sourceLabel = "Shared video file",
        errorPrefix = "Video analysis failed",
        missingApiKeyMessage = "Gemini API key is required to analyze shared video files. Please add it in Settings."
    )
}

internal suspend fun extractAndPersistLocalVideo(
    context: Context,
    repository: RecipeStore,
    uri: Uri,
    sourceLabel: String,
    onStatus: (String) -> Unit
): Recipe {
    var videoFile: File? = null
    try {
        videoFile = VideoFileStore.copyToCache(context, uri)
        val extracted = GeminiRecipeExtractor.extractRecipe(
            context = context,
            videoFile = videoFile,
            textCaption = null,
            sourceUrl = sourceLabel,
            creatorName = null,
            onStatus = onStatus
        )
        return persistRecipe(
            repository,
            Recipe(
                id = 0L,
                title = extracted.title,
                sourceUrl = "",
                creator = extracted.creator,
                category = extracted.category,
                tags = extracted.tags,
                ingredients = extracted.ingredients,
                steps = extracted.steps,
                notes = extracted.notes,
                favorite = false,
                cooked = false,
                status = RecipeStatus.Draft,
                savedDate = LocalDate.now().toString(),
                totalTimeMinutes = extracted.totalTimeMinutes
            )
        )
    } finally {
        withContext(NonCancellable + Dispatchers.IO) {
            videoFile?.delete()
            InstagramResolver.cleanCachedReelVideos(context)
        }
    }
}

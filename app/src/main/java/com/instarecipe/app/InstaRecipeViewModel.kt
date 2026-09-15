package com.instarecipe.app

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDate

class InstaRecipeViewModel(
    application: Application,
    private val repository: RecipeStore
) : AndroidViewModel(application) {
    private val videoExtractionController = VideoExtractionController(viewModelScope)

    val videoExtractionState: StateFlow<VideoExtractionState> = videoExtractionController.state
    val recipes: StateFlow<List<Recipe>> = repository.recipes.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    init {
        viewModelScope.launch { repository.migrateLegacyPreferences(application) }
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
                savedDate = LocalDate.now().toString()
            )
        )
    } finally {
        withContext(NonCancellable + Dispatchers.IO) {
            videoFile?.delete()
            InstagramResolver.cleanCachedReelVideos(context)
        }
    }
}

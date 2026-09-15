package com.instarecipe.app

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ExtractionProgress(
    val isExtracting: Boolean = false,
    val stage: String = "",
    val sourceUrl: String? = null,
    val error: String? = null
)

data class VideoExtractionState(
    val progress: ExtractionProgress = ExtractionProgress(),
    val completedRecipe: Recipe? = null
)

internal class VideoExtractionController(private val scope: CoroutineScope) {
    private val mutableState = MutableStateFlow(VideoExtractionState())
    val state: StateFlow<VideoExtractionState> = mutableState.asStateFlow()

    private var activeJob: Job? = null

    fun start(
        initialStage: String,
        errorPrefix: String,
        extract: suspend (onStatus: (String) -> Unit) -> Recipe
    ) {
        if (activeJob?.isActive == true) return

        activeJob = scope.launch {
            mutableState.value = VideoExtractionState(
                progress = ExtractionProgress(isExtracting = true, stage = initialStage)
            )
            try {
                val recipe = extract { stage ->
                    mutableState.update { current ->
                        current.copy(progress = current.progress.copy(stage = stage))
                    }
                }
                mutableState.value = VideoExtractionState(completedRecipe = recipe)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                val detail = error.message?.takeIf { it.isNotBlank() }
                    ?: "The video could not be processed."
                mutableState.value = VideoExtractionState(
                    progress = ExtractionProgress(error = "$errorPrefix: $detail")
                )
            }
        }
    }

    fun reportError(message: String) {
        if (activeJob?.isActive == true) return
        mutableState.value = VideoExtractionState(progress = ExtractionProgress(error = message))
    }

    fun dismissError() {
        mutableState.update { current ->
            current.copy(progress = current.progress.copy(error = null))
        }
    }

    fun acknowledgeCompletedRecipe(recipeId: Long) {
        mutableState.update { current ->
            if (current.completedRecipe?.id == recipeId) current.copy(completedRecipe = null) else current
        }
    }
}

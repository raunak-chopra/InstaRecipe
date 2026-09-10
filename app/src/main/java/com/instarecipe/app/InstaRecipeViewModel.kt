package com.instarecipe.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class InstaRecipeViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = RecipeRepository.get(application)

    val recipes: StateFlow<List<Recipe>> = repository.recipes.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    init {
        viewModelScope.launch { repository.migrateLegacyPreferences(application) }
    }

    fun upsert(recipe: Recipe) {
        viewModelScope.launch { repository.upsert(recipe.copy(tags = TagNormalizer.normalizeAll(recipe.tags))) }
    }

    fun update(id: Long, transform: Recipe.() -> Recipe) {
        recipes.value.firstOrNull { it.id == id }?.let { upsert(it.transform()) }
    }

    fun delete(id: Long) {
        viewModelScope.launch { repository.delete(id) }
    }
}

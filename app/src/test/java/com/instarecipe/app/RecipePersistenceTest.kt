package com.instarecipe.app

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class RecipePersistenceTest {
    @Test
    fun persistedRecipeCarriesGeneratedIdAndNormalizedTags() = runTest {
        val store = RecordingRecipeStore(generatedId = 42L)
        val input = Recipe(
            id = 0L,
            title = "Test recipe",
            sourceUrl = "",
            creator = "",
            category = "Dinner",
            tags = listOf(" veg ", "VEGETARIAN", "#quick recipe"),
            ingredients = emptyList(),
            steps = emptyList(),
            notes = "",
            favorite = false,
            cooked = false,
            status = RecipeStatus.Draft,
            savedDate = "2026-09-14"
        )

        val persisted = persistRecipe(store, input)

        assertEquals(42L, persisted.id)
        assertEquals(listOf("Vegetarian", "Quick"), persisted.tags)
        assertEquals(listOf("Vegetarian", "Quick"), store.lastUpserted?.tags)
    }
}

private class RecordingRecipeStore(private val generatedId: Long) : RecipeStore {
    override val recipes: Flow<List<Recipe>> = emptyFlow()
    var lastUpserted: Recipe? = null

    override suspend fun upsert(recipe: Recipe): Recipe {
        lastUpserted = recipe
        return recipe.copy(id = generatedId)
    }

    override suspend fun update(id: Long, transform: Recipe.() -> Recipe) = Unit
    override suspend fun delete(id: Long) = Unit
    override suspend fun findBySourceUrl(sourceUrl: String): Recipe? = null
    override suspend fun createOrGetBySourceUrl(recipe: Recipe): Recipe = recipe.copy(id = generatedId)
    override suspend fun migrateLegacyPreferences(context: Context) = Unit
}

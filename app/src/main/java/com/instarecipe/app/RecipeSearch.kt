package com.instarecipe.app

import java.util.Locale

private val QuickTimePattern = Regex(
    """(?i)(?:prep|cook|total)?\s*time:?\s*(\d+)\s*(?:mins?|minutes?)"""
)

data class SearchableRecipe(
    val recipe: Recipe,
    val searchableText: String
)

fun buildRecipeSearchIndex(recipes: List<Recipe>): List<SearchableRecipe> = recipes.map { recipe ->
    SearchableRecipe(
        recipe = recipe,
        searchableText = buildString {
            append(recipe.title).append(' ')
            append(recipe.creator).append(' ')
            append(recipe.category).append(' ')
            append(recipe.tags.joinToString(" ")).append(' ')
            append(recipe.ingredients.joinToString(" ")).append(' ')
            append(recipe.steps.joinToString(" ")).append(' ')
            append(recipe.notes)
        }.lowercase(Locale.ROOT)
    )
}

fun searchRecipes(index: List<SearchableRecipe>, query: String): List<Recipe> {
    val terms = query.trim().lowercase(Locale.ROOT).split(Regex("""\s+""")).filter(String::isNotBlank)
    if (terms.isEmpty()) return emptyList()
    return index.asSequence()
        .filter { entry -> terms.all(entry.searchableText::contains) }
        .map(SearchableRecipe::recipe)
        .toList()
}

fun isQuickRecipe(recipe: Recipe): Boolean {
    if (TagNormalizer.matches(recipe.tags, "Quick")) return true
    val totalMinutes = QuickTimePattern.findAll(recipe.notes)
        .mapNotNull { it.groupValues.getOrNull(1)?.toIntOrNull() }
        .sum()
    return totalMinutes in 1..20
}

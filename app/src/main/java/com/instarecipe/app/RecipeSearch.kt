package com.instarecipe.app

import java.text.Normalizer
import java.util.Locale

private val QuickTimePattern = Regex(
    """(?i)(?:prep|cook|total)?\s*time:?\s*(\d+)\s*(?:mins?|minutes?)"""
)

/** Search actual loaded content as well as optional index hits; never depend on index health. */
fun List<Recipe>.filterBySearch(query: String, matchingIds: Set<Long> = emptySet()): List<Recipe> {
    if (query.isBlank()) return this
    val terms = searchText(query).split(Regex("""[^\p{L}\p{M}\p{N}]+""")).filter(String::isNotBlank)
    if (terms.isEmpty()) return emptyList()
    return filter { recipe ->
        val content = searchText(listOf(recipe.title, recipe.creator, recipe.category, recipe.notes,
            recipe.tags.joinToString(" "), recipe.ingredients.joinToString(" "), recipe.steps.joinToString(" ")).joinToString(" "))
        recipe.id in matchingIds || terms.all { it in content }
    }
}

private fun searchText(value: String): String = Normalizer.normalize(value, Normalizer.Form.NFKD)
    .replace(Regex("\\p{Mn}+"), "").lowercase(Locale.ROOT)

fun isQuickRecipe(recipe: Recipe): Boolean {
    if (TagNormalizer.matches(recipe.tags, "Quick")) return true
    // Structured time wins; older imports only carry times inside free-text notes.
    recipe.totalTimeMinutes?.let { return it in 1..20 }
    val totalMinutes = QuickTimePattern.findAll(recipe.notes)
        .mapNotNull { it.groupValues.getOrNull(1)?.toIntOrNull() }
        .sum()
    return totalMinutes in 1..20
}

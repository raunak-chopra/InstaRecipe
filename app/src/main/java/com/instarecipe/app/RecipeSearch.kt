package com.instarecipe.app

private val QuickTimePattern = Regex(
    """(?i)(?:prep|cook|total)?\s*time:?\s*(\d+)\s*(?:mins?|minutes?)"""
)

/** Keeps [recipes] order while limiting it to full-text matches; a blank query keeps everything. */
fun List<Recipe>.filterBySearch(query: String, matchingIds: Set<Long>): List<Recipe> =
    if (query.isBlank()) this else filter { it.id in matchingIds }

fun isQuickRecipe(recipe: Recipe): Boolean {
    if (TagNormalizer.matches(recipe.tags, "Quick")) return true
    // Structured time wins; older imports only carry times inside free-text notes.
    recipe.totalTimeMinutes?.let { return it in 1..20 }
    val totalMinutes = QuickTimePattern.findAll(recipe.notes)
        .mapNotNull { it.groupValues.getOrNull(1)?.toIntOrNull() }
        .sum()
    return totalMinutes in 1..20
}

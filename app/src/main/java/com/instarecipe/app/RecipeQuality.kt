package com.instarecipe.app

/** A recipe is only allowed into the cookbook when it contains real, actionable content. */
internal enum class RecipeQualityFailure {
    MissingTitle,
    MissingIngredients,
    MissingSteps,
    PlaceholderContent
}

internal fun RecipeQualityFailure.userMessage(): String = when (this) {
    RecipeQualityFailure.MissingTitle -> "The recipe did not include a usable title."
    RecipeQualityFailure.MissingIngredients -> "No usable ingredients were found."
    RecipeQualityFailure.MissingSteps -> "No usable cooking steps were found."
    RecipeQualityFailure.PlaceholderContent -> "The extraction returned placeholder recipe details."
}

internal data class RecipeQualityAssessment(
    val failure: RecipeQualityFailure? = null
) {
    val isValid: Boolean get() = failure == null
}

private val placeholderText = Regex(
    """(?i)^(?:unknown|unknown recipe|untitled|untitled recipe|instagram recipe|instagram recipe draft|n/?a|none|not provided|not available|could not determine|cannot determine|as needed|details unavailable)[.! ]*$"""
)

internal fun String.isPlaceholderRecipeText(): Boolean = trim().matches(placeholderText)

internal fun meaningfulRecipeItems(values: List<String>): List<String> = values
    .map(String::trim)
    .filter { it.isNotBlank() && !it.isPlaceholderRecipeText() }

internal fun assessRecipeQuality(
    title: String,
    ingredients: List<String>,
    steps: List<String>
): RecipeQualityAssessment {
    if (title.trim().isBlank()) return RecipeQualityAssessment(RecipeQualityFailure.MissingTitle)
    if (title.isPlaceholderRecipeText()) return RecipeQualityAssessment(RecipeQualityFailure.PlaceholderContent)
    if (meaningfulRecipeItems(ingredients).isEmpty()) {
        return RecipeQualityAssessment(RecipeQualityFailure.MissingIngredients)
    }
    if (meaningfulRecipeItems(steps).isEmpty()) {
        return RecipeQualityAssessment(RecipeQualityFailure.MissingSteps)
    }
    if (ingredients.any { it.trim().isPlaceholderRecipeText() } || steps.any { it.trim().isPlaceholderRecipeText() }) {
        return RecipeQualityAssessment(RecipeQualityFailure.PlaceholderContent)
    }
    return RecipeQualityAssessment()
}

internal fun Recipe.isCookbookReady(): Boolean =
    status == RecipeStatus.Saved && assessRecipeQuality(title, ingredients, steps).isValid

internal fun Recipe.isIncompleteRecipe(): Boolean =
    !assessRecipeQuality(title, ingredients, steps).isValid

internal fun Recipe.displayTitle(): String =
    title.takeUnless(String::isPlaceholderRecipeText)
        ?.takeIf(String::isNotBlank)
        ?: if (status == RecipeStatus.Draft) "Recipe needs review" else "Recipe details needed"

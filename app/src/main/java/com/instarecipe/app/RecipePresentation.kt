package com.instarecipe.app

internal const val PARTIAL_RECIPE_TAG = "Needs details"

/** Validate recipe entry without changing the resolver's broader legacy source handling. */
internal fun recipeImportUrl(text: String): String? = InstagramResolver.extractInstagramUrl(text)?.takeIf {
    it.matches(Regex("""https://www\.instagram\.com/reel/[A-Za-z0-9_-]+/""", RegexOption.IGNORE_CASE))
}

internal enum class ImportStage(val label: String, val action: String) {
    Working("Reading recipe…", "Import in progress"),
    Connection("Connect recipe imports", "Open connection settings"),
    Review("Ready to review", "Review recipe"),
    Partial("Recipe needs more details", "Complete recipe"),
    Source("Needs a video or caption", "Add video or text"),
    Draft("Unfinished recipe", "Continue recipe")
}

/** Presentation over existing durable fields. No recipe is moved or discarded. */
internal fun Recipe.importStage(hasConnection: Boolean = true, isRetrying: Boolean = false): ImportStage = when {
    isRetrying || TagNormalizer.matches(tags, INSTAGRAM_PROCESSING_TAG) -> ImportStage.Working
    !hasConnection && normalizedSourceUrl(sourceUrl) != null && isIncompleteRecipe() -> ImportStage.Connection
    !isIncompleteRecipe() && !TagNormalizer.matches(tags, PARTIAL_RECIPE_TAG) -> ImportStage.Review
    meaningfulRecipeItems(ingredients).isNotEmpty() || meaningfulRecipeItems(steps).isNotEmpty() -> ImportStage.Partial
    normalizedSourceUrl(sourceUrl) != null -> ImportStage.Source
    else -> ImportStage.Draft
}

internal fun Recipe.importDescription(stage: ImportStage): String = when (stage) {
    ImportStage.Working -> "Your source is kept. You can leave this screen while we read it."
    ImportStage.Connection -> "Your link is kept. Add your Gemini key in Settings, then try again."
    ImportStage.Review -> "${ingredients.size} ingredients · ${steps.size} steps found. Check quantities before saving."
    ImportStage.Partial -> when {
        meaningfulRecipeItems(ingredients).isEmpty() -> "Add the missing ingredients. Your other details are kept."
        meaningfulRecipeItems(steps).isEmpty() -> "Add the missing cooking steps. Your ingredients are kept."
        else -> "Check the missing details in the recipe notes before saving."
    }
    ImportStage.Source -> "The link is kept. Add a video or recipe text to fill in the cooking details."
    ImportStage.Draft -> "Pick up where you left off. You can keep this as a draft until it is ready."
}

internal fun reviewedRecipeTags(tags: List<String>): List<String> = TagNormalizer.normalizeAll(tags.filterNot {
    TagNormalizer.key(it) in setOf("processing", "needs review", TagNormalizer.key(PARTIAL_RECIPE_TAG))
})

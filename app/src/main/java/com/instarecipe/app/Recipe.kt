package com.instarecipe.app

import androidx.compose.runtime.Immutable

enum class RecipeStatus {
    Draft,
    Saved
}

enum class DietType {
    Unknown,
    Vegetarian,
    NonVegetarian,
    Vegan
}

internal fun DietType.displayLabel(): String = when (this) {
    DietType.Unknown -> "Diet not set"
    DietType.Vegetarian -> "Veg"
    DietType.NonVegetarian -> "Non-veg"
    DietType.Vegan -> "Vegan"
}

private val nonVegetarianSignal = Regex(
    "\\b(chicken|fish|seafood|prawn|shrimp|mutton|lamb|beef|pork|bacon|ham|turkey|duck|anchovy|egg|eggs|gelatin|meat)\\b",
    RegexOption.IGNORE_CASE
)
private val veganSignal = Regex("\\b(vegan|plant[- ]?based)\\b", RegexOption.IGNORE_CASE)
private val vegetarianSignal = Regex("\\b(veg|vegetarian|vegetable|plant[- ]?based)\\b", RegexOption.IGNORE_CASE)

internal fun Recipe.effectiveDietType(): DietType {
    if (dietType != DietType.Unknown) return dietType
    val signals = buildList {
        add(title)
        add(category)
        addAll(tags)
        addAll(ingredients)
    }.joinToString(" ")
    return when {
        nonVegetarianSignal.containsMatchIn(signals) -> DietType.NonVegetarian
        veganSignal.containsMatchIn(signals) -> DietType.Vegan
        vegetarianSignal.containsMatchIn(signals) -> DietType.Vegetarian
        else -> DietType.Unknown
    }
}

private val legacyUncategorizedLabels = setOf("saved to try", "recipe", "uncategorized", "other", "others")

/** Keeps old imports from leaking the retired "Saved to try" label into the UI. */
internal fun Recipe.displayCategory(): String {
    val value = category.trim()
    return if (value.isBlank() || value.lowercase() in legacyUncategorizedLabels) "Other" else value
}

internal fun Recipe.homeCategoryKey(): String {
    val categorySignals = category.lowercase()
    val signals = buildList {
        add(category)
        addAll(tags)
    }.joinToString(" ").lowercase()

    return when {
        "breakfast" in categorySignals || "brunch" in categorySignals -> "breakfast"
        "lunch" in categorySignals -> "lunch"
        "dinner" in categorySignals || "supper" in categorySignals -> "dinner"
        Regex("\\b(snack|snacks|tea[- ]?time)\\b").containsMatchIn(categorySignals) -> "snack"
        effectiveDietType() == DietType.NonVegetarian -> "nonveg"
        effectiveDietType() == DietType.Vegan || effectiveDietType() == DietType.Vegetarian -> "veg"
        Regex("\\b(non[- ]?veg|non[- ]?vegetarian|meat|chicken|fish|seafood|mutton|beef|pork)\\b").containsMatchIn(signals) -> "nonveg"
        Regex("\\b(veg|vegetarian|vegan|plant[- ]?based)\\b").containsMatchIn(signals) -> "veg"
        else -> "other"
    }
}

internal fun Recipe.matchesHomeCategory(filterId: String): Boolean {
    val signals = buildList {
        add(category)
        addAll(tags)
    }.joinToString(" ").lowercase()
    return when (filterId) {
        "veg", "category:veg" -> when (effectiveDietType()) {
            DietType.Vegetarian, DietType.Vegan -> true
            DietType.NonVegetarian -> false
            DietType.Unknown -> Regex("\\b(veg|vegetarian|vegan|plant[- ]?based)\\b").containsMatchIn(signals) &&
                !Regex("\\b(non[- ]?veg|non[- ]?vegetarian|meat|chicken|fish|seafood|mutton|beef|pork)\\b").containsMatchIn(signals)
        }
        "nonveg", "category:nonveg" -> effectiveDietType() == DietType.NonVegetarian
        "category:breakfast" -> "breakfast" in signals || "brunch" in signals
        "category:lunch" -> "lunch" in signals
        "category:dinner" -> "dinner" in signals || "supper" in signals
        "category:snack" -> Regex("\\b(snack|snacks|tea[- ]?time)\\b").containsMatchIn(signals)
        "category:other" -> homeCategoryKey() == "other"
        else -> false
    }
}

@Immutable
data class Recipe(
    val id: Long,
    val title: String,
    val sourceUrl: String,
    val creator: String,
    val category: String,
    val tags: List<String>,
    val ingredients: List<String>,
    val steps: List<String>,
    val notes: String,
    val favorite: Boolean,
    val cooked: Boolean,
    val status: RecipeStatus,
    val savedDate: String,
    /** Optional, source-backed recipe facts. Blank/null means the source did not supply it. */
    val totalTimeMinutes: Int? = null,
    val activeTimeMinutes: Int? = null,
    val yield: String = "",
    val skillLevel: String = "",
    /** A private, on-device photo the cook chose after making this recipe. */
    val cookPhotoPath: String = "",
    /** ISO local date recorded when the recipe was last marked cooked. */
    val cookedAt: String? = null,
    val dietType: DietType = DietType.Unknown
)

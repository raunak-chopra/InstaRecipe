package com.instarecipe.app.ui.components

import com.instarecipe.app.Recipe


/**
 * The small, deliberately curated shelf used by Home and Collections. A category must map to a
 * meaningful chapter before it receives photography; personal statuses such as "Saved to try"
 * intentionally stay text-led.
 */
data class EditorialChapter(
    val id: String,
    val title: String,
    val note: String,
    val categoryMatchers: Set<String>
)

private val editorialChapters = listOf(
    EditorialChapter(
        id = "dinner",
        title = "Dinner",
        note = "The table after a long day.",
        categoryMatchers = setOf("dinner", "weeknight", "main", "mains")
    ),
    EditorialChapter(
        id = "quick",
        title = "Quick recipes",
        note = "Good food, soon.",
        categoryMatchers = setOf("quick", "quick recipes", "lunch")
    ),
    EditorialChapter(
        id = "sauces",
        title = "Sauces & condiments",
        note = "Small jars, big returns.",
        categoryMatchers = setOf("sauces & condiments", "sauces", "condiments")
    ),
    EditorialChapter(
        id = "fresh",
        title = "Fresh & green",
        note = "A little brightness on the shelf.",
        categoryMatchers = setOf("fresh & green", "vegetarian", "salads", "vegetables")
    ),
    EditorialChapter(
        id = "sweet",
        title = "Sweet things",
        note = "For the last page of dinner.",
        categoryMatchers = setOf("sweet things", "dessert", "desserts", "baking")
    ),
    EditorialChapter(
        id = "charred",
        title = "Charred & smoky",
        note = "Fire, depth, and a little edge.",
        categoryMatchers = setOf("charred & smoky", "grill", "smoky")
    ),
    EditorialChapter(
        id = "summer",
        title = "Summer table",
        note = "The bright, easy chapter.",
        categoryMatchers = setOf("summer table", "summer", "seasonal")
    )
)

fun editorialChapterFor(category: String): EditorialChapter? {
    val normalized = category.trim().lowercase()
    if (normalized.isBlank() || normalized == "saved to try") return null
    return editorialChapters.firstOrNull { normalized in it.categoryMatchers }
}

data class ShelfChapter(val chapter: EditorialChapter, val recipes: List<Recipe>) {
    val targetCategory: String get() = recipes.first().category
}

fun editorialShelfChapters(recipes: List<Recipe>): List<ShelfChapter> = editorialChapters.mapNotNull { chapter ->
    val matching = recipes.filter { recipe ->
        recipe.category.trim().lowercase() in chapter.categoryMatchers
    }
    matching.takeIf { it.isNotEmpty() }?.let { ShelfChapter(chapter, it) }
}

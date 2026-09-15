package com.instarecipe.app

import androidx.compose.runtime.Immutable

enum class RecipeStatus {
    Draft,
    Saved
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
    val savedDate: String
)

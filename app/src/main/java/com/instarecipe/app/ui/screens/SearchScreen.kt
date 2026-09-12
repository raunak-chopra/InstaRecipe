package com.instarecipe.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.instarecipe.app.CulinaryEmptyState
import com.instarecipe.app.Recipe
import com.instarecipe.app.buildRecipeSearchIndex
import com.instarecipe.app.searchRecipes
import com.instarecipe.app.ui.components.ModernRecipeCard
import kotlinx.coroutines.delay

@Composable
internal fun SearchTabScreen(
    recipes: List<Recipe>,
    onOpen: (Recipe) -> Unit,
    onToggleFavorite: (Recipe) -> Unit,
    onToggleCooked: (Recipe) -> Unit,
    onStartCooking: (Recipe) -> Unit,
    onTagSelected: (String) -> Unit
) {
    var query by rememberSaveable { mutableStateOf("") }
    var debouncedQuery by rememberSaveable { mutableStateOf("") }
    val searchIndex = remember(recipes) { buildRecipeSearchIndex(recipes) }
    LaunchedEffect(query) {
        if (query.isNotBlank()) delay(180)
        debouncedQuery = query
    }
    val filtered = remember(debouncedQuery, searchIndex) { searchRecipes(searchIndex, debouncedQuery) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item(contentType = "search-header") {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Pantry & Recipe Search", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Search by ingredient, dish name, or chef...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                    shape = MaterialTheme.shapes.medium,
                    singleLine = true
                )
            }
        }

        if (query.isBlank()) {
            item(contentType = "empty") {
                CulinaryEmptyState(
                    title = "What's in your fridge?",
                    message = "Type ingredients like 'paneer', 'garlic', 'quinoa', or dishes like 'pasta' to find recipes."
                )
            }
        } else if (filtered.isEmpty()) {
            item(contentType = "empty") {
                CulinaryEmptyState(
                    title = "No recipes found",
                    message = "No recipes matched '$query'. Try another ingredient or save a new reel!"
                )
            }
        } else {
            items(filtered, key = { it.id }, contentType = { "recipe-card" }) { recipe ->
                ModernRecipeCard(
                    recipe = recipe,
                    onOpen = { onOpen(recipe) },
                    onToggleFavorite = { onToggleFavorite(recipe) },
                    onToggleCooked = { onToggleCooked(recipe) },
                    onStartCooking = { onStartCooking(recipe) },
                    onTagClick = onTagSelected
                )
            }
        }
    }
}

package com.instarecipe.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.instarecipe.app.*
import com.instarecipe.app.ui.components.ModernRecipeCard
import com.instarecipe.app.ui.theme.AppRadii
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.CancellationException

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun RecipeLibraryScreen(
    recipes: List<Recipe>,
    draftCount: Int,
    selectedFilterId: String,
    onFilterSelected: (String) -> Unit,
    onOpen: (Recipe) -> Unit,
    onToggleFavorite: (Recipe) -> Unit,
    onToggleCooked: (Recipe) -> Unit,
    onStartCooking: (Recipe) -> Unit,
    onTagSelected: (String) -> Unit,
    onOpenImports: () -> Unit,
    onAdd: () -> Unit,
    searchRecipeIds: (String) -> Flow<Set<Long>>,
    resumeRecipe: Recipe? = null,
    isLoading: Boolean = false
) {
    var query by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf("") }
    var tag by rememberSaveable { mutableStateOf("") }
    var diet by rememberSaveable { mutableStateOf("") }
    var quick by rememberSaveable { mutableStateOf(false) }
    var sort by rememberSaveable { mutableStateOf("recent") }
    var showFilters by rememberSaveable { mutableStateOf(false) }
    var searchFailed by remember { mutableStateOf(false) }
    var searchAttempt by remember { mutableStateOf(0) }
    val matchingIds by produceState<Set<Long>?>(initialValue = null, key1 = query, key2 = searchAttempt) {
        value = null
        searchFailed = false
        if (query.isBlank()) value = emptySet()
        else try {
            searchRecipeIds(query).collect { value = it }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            searchFailed = true
            value = emptySet()
        }
    }
    val categories = remember(recipes) { recipes.map { it.displayCategory() }.distinct().sortedBy { it.lowercase() } }
    val tags = remember(recipes) {
        recipes.flatMap { it.tags }.mapNotNull(TagNormalizer::normalize).distinctBy(TagNormalizer::key)
            .filterNot { TagNormalizer.key(it) in setOf("instagram", "needs review", "processing", "needs details") }
            .sortedBy { it.lowercase() }
    }
    val filterCount = listOf(category.isNotBlank(), tag.isNotBlank(), diet.isNotBlank(), quick).count { it } +
        if (selectedFilterId.startsWith("tag:")) 1 else 0
    val results = remember(recipes, query, matchingIds, selectedFilterId, category, tag, diet, quick, sort) {
        val selected = recipes.filterBySearch(query, matchingIds.orEmpty()).filter { recipe ->
            val matchesView = when {
                selectedFilterId == "favorites" -> recipe.favorite
                selectedFilterId == "tocook" -> !recipe.cooked
                selectedFilterId == "cooked" -> recipe.cooked
                selectedFilterId.startsWith("tag:") -> recipe.tags.any { TagNormalizer.key(it) == selectedFilterId.removePrefix("tag:") }
                else -> true
            }
            matchesView && (category.isBlank() || recipe.displayCategory() == category) &&
                (tag.isBlank() || TagNormalizer.matches(recipe.tags, tag)) &&
                (diet.isBlank() || recipe.effectiveDietType().name == diet) && (!quick || isQuickRecipe(recipe))
        }
        when (sort) {
            "title" -> selected.sortedBy { it.title.lowercase() }
            "time" -> selected.sortedBy { it.totalTimeMinutes ?: Int.MAX_VALUE }
            else -> selected
        }
    }
    fun clearFilters() { category = ""; tag = ""; diet = ""; quick = false; onFilterSelected("all") }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().widthIn(max = 800.dp).align(Alignment.TopCenter).imePadding(),
            contentPadding = PaddingValues(start = 20.dp, top = 12.dp, end = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                OutlinedTextField(
                    value = query, onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    label = { Text("Search your recipes") },
                    placeholder = { Text("Recipe, ingredient or creator") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = { if (query.isNotEmpty()) IconButton(onClick = { query = "" }) { Icon(Icons.Default.Close, "Clear search") } },
                    shape = RoundedCornerShape(AppRadii.control)
                )
            }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(listOf("all" to "All", "favorites" to "Favorites", "tocook" to "To cook", "cooked" to "Cooked")) { (id, label) ->
                        FilterChip(selected = selectedFilterId == id, onClick = { onFilterSelected(id) }, label = { Text(label) }, modifier = Modifier.heightIn(min = 48.dp))
                    }
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("${results.size} ${if (results.size == 1) "recipe" else "recipes"}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    TextButton(onClick = { showFilters = true }, modifier = Modifier.heightIn(min = 48.dp)) {
                        Text(if (filterCount > 0) "Filters & sort ($filterCount)" else "Filters & sort")
                    }
                }
                if (selectedFilterId.startsWith("tag:")) {
                    Text("Tag: ${selectedFilterId.removePrefix("tag:")}", style = MaterialTheme.typography.bodyMedium)
                }
            }
            if (resumeRecipe != null) item {
                OutlinedButton(onClick = { onStartCooking(resumeRecipe) }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Resume cooking: ${resumeRecipe.displayTitle()}") }
            }
            if (draftCount > 0) item {
                OutlinedButton(onClick = onOpenImports, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("$draftCount ${if (draftCount == 1) "import" else "imports"} to finish") }
            }
            when {
                searchFailed -> item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Search couldn't finish. Your recipes are still kept.", color = MaterialTheme.colorScheme.error)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            TextButton(onClick = { searchAttempt++ }) { Text("Try search again") }
                            TextButton(onClick = { query = "" }) { Text("Show all recipes") }
                        }
                    }
                }
                isLoading || (query.isNotBlank() && matchingIds == null) -> item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
                results.isEmpty() -> item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        shape = RoundedCornerShape(AppRadii.card)
                    ) {
                        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                if (recipes.isEmpty()) "Your recipes start here" else "No matching recipes",
                                style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() }
                            )
                            Text(
                                if (recipes.isEmpty()) "Share a cooking Reel to InstaRecipe, paste a link, or write your own recipe."
                                else if (query.isNotBlank()) "Nothing matches “$query”. Try another ingredient or clear your filters."
                                else "There are no recipes in this view. Choose All or clear your filters.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (recipes.isEmpty()) Button(onClick = onAdd) { Text("Add a recipe") }
                            else {
                                TextButton(onClick = { clearFilters() }) { Text("Clear filters") }
                                if (query.isNotBlank()) TextButton(onClick = { query = "" }) { Text("Clear search") }
                            }
                        }
                    }
                }
                else -> items(results, key = { it.id }, contentType = { "recipe" }) { recipe ->
                    ModernRecipeCard(recipe, { onOpen(recipe) }, { onToggleFavorite(recipe) }, { onToggleCooked(recipe) }, { onStartCooking(recipe) }, onTagClick = onTagSelected)
                }
            }
        }
    }
    if (showFilters) ModalBottomSheet(onDismissRequest = { showFilters = false }) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).weight(1f, fill = false)) {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(bottom = 16.dp)) {
                item { Text("Filters & sort", style = MaterialTheme.typography.titleLarge) }
                item {
                    Text("Sort by", style = MaterialTheme.typography.titleMedium)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("recent" to "Recently added", "title" to "Recipe name", "time" to "Cooking time").forEach { (id, label) ->
                            FilterChip(sort == id, { sort = id }, label = { Text(label) })
                        }
                    }
                }
                item {
                    Text("Category", style = MaterialTheme.typography.titleMedium)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(category.isBlank(), { category = "" }, label = { Text("Any category") })
                        categories.forEach { value -> FilterChip(category == value, { category = if (category == value) "" else value }, label = { Text(value) }) }
                    }
                }
                item {
                    Text("Diet", style = MaterialTheme.typography.titleMedium)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(DietType.Vegetarian, DietType.NonVegetarian, DietType.Vegan).forEach { value ->
                            FilterChip(diet == value.name, { diet = if (diet == value.name) "" else value.name }, label = { Text(value.displayLabel()) })
                        }
                    }
                    FilterChip(quick, { quick = !quick }, label = { Text("20 min or less") })
                }
                if (tags.isNotEmpty()) item {
                    Text("Tags", style = MaterialTheme.typography.titleMedium)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        tags.forEach { value -> FilterChip(tag == value, { tag = if (tag == value) "" else value }, label = { Text(value) }) }
                    }
                }
                item {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        TextButton(onClick = { clearFilters() }) { Text("Clear filters") }
                        Button(onClick = { showFilters = false }) { Text("Show recipes") }
                    }
                }
            }
        }
    }
}

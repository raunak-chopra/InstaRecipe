package com.instarecipe.app.ui.screens

import android.content.Context
import android.net.Uri
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.instarecipe.app.ui.components.CategoryFilter
import com.instarecipe.app.ui.components.CategoryFilterRow
import com.instarecipe.app.ui.components.FilterIconType
import com.instarecipe.app.ui.components.ModernRecipeCard
import com.instarecipe.app.ui.theme.DeepCoral
import com.instarecipe.app.ui.theme.AppMotion
import com.instarecipe.app.ui.theme.LocalReducedMotion
import com.instarecipe.app.ui.theme.ThemeMode
import kotlinx.coroutines.launch
import com.instarecipe.app.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MainScaffold(
    recipes: List<Recipe>,
    selectedTab: Tab,
    extractionProgress: ExtractionProgress,
    onDismissError: () -> Unit,
    onTabSelected: (Tab) -> Unit,
    libraryFilterId: String,
    onLibraryFilterSelected: (String) -> Unit,
    onTagSelected: (String) -> Unit,
    onOpen: (Recipe) -> Unit,
    onToggleFavorite: (Recipe) -> Unit,
    onToggleCooked: (Recipe) -> Unit,
    onStartCooking: (Recipe) -> Unit,
    onAdd: () -> Unit,
    onImportVideo: () -> Unit,
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit
) {
    val savedRecipes = remember(recipes) { recipes.filter { it.status == RecipeStatus.Saved } }
    val draftRecipes = remember(recipes) { recipes.filter { it.status == RecipeStatus.Draft } }
    val reducedMotion = LocalReducedMotion.current
    val fabInteractionSource = remember { MutableInteractionSource() }
    val isFabPressed by fabInteractionSource.collectIsPressedAsState()
    var showAddSheet by rememberSaveable { mutableStateOf(false) }
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Restaurant,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Text(
                            "InstaRecipe",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge
                        )
                        Icon(
                            Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            if (selectedTab != Tab.Settings) {
                ExtendedFloatingActionButton(
                    onClick = { showAddSheet = true },
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("Add recipe", fontWeight = FontWeight.SemiBold) },
                    containerColor = if (isFabPressed) DeepCoral else MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = RoundedCornerShape(16.dp),
                    interactionSource = fabInteractionSource
                )
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            ) {
                Tab.entries.forEach { tab ->
                    val isSelected = selectedTab == tab
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { onTabSelected(tab) },
                        icon = {
                            Icon(
                                tab.icon,
                                contentDescription = tab.label,
                                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        label = {
                            Text(
                                tab.label,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Live AI Extraction Progress Banner
            if (extractionProgress.isExtracting) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.5.dp, color = MaterialTheme.colorScheme.primary)
                            Text("Creating your recipe", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                        Text(extractionProgress.stage, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                        LinearProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp)),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.outlineVariant
                        )
                    }
                }
            } else if (extractionProgress.error != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.size(10.dp))
                        Text(
                            extractionProgress.error,
                            modifier = Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                        IconButton(onClick = onDismissError) {
                            Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }

            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = {
                    if (reducedMotion) {
                        fadeIn(snap()) togetherWith fadeOut(snap())
                    } else {
                        fadeIn(tween(AppMotion.content, easing = AppMotion.standardEasing)) togetherWith
                            fadeOut(tween(AppMotion.state, easing = AppMotion.standardEasing))
                    }
                },
                label = "tab content",
                modifier = Modifier.weight(1f)
            ) { activeTab -> when (activeTab) {
                Tab.Library -> LibraryTabScreen(
                    recipes = savedRecipes,
                    selectedFilterId = libraryFilterId,
                    onFilterSelected = onLibraryFilterSelected,
                    onOpen = onOpen,
                    onToggleFavorite = onToggleFavorite,
                    onToggleCooked = onToggleCooked,
                    onStartCooking = onStartCooking,
                    onTagSelected = onTagSelected
                )

                Tab.Inbox -> InboxTabScreen(
                    drafts = draftRecipes,
                    onOpen = onOpen,
                    onToggleFavorite = onToggleFavorite,
                    onToggleCooked = onToggleCooked,
                    onStartCooking = onStartCooking,
                    onImportVideo = onImportVideo,
                    onTagSelected = onTagSelected
                )

                Tab.Settings -> SettingsScreen(
                    themeMode = themeMode,
                    onThemeModeChange = onThemeModeChange
                )
            } }
        }
    }

    if (showAddSheet) {
        ModalBottomSheet(onDismissRequest = { showAddSheet = false }) {
            Column(
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Add a recipe", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(
                    "Share a cooking Reel to InstaRecipe, choose a saved video, or enter a recipe yourself.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(
                    onClick = {
                        showAddSheet = false
                        onImportVideo()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.PlayCircle, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text("Choose a saved video")
                }
                OutlinedButton(
                    onClick = {
                        showAddSheet = false
                        onAdd()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text("Enter manually or paste a link")
                }
            }
        }
    }
}

@Composable
private fun LibraryTabScreen(
    recipes: List<Recipe>,
    selectedFilterId: String,
    onFilterSelected: (String) -> Unit,
    onOpen: (Recipe) -> Unit,
    onToggleFavorite: (Recipe) -> Unit,
    onToggleCooked: (Recipe) -> Unit,
    onStartCooking: (Recipe) -> Unit,
    onTagSelected: (String) -> Unit
) {
    var query by rememberSaveable { mutableStateOf("") }
    val searchIndex = remember(recipes) { buildRecipeSearchIndex(recipes) }
    val searchedRecipes = remember(query, searchIndex) {
        if (query.isBlank()) recipes else searchRecipes(searchIndex, query)
    }
    val filters = remember(recipes) {
        val builtIns = listOf(
            CategoryFilter("all", "All"),
            CategoryFilter("favorites", "Favorites", FilterIconType.Favorite),
            CategoryFilter("quick", "Quick (<20m)", FilterIconType.Timer),
            CategoryFilter("protein", "High Protein"),
            CategoryFilter("veg", "Vegetarian"),
            CategoryFilter("cooked", "Cooked", FilterIconType.Check)
        )
        val builtInKeys = setOf("quick", "high protein", "vegetarian")
        val tagFilters = TagNormalizer.normalizeAll(recipes.flatMap { it.tags })
            .filterNot { TagNormalizer.key(it) in builtInKeys }
            .sortedBy { it.lowercase() }
            .map { CategoryFilter("tag:${TagNormalizer.key(it)}", it) }
        builtIns + tagFilters
    }

    val filteredRecipes = remember(selectedFilterId, searchedRecipes) {
        when (selectedFilterId) {
            "all" -> searchedRecipes
            "favorites" -> searchedRecipes.filter { it.favorite }
            "quick" -> searchedRecipes.filter(::isQuickRecipe)
            "protein" -> searchedRecipes.filter { recipe ->
                TagNormalizer.matches(recipe.tags, "High Protein")
            }
            "veg" -> searchedRecipes.filter { recipe ->
                TagNormalizer.matches(recipe.tags, "Vegetarian")
            }
            "cooked" -> searchedRecipes.filter { it.cooked }
            else -> if (selectedFilterId.startsWith("tag:")) {
                val tagKey = selectedFilterId.removePrefix("tag:")
                searchedRecipes.filter { recipe -> recipe.tags.any { TagNormalizer.key(it) == tagKey } }
            } else {
                searchedRecipes.filter { it.category.equals(selectedFilterId, ignoreCase = true) }
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 112.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column(modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp)) {
                Text(
                    "My Cookbook",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "${recipes.size} saved ${if (recipes.size == 1) "recipe" else "recipes"} ready to cook",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Search recipes or ingredients") },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium
                )
            }
        }

        // Horizontal Category Filter Pills
        item {
            CategoryFilterRow(
                filters = filters,
                selectedFilterId = selectedFilterId,
                onFilterSelected = onFilterSelected
            )
        }

        if (filteredRecipes.isEmpty()) {
            item {
                CulinaryEmptyState(
                    title = if (selectedFilterId == "all") "Your Cookbook is Fresh" else "No matching recipes",
                    message = if (selectedFilterId == "all") {
                        "Share a cooking Reel or choose a saved video to create your first recipe."
                    } else {
                        "Try another search or switch back to All."
                    }
                )
            }
        } else {
            items(filteredRecipes, key = { it.id }, contentType = { "recipe-card" }) { recipe ->
                Box(modifier = Modifier.padding(horizontal = 20.dp)) {
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
}
@Composable
private fun InboxTabScreen(
    drafts: List<Recipe>,
    onOpen: (Recipe) -> Unit,
    onToggleFavorite: (Recipe) -> Unit,
    onToggleCooked: (Recipe) -> Unit,
    onStartCooking: (Recipe) -> Unit,
    onImportVideo: () -> Unit,
    onTagSelected: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, top = 14.dp, end = 20.dp, bottom = 112.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column {
                Text(
                    "Imports",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Recipes being created or waiting for your review.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column {
                        Text("Create from a saved video", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Text(
                            "Choose a cooking video and InstaRecipe will turn it into an editable recipe.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Button(
                        onClick = onImportVideo,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.PlayCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.size(4.dp))
                        Text("Choose video")
                    }
                }
            }
        }

        if (drafts.isEmpty()) {
            item {
                CulinaryEmptyState(
                    title = "Nothing waiting for review",
                    message = "Share a cooking Reel to InstaRecipe or choose a saved video to get started."
                )
            }
        } else {
            items(drafts, key = { it.id }, contentType = { "recipe-card" }) { draft ->
                ModernRecipeCard(
                    recipe = draft,
                    onOpen = { onOpen(draft) },
                    onToggleFavorite = { onToggleFavorite(draft) },
                    onToggleCooked = { onToggleCooked(draft) },
                    onStartCooking = { onStartCooking(draft) },
                    onTagClick = onTagSelected
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SettingsScreen(
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var apiKey by remember { mutableStateOf(GeminiRecipeExtractor.getApiKey(context)) }
    var backupApiKey by remember { mutableStateOf(GeminiRecipeExtractor.getBackupApiKey(context)) }
    var showApiKey by rememberSaveable { mutableStateOf(false) }
    var customResolver by remember { mutableStateOf(GeminiRecipeExtractor.getCustomResolver(context)) }

    var storageCleanedNotice by remember { mutableStateOf<String?>(null) }

    var testStatus by remember { mutableStateOf<String?>(null) }
    var isTesting by remember { mutableStateOf(false) }
    var advancedExpanded by rememberSaveable { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text("Settings", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("Choose your appearance and connect recipe creation.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = MaterialTheme.shapes.large,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Appearance", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Choose a theme or follow your phone automatically.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ThemeMode.entries.forEach { mode ->
                            FilterChip(
                                selected = themeMode == mode,
                                onClick = { onThemeModeChange(mode) },
                                label = { Text(mode.name) },
                                leadingIcon = if (themeMode == mode) {
                                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                } else null
                            )
                        }
                    }
                }
            }
        }

        // Gemini AI Section
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text("Recipe creation", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        "Your free key is tried first. If it times out, reaches its quota, or is unavailable, InstaRecipe uses your paid fallback key.",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    OutlinedTextField(
                        value = apiKey,
                        onValueChange = {
                            apiKey = it
                        },
                        label = { Text("Free key (primary)") },
                        placeholder = { Text("AIzaSy...") },
                        leadingIcon = { Icon(Icons.Default.Key, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                        trailingIcon = {
                            IconButton(onClick = { showApiKey = !showApiKey }) {
                                Icon(
                                    if (showApiKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (showApiKey) "Hide API key" else "Show API key"
                                )
                            }
                        },
                        visualTransformation = if (showApiKey) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = backupApiKey,
                        onValueChange = { backupApiKey = it },
                        label = { Text("Paid key (fallback)") },
                        placeholder = { Text("Optional paid key") },
                        leadingIcon = { Icon(Icons.Default.Key, contentDescription = null, tint = MaterialTheme.colorScheme.secondary) },
                        trailingIcon = {
                            IconButton(onClick = { showApiKey = !showApiKey }) {
                                Icon(
                                    if (showApiKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (showApiKey) "Hide API keys" else "Show API keys"
                                )
                            }
                        },
                        visualTransformation = if (showApiKey) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    Text(
                        "The paid key is used only when the free key is rejected, rate-limited, unavailable, or takes too long.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        "Use current Auth keys from Google AI Studio. Keys stay encrypted on this device.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        "Connected service: Google Gemini",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedButton(
                            onClick = {
                                testStatus = if (GeminiRecipeExtractor.setApiKeys(context, apiKey, backupApiKey)) {
                                    "Gemini keys saved securely on this device."
                                } else {
                                    "The Gemini keys could not be encrypted on this device."
                                }
                            },
                            enabled = !isTesting,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Save")
                        }
                        Button(
                            onClick = {
                                isTesting = true
                                testStatus = null
                                coroutineScope.launch {
                                    val res = GeminiRecipeExtractor.testConnections(apiKey, backupApiKey)
                                    if (res.isSuccess && !GeminiRecipeExtractor.setApiKeys(context, apiKey, backupApiKey)) {
                                        testStatus = "Connection worked, but the keys could not be encrypted on this device."
                                        isTesting = false
                                        return@launch
                                    }
                                    isTesting = false
                                    testStatus = res.getOrElse { it.message ?: "Failed" }
                                }
                            },
                            enabled = (apiKey.isNotBlank() || backupApiKey.isNotBlank()) && !isTesting,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            if (isTesting) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                                Spacer(Modifier.size(8.dp))
                                Text("Testing...")
                            } else {
                                Icon(Icons.Default.Check, contentDescription = null)
                                Spacer(Modifier.size(4.dp))
                                Text("Check connection")
                            }
                        }
                    }

                    if (testStatus != null) {
                        val isSuccess = testStatus?.let {
                            it.contains("ready", ignoreCase = true) ||
                                it.contains("successfully", ignoreCase = true) ||
                                it.contains("saved securely", ignoreCase = true)
                        } == true
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSuccess) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = testStatus!!,
                                color = if (isSuccess) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(12.dp),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        item {
            TextButton(
                onClick = { advancedExpanded = !advancedExpanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (advancedExpanded) "Hide advanced settings" else "Advanced settings")
            }
        }

        if (advancedExpanded) {
        // Storage & Auto-Deletion Policy
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text("Temporary files", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        "Downloaded and imported working videos are deleted after a recipe is created.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    OutlinedButton(
                        onClick = {
                            InstagramResolver.cleanCachedReelVideos(context)
                            storageCleanedNotice = "All temporary video cache files deleted!"
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.size(6.dp))
                        Text("Clear temporary videos", fontWeight = FontWeight.SemiBold)
                    }
                    if (storageCleanedNotice != null) {
                        Text(
                            text = storageCleanedNotice!!,
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // Video Resolver Section
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.PlayCircle, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                        Text("Custom video service", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        "Optionally provide a trusted HTTPS service for retrieving public Reel videos.",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    OutlinedTextField(
                        value = customResolver,
                        onValueChange = {
                            customResolver = it
                            val resolverUri = runCatching { Uri.parse(it.trim()) }.getOrNull()
                            if (it.isBlank() || (resolverUri?.scheme == "https" && !resolverUri.host.isNullOrBlank())) {
                                GeminiRecipeExtractor.setCustomResolver(context, it)
                            }
                        },
                        label = { Text("Custom service URL (optional)") },
                        placeholder = { Text("https://my-cobalt-instance.example.com") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                    Text(
                        "Leave this empty unless you operate a compatible service. If a Reel cannot be read, choose a saved video instead.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // How to use guide
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Importing from Instagram", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                    Text("Saved video", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                    Text("Download the cooking Reel, then choose it from Add recipe. InstaRecipe reads the video, audio, and on-screen instructions.", style = MaterialTheme.typography.bodySmall)

                    Spacer(Modifier.height(4.dp))
                    Text("Caption or link", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                    Text("Add a recipe manually, then paste the post caption or public Reel link into the import tools.", style = MaterialTheme.typography.bodySmall)

                    Spacer(Modifier.height(4.dp))
                    Text("Direct share", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                    Text("From Instagram, use Share and select InstaRecipe. The new recipe appears under Imports while it is being created.", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        }
    }
}

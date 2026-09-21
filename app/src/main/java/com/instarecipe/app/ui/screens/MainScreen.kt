package com.instarecipe.app.ui.screens

import android.content.Context
import android.net.Uri
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.instarecipe.app.ui.components.CategoryFilter
import com.instarecipe.app.ui.components.CategoryFilterRow
import com.instarecipe.app.ui.components.FilterIconType
import com.instarecipe.app.ui.components.editorialChapterFor
import com.instarecipe.app.ui.components.editorialShelfChapters
import com.instarecipe.app.ui.components.ModernRecipeCard
import com.instarecipe.app.R
import com.instarecipe.app.ui.theme.AppMotion
import com.instarecipe.app.ui.theme.AppRadii
import com.instarecipe.app.ui.theme.LocalMotionMode
import com.instarecipe.app.ui.theme.MotionMode
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
    onRetryInstagramImport: (Recipe) -> Unit,
    retryingInstagramImportIds: Set<Long>,
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    motionMode: MotionMode,
    onMotionModeChange: (MotionMode) -> Unit
) {
    val savedRecipes = remember(recipes) { recipes.filter(Recipe::isCookbookReady) }
    val draftRecipes = remember(recipes) { recipes.filter(Recipe::belongsInImports) }
    val primaryTabs = remember { listOf(Tab.Home, Tab.Collections, Tab.Saved, Tab.Settings) }
    val motion = LocalMotionMode.current
    var showAddSheet by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(selectedTab) {
        if (selectedTab == Tab.Explore) onTabSelected(Tab.Home)
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(34.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_brand_mark),
                                    contentDescription = null,
                                    tint = Color.Unspecified,
                                    modifier = Modifier.size(25.dp)
                                )
                            }
                        }
                        Column {
                            Text("InstaRecipe", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleLarge)
                            Text(
                                "SAVED. SORTED. COOKED.",
                                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.2.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {},
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp
            ) {
                primaryTabs.forEach { tab ->
                    val isSelected = selectedTab == tab || (selectedTab == Tab.Inbox && tab == Tab.Home)
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { onTabSelected(tab) },
                        icon = {
                            Icon(
                                tab.icon,
                                contentDescription = tab.label,
                                tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        label = {
                            Text(
                                tab.label,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
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
            // Import progress banner
            if (extractionProgress.isExtracting) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    shape = RoundedCornerShape(AppRadii.surface)
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
                    shape = RoundedCornerShape(AppRadii.surface)
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
                    if (motion == MotionMode.Off) {
                        fadeIn(snap()) togetherWith fadeOut(snap())
                    } else if (motion == MotionMode.Reduced) {
                        fadeIn(tween(80)) togetherWith fadeOut(tween(80))
                    } else {
                        fadeIn(tween(AppMotion.content, easing = AppMotion.standardEasing)) togetherWith
                            fadeOut(tween(AppMotion.state, easing = AppMotion.standardEasing))
                    }
                },
                label = "tab content",
                modifier = Modifier.weight(1f)
            ) { activeTab -> when (activeTab) {
                Tab.Home, Tab.Explore -> LibraryTabScreen(
                    recipes = savedRecipes,
                    draftCount = draftRecipes.size,
                    selectedFilterId = libraryFilterId,
                    onFilterSelected = onLibraryFilterSelected,
                    onOpen = onOpen,
                    onToggleFavorite = onToggleFavorite,
                    onToggleCooked = onToggleCooked,
                    onStartCooking = onStartCooking,
                    onTagSelected = onTagSelected,
                    onOpenImports = { onTabSelected(Tab.Inbox) },
                    onSaveReel = { showAddSheet = true },
                    onRetryInstagramImport = onRetryInstagramImport,
                    retryingInstagramImportIds = retryingInstagramImportIds
                )

                Tab.Collections -> CollectionsTabScreen(
                    recipes = savedRecipes,
                    onOpen = onOpen,
                    onExploreCategory = { category ->
                        onLibraryFilterSelected(category)
                        onTabSelected(Tab.Home)
                    }
                )

                Tab.Saved -> SavedTabScreen(
                    recipes = savedRecipes,
                    onOpen = onOpen,
                    onToggleFavorite = onToggleFavorite,
                    onToggleCooked = onToggleCooked,
                    onStartCooking = onStartCooking,
                    onExplore = { onTabSelected(Tab.Home) }
                )

                Tab.Inbox -> InboxTabScreen(
                    drafts = draftRecipes,
                    onOpen = onOpen,
                    onToggleFavorite = onToggleFavorite,
                    onToggleCooked = onToggleCooked,
                    onStartCooking = onStartCooking,
                    onImportVideo = onImportVideo,
                    onRetryInstagramImport = onRetryInstagramImport,
                    retryingInstagramImportIds = retryingInstagramImportIds,
                    onTagSelected = onTagSelected
                )

                Tab.Settings -> SettingsScreen(
                    themeMode = themeMode,
                    onThemeModeChange = onThemeModeChange,
                    motionMode = motionMode,
                    onMotionModeChange = onMotionModeChange
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
                Text("Add to your shelf", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text(
                    "Save from Instagram or add a recipe manually. InstaRecipe keeps the source, sorts the details, and gets it ready for the stove.",
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
                    Text("Import a cooking video")
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
                    Text("Add it manually")
                }
            }
        }
    }
}

@Composable
private fun HomeTabScreen(
    recipes: List<Recipe>,
    draftCount: Int,
    onOpen: (Recipe) -> Unit,
    onToggleFavorite: (Recipe) -> Unit,
    onToggleCooked: (Recipe) -> Unit,
    onStartCooking: (Recipe) -> Unit,
    onExplore: () -> Unit,
    onOpenCollections: () -> Unit,
    onOpenChapter: (String) -> Unit,
    onOpenSaved: () -> Unit,
    onOpenImports: () -> Unit,
    onAddRecipe: () -> Unit
) {
    val shelfChapters = remember(recipes) { editorialShelfChapters(recipes) }
    val featuredChapter = shelfChapters.firstOrNull()
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, top = 16.dp, end = 20.dp, bottom = 112.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("YOUR KITCHEN SHELF", style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.1.sp), color = MaterialTheme.colorScheme.primary)
                Text("Saved ideas. Sorted for cooking.", style = MaterialTheme.typography.displaySmall)
                Text(
                    "A personal cookbook for the recipes you find, make, and want to return to.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(AppRadii.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column {
                    Image(
                        painter = painterResource(R.drawable.editorial_home_feature),
                        contentDescription = "Editorial cookbook photograph of a shared table",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxWidth().height(236.dp)
                    )
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("READY FOR THE STOVE", style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp), color = MaterialTheme.colorScheme.primary)
                        Text(featuredChapter?.chapter?.title ?: "Start a chapter worth keeping", style = MaterialTheme.typography.headlineMedium)
                        Text(
                            featuredChapter?.let { "${it.recipes.size} recipes in this chapter. ${it.chapter.note}" }
                                ?: "Save a recipe or add one manually to start your shelf.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(
                            onClick = { featuredChapter?.let { onOpenChapter(it.targetCategory) } ?: onAddRecipe() },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                            shape = RoundedCornerShape(AppRadii.control)
                        ) {
                            Text(if (featuredChapter == null) "Save a recipe" else "Open chapter")
                        }
                    }
                }
            }
        }
        if (draftCount > 0) {
            item {
                OutlinedButton(
                    onClick = onOpenImports,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    shape = RoundedCornerShape(AppRadii.control)
                ) {
                    Text("Imports waiting for review: $draftCount")
                }
            }
        }
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Your recipe index", style = MaterialTheme.typography.headlineMedium)
                TextButton(onClick = onOpenCollections) { Text("All chapters") }
            }
        }
        if (shelfChapters.isEmpty()) {
            item {
                CulinaryEmptyState(
                    title = "Your shelf is waiting",
                    message = "Save recipes into a named category and the first books will appear here."
                )
            }
        } else {
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    items(shelfChapters, key = { it.chapter.id }) { shelf ->
                        Card(
                            onClick = { onOpenChapter(shelf.targetCategory) },
                            modifier = Modifier.width(176.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(AppRadii.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Column {
                                Image(
                                    painter = painterResource(shelf.chapter.coverRes),
                                    contentDescription = "Editorial cover for the ${shelf.chapter.title} chapter",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxWidth().height(220.dp)
                                )
                                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("${shelf.recipes.size} recipes", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                                    Text(shelf.chapter.title, style = MaterialTheme.typography.titleLarge)
                                    Text(shelf.chapter.note, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
                                }
                            }
                        }
                    }
                }
            }
        }
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Ready to cook", style = MaterialTheme.typography.headlineMedium)
                TextButton(onClick = onExplore) { Text("Explore") }
            }
        }
        recipes.take(2).forEach { recipe ->
            item(key = "home-${recipe.id}", contentType = "home-recipe") {
                ModernRecipeCard(
                    recipe = recipe,
                    onOpen = { onOpen(recipe) },
                    onToggleFavorite = { onToggleFavorite(recipe) },
                    onToggleCooked = { onToggleCooked(recipe) },
                    onStartCooking = { onStartCooking(recipe) }
                )
            }
        }
        if (recipes.isNotEmpty()) {
            item {
                OutlinedButton(
                    onClick = onOpenSaved,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    shape = RoundedCornerShape(AppRadii.control)
                ) { Text("Open your saved index") }
            }
        }
    }
}

@Composable
private fun CollectionsTabScreen(
    recipes: List<Recipe>,
    onOpen: (Recipe) -> Unit,
    onExploreCategory: (String) -> Unit
) {
    val chapters = remember(recipes) {
        recipes.groupBy { it.displayCategory() }
            .entries
            .sortedByDescending { it.value.size }
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, top = 16.dp, end = 20.dp, bottom = 112.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("COLLECTIONS", style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.1.sp), color = MaterialTheme.colorScheme.primary)
                Text("The cover and chapter", style = MaterialTheme.typography.displaySmall)
                Text("Each chapter is built from your real recipe categories.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (chapters.isEmpty()) {
            item {
                CulinaryEmptyState(
                    title = "No chapters yet",
                    message = "Save recipes with a category and this shelf will arrange them into useful chapters."
                )
            }
        } else {
            items(chapters, key = { it.key }, contentType = { "collection" }) { (category, recipesInChapter) ->
                val chapter = editorialChapterFor(category)
                Card(
                    onClick = { onExploreCategory(category) },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(AppRadii.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column {
                        if (chapter != null) {
                            Image(
                                painter = painterResource(chapter.coverRes),
                                contentDescription = "Editorial cover for the $category recipe collection",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxWidth().height(220.dp)
                            )
                        } else {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.fillMaxWidth().height(112.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(18.dp),
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text("PERSONAL CHAPTER", style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp), color = MaterialTheme.colorScheme.onPrimaryContainer)
                                    Text("A place waiting for its own story.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                }
                            }
                        }
                        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("${recipesInChapter.size} ${if (recipesInChapter.size == 1) "recipe" else "recipes"}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                            Text(category, style = MaterialTheme.typography.headlineMedium)
                            chapter?.let {
                                Text(it.note, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            recipesInChapter.firstOrNull()?.let { recipe ->
                                Text(recipe.title, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SavedTabScreen(
    recipes: List<Recipe>,
    onOpen: (Recipe) -> Unit,
    onToggleFavorite: (Recipe) -> Unit,
    onToggleCooked: (Recipe) -> Unit,
    onStartCooking: (Recipe) -> Unit,
    onExplore: () -> Unit
) {
    var shelfFilter by rememberSaveable { mutableStateOf("all") }
    val filtered = remember(recipes, shelfFilter) {
        when (shelfFilter) {
            "ready" -> recipes.filterNot { it.cooked }
            "cooked" -> recipes.filter { it.cooked }
            "favorites" -> recipes.filter { it.favorite }
            else -> recipes
        }
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, top = 16.dp, end = 20.dp, bottom = 112.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("YOUR SHELF", style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.1.sp), color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(8.dp))
            Text("Saved, cooked, and close at hand.", style = MaterialTheme.typography.displaySmall)
        }
        item {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("all" to "All", "ready" to "To cook", "cooked" to "Cooked", "favorites" to "Saved").forEach { (id, label) ->
                    FilterChip(selected = shelfFilter == id, onClick = { shelfFilter = id }, label = { Text(label) })
                }
            }
        }
        if (filtered.isEmpty()) {
            item {
                CulinaryEmptyState(
                    title = if (recipes.isEmpty()) "Nothing saved yet" else "Nothing in this view",
                    message = if (recipes.isEmpty()) "Explore recipes or save one from a Reel to begin your personal shelf." else "Choose another shelf filter to see more recipes."
                )
            }
            item {
                Button(onClick = onExplore, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp), shape = RoundedCornerShape(AppRadii.control)) { Text("Explore recipes") }
            }
        } else {
            items(filtered, key = { "saved-${it.id}" }, contentType = { "saved-recipe" }) { recipe ->
                ModernRecipeCard(
                    recipe = recipe,
                    onOpen = { onOpen(recipe) },
                    onToggleFavorite = { onToggleFavorite(recipe) },
                    onToggleCooked = { onToggleCooked(recipe) },
                    onStartCooking = { onStartCooking(recipe) }
                )
            }
        }
    }
}

@Composable
private fun LibraryTabScreen(
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
    onSaveReel: () -> Unit,
    onRetryInstagramImport: (Recipe) -> Unit,
    retryingInstagramImportIds: Set<Long>
) {
    var query by rememberSaveable { mutableStateOf("") }
    val searchIndex = remember(recipes) { buildRecipeSearchIndex(recipes) }
    val searchedRecipes = remember(query, searchIndex) {
        if (query.isBlank()) recipes else searchRecipes(searchIndex, query)
    }
    val filters = remember(recipes) {
        val builtIns = listOf(
            CategoryFilter("all", "All"),
            CategoryFilter("category:veg", "Veg"),
            CategoryFilter("category:nonveg", "Non-veg"),
            CategoryFilter("favorites", "Favorites", FilterIconType.Favorite),
            CategoryFilter("quick", "Quick (<20m)", FilterIconType.Timer),
            CategoryFilter("protein", "High Protein"),
            CategoryFilter("cooked", "Cooked", FilterIconType.Check)
        )
        val categoryFilters = listOf(
            CategoryFilter("category:breakfast", "Breakfast"),
            CategoryFilter("category:lunch", "Lunch"),
            CategoryFilter("category:dinner", "Dinner"),
            CategoryFilter("category:snack", "Snack"),
            CategoryFilter("category:other", "Other")
        )
        val builtInKeys = setOf("quick", "high protein", "vegetarian", "non-veg", "nonveg", "breakfast", "lunch", "dinner", "snack", "snacks")
        val tagFilters = TagNormalizer.normalizeAll(recipes.flatMap { it.tags })
            .filterNot { TagNormalizer.key(it) in builtInKeys }
            .sortedBy { it.lowercase() }
            .map { CategoryFilter("tag:${TagNormalizer.key(it)}", it) }
        builtIns + categoryFilters + tagFilters
    }

    val filteredRecipes = remember(selectedFilterId, searchedRecipes) {
        when (selectedFilterId) {
            "all" -> searchedRecipes
            "favorites" -> searchedRecipes.filter { it.favorite }
            "quick" -> searchedRecipes.filter(::isQuickRecipe)
            "protein" -> searchedRecipes.filter { recipe ->
                TagNormalizer.matches(recipe.tags, "High Protein")
            }
            "veg", "category:veg", "category:nonveg", "category:breakfast", "category:lunch", "category:dinner", "category:snack", "category:other" ->
                searchedRecipes.filter { it.matchesHomeCategory(selectedFilterId) }
            "cooked" -> searchedRecipes.filter { it.cooked }
            else -> if (selectedFilterId.startsWith("tag:")) {
                val tagKey = selectedFilterId.removePrefix("tag:")
                searchedRecipes.filter { recipe -> recipe.tags.any { TagNormalizer.key(it) == tagKey } }
            } else {
                searchedRecipes.filter { it.displayCategory().equals(selectedFilterId, ignoreCase = true) }
            }
        }
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize().imePadding(),
        contentPadding = PaddingValues(bottom = 112.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column(
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        "YOUR RECIPE INDEX",
                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.1.sp),
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text("Find something for the stove.", style = MaterialTheme.typography.headlineMedium)
                    Text(
                        if (recipes.isEmpty()) "Save a recipe or add one manually to start your index."
                        else "${recipes.size} ${if (recipes.size == 1) "recipe" else "recipes"} organized on your kitchen shelf.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(
                        onClick = onSaveReel,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                        shape = RoundedCornerShape(AppRadii.control)
                    ) {
                        Icon(Icons.Default.BookmarkAdd, contentDescription = null)
                        Spacer(Modifier.size(8.dp))
                        Text("Save a recipe", fontWeight = FontWeight.Bold)
                    }
                }
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search recipes or ingredients") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(AppRadii.surface)
                )
                if (draftCount > 0) {
                    OutlinedButton(
                        onClick = onOpenImports,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                        shape = RoundedCornerShape(AppRadii.control)
                    ) {
                        Icon(Icons.Default.Inbox, contentDescription = null)
                        Spacer(Modifier.size(8.dp))
                        Text("Imports waiting for review: $draftCount")
                    }
                }
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
                    title = if (selectedFilterId == "all") "Your first recipe starts with Share" else "Nothing here yet",
                    message = if (selectedFilterId == "all") {
                        "From Instagram, tap Share and choose InstaRecipe. We’ll keep the useful cooking bits here."
                    } else {
                        "Try another search or switch back to All."
                    }
                )
            }
        } else {
            items(filteredRecipes, key = { it.id }, contentType = { "recipe-card" }) { recipe ->
                val canRetryFromLink = remember(recipe) { recipe.canExtractFromLinkAgain() }
                val isRetrying = recipe.id in retryingInstagramImportIds
                Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                    ModernRecipeCard(
                        recipe = recipe,
                        onOpen = { onOpen(recipe) },
                        onToggleFavorite = { onToggleFavorite(recipe) },
                        onToggleCooked = { onToggleCooked(recipe) },
                        onStartCooking = { onStartCooking(recipe) },
                        onTagClick = onTagSelected,
                        footerActionLabel = if (canRetryFromLink) {
                            if (isRetrying) "Extracting from link…" else "Extract from link again"
                        } else null,
                        onFooterAction = if (canRetryFromLink) {
                            { onRetryInstagramImport(recipe) }
                        } else null,
                        footerActionEnabled = !isRetrying
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
    onRetryInstagramImport: (Recipe) -> Unit,
    retryingInstagramImportIds: Set<Long>,
    onTagSelected: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().imePadding(),
        contentPadding = PaddingValues(start = 20.dp, top = 14.dp, end = 20.dp, bottom = 112.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column {
                Text(
                    "Your imports",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "New finds land here while they become cookable recipes.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                        shape = MaterialTheme.shapes.extraSmall
                    ) {
                        Text(
                            "A SMALL PREP SHORTCUT",
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.9.sp),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Column {
                        Text("Already have the video?", style = MaterialTheme.typography.titleLarge)
                        Text(
                            "Drop it in here. You can review every ingredient before saving.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Button(
                        onClick = onImportVideo,
                        shape = RoundedCornerShape(AppRadii.control),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.PlayCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.size(4.dp))
                        Text("Choose a cooking video", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        if (drafts.isEmpty()) {
            item {
                CulinaryEmptyState(
                    title = "All caught up",
                    message = "Share a cooking Reel whenever you find one worth making."
                )
            }
        } else {
            items(drafts, key = { it.id }, contentType = { "recipe-card" }) { draft ->
                val canRetryFromLink = remember(draft) { draft.canExtractFromLinkAgain() }
                val isRestarting = draft.id in retryingInstagramImportIds
                ModernRecipeCard(
                    recipe = draft,
                    onOpen = { onOpen(draft) },
                    onToggleFavorite = { onToggleFavorite(draft) },
                    onToggleCooked = { onToggleCooked(draft) },
                    onStartCooking = { onStartCooking(draft) },
                    onTagClick = onTagSelected,
                    footerActionLabel = if (canRetryFromLink) {
                        if (isRestarting) "Starting extraction…" else "Extract from link again"
                    } else null,
                    onFooterAction = if (canRetryFromLink) {
                        { onRetryInstagramImport(draft) }
                    } else null,
                    footerActionEnabled = !isRestarting
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun InstagramLoginDialog(
    onDismiss: () -> Unit,
    onLoginSuccess: (String?) -> Unit
) {
    var loading by remember { mutableStateOf(true) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Link Instagram profile") },
        text = {
            Box(Modifier.fillMaxWidth().height(560.dp)) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { context ->
                        WebView(context).apply {
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.allowFileAccess = false
                            settings.allowContentAccess = false
                            settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_NEVER_ALLOW
                            CookieManager.getInstance().setAcceptCookie(true)
                            CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                            webViewClient = object : WebViewClient() {
                                private fun checkSession() {
                                    val cookies = CookieManager.getInstance().getCookie("https://www.instagram.com")
                                    if (!cookies.isNullOrBlank() && cookies.contains("sessionid=") && cookies.contains("ds_user_id=")) {
                                        CookieManager.getInstance().flush()
                                        onLoginSuccess(InstagramSessionManager.extractCookieValue(cookies, "ds_user_id"))
                                    }
                                }

                                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                    val host = request?.url?.host.orEmpty().lowercase()
                                    return !(request?.url?.scheme == "https" && (host == "instagram.com" || host.endsWith(".instagram.com")))
                                }

                                override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                                    loading = true
                                    checkSession()
                                }

                                override fun onPageFinished(view: WebView?, url: String?) {
                                    loading = false
                                    checkSession()
                                }
                            }
                            loadUrl("https://www.instagram.com/accounts/login/")
                        }
                    }
                )
                if (loading) CircularProgressIndicator(Modifier.align(Alignment.Center))
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SettingsScreen(
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    motionMode: MotionMode,
    onMotionModeChange: (MotionMode) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var apiKey by remember { mutableStateOf(GeminiRecipeExtractor.getApiKey(context)) }
    var backupApiKey by remember { mutableStateOf(GeminiRecipeExtractor.getBackupApiKey(context)) }
    var showApiKey by rememberSaveable { mutableStateOf(false) }
    var customResolver by remember { mutableStateOf(GeminiRecipeExtractor.getCustomResolver(context)) }
    var instagramProfile by remember { mutableStateOf(GeminiRecipeExtractor.getInstagramProfileUrl(context)) }

    var storageCleanedNotice by remember { mutableStateOf<String?>(null) }

    var testStatus by remember { mutableStateOf<String?>(null) }
    var isTesting by remember { mutableStateOf(false) }
    var advancedExpanded by rememberSaveable { mutableStateOf(false) }
    var instagramConnected by remember { mutableStateOf(InstagramSessionManager.isLoggedIn(context)) }
    var instagramUserId by remember { mutableStateOf(InstagramSessionManager.getUserId(context)) }
    var showInstagramLogin by rememberSaveable { mutableStateOf(false) }

    if (showInstagramLogin) {
        InstagramLoginDialog(
            onDismiss = { showInstagramLogin = false },
            onLoginSuccess = { userId ->
                instagramConnected = true
                instagramUserId = userId
                showInstagramLogin = false
            }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().imePadding(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text("Your kitchen", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("Make the journal feel like yours and keep recipe imports running.", color = MaterialTheme.colorScheme.onSurfaceVariant)
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

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = MaterialTheme.shapes.large,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Motion: ${motionMode.label}", style = MaterialTheme.typography.titleMedium)
                    Text(
                        motionMode.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MotionMode.entries.forEach { mode ->
                            FilterChip(
                                selected = motionMode == mode,
                                onClick = { onMotionModeChange(mode) },
                                label = { Text(mode.label) },
                                leadingIcon = if (motionMode == mode) {
                                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                } else null,
                                modifier = Modifier.semantics {
                                    stateDescription = if (motionMode == mode) "Selected" else "Not selected"
                                }
                            )
                        }
                    }
                    Text(
                        "Motion follows the system setting until you choose a mode here. Reduced and Off also apply to cooking and future promotional surfaces.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Recipe import connection
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(AppRadii.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Key, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text("Reel import connection", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        "Your primary Gemini key is tried first. If it is unavailable, InstaRecipe can use an optional backup key.",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    OutlinedTextField(
                        value = apiKey,
                        onValueChange = {
                            apiKey = it
                        },
                        label = { Text("Primary Gemini Auth key") },
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
                        label = { Text("Backup Gemini Auth key") },
                        placeholder = { Text("Optional backup key") },
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
                        "The backup key is used only when the primary key is rejected, rate-limited, unavailable, or takes too long.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        "Use current Auth keys from Google AI Studio; legacy Standard keys no longer work. Keys stay encrypted on this device.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        "Connected service: Google Gemini",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                testStatus = if (GeminiRecipeExtractor.setApiKeys(context, apiKey, backupApiKey)) {
                                    "Gemini keys saved securely on this device."
                                } else {
                                    "The Gemini keys could not be encrypted on this device."
                                }
                            },
                            enabled = !isTesting,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
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
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
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

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(AppRadii.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Instagram access", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        if (instagramConnected) "Connected for personal Reel imports${instagramUserId?.let { " • user $it" }.orEmpty()}"
                        else "Not connected. Public links may be blocked by Instagram's login wall.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (instagramConnected) {
                        OutlinedButton(
                            onClick = {
                                InstagramSessionManager.clearSession(context)
                                instagramConnected = false
                                instagramUserId = null
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) { Text("Disconnect and clear session") }
                    } else {
                        Button(
                            onClick = { showInstagramLogin = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) { Text("Link Instagram profile") }
                    }
                    Text(
                        "This uses an app-private Instagram WebView session for your personal imports. You can disconnect at any time.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(AppRadii.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Instagram profile", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        "Save a public creator profile so supported resolvers can use it as context when a Reel link is shared.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = instagramProfile,
                        onValueChange = {
                            instagramProfile = it
                            if (it.isBlank() || InstagramResolver.isInstagramProfileUrl(it)) {
                                GeminiRecipeExtractor.setInstagramProfileUrl(context, it)
                            }
                        },
                        label = { Text("Public profile link (optional)") },
                        placeholder = { Text("https://www.instagram.com/creator") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                    Text(
                        "This does not sign in to Instagram or read private posts. It only helps a configured resolver identify the creator.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (advancedExpanded) {
        // Storage & Auto-Deletion Policy
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(AppRadii.surface),
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
                shape = RoundedCornerShape(AppRadii.surface),
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
                shape = RoundedCornerShape(AppRadii.surface),
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

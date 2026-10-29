package com.instarecipe.app.ui.screens

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.Flow
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
    onImportScreenshot: () -> Unit,
    onImportLink: (String) -> Unit,
    onImportText: (String) -> Unit,
    resumeRecipe: Recipe?,
    isLibraryLoading: Boolean,
    onRetryInstagramImport: (Recipe) -> Unit,
    onApproveDraft: suspend (Recipe) -> Unit,
    retryingInstagramImportIds: Set<Long>,
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    motionMode: MotionMode,
    onMotionModeChange: (MotionMode) -> Unit
) {
    val savedRecipes = remember(recipes) { recipes.filter(Recipe::isCookbookReady) }
    val draftRecipes = remember(recipes) { recipes.filter(Recipe::belongsInImports) }
    val primaryTabs = remember { listOf(Tab.Home, Tab.Inbox, Tab.Settings) }
    val motion = LocalMotionMode.current
    var showAddSheet by rememberSaveable { mutableStateOf(false) }
    val tabStates = androidx.compose.runtime.saveable.rememberSaveableStateHolder()
    val context = LocalContext.current
    val hasConnection = remember(selectedTab, recipes) { GeminiRecipeExtractor.getApiKeys(context).isNotEmpty() }
    LaunchedEffect(selectedTab) {
        if (selectedTab == Tab.Explore || selectedTab == Tab.Collections || selectedTab == Tab.Saved) onTabSelected(Tab.Home)
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(selectedTab.label, style = MaterialTheme.typography.headlineSmall)
                        if (selectedTab == Tab.Home) Text(
                            "${savedRecipes.size} ${if (savedRecipes.size == 1) "recipe" else "recipes"}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    if (selectedTab != Tab.Settings) TextButton(onClick = { showAddSheet = true }, modifier = Modifier.heightIn(min = 48.dp)) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("Add recipe")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        floatingActionButton = {},
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp
            ) {
                primaryTabs.forEach { tab ->
                    val isSelected = selectedTab == tab
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { onTabSelected(tab) },
                        icon = {
                            Icon(
                                tab.icon,
                                contentDescription = null,
                                tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        label = {
                            Text(
                                if (tab == Tab.Inbox && draftRecipes.any { it.importStage(hasConnection) != ImportStage.Working }) "Imports (${draftRecipes.count { it.importStage(hasConnection) != ImportStage.Working }})" else tab.label,
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
            ) { activeTab -> tabStates.SaveableStateProvider(activeTab.name) {
                when (activeTab) {
                    Tab.Home, Tab.Explore, Tab.Collections, Tab.Saved -> RecipeLibraryScreen(
                        recipes = savedRecipes, draftCount = draftRecipes.size,
                        selectedFilterId = libraryFilterId, onFilterSelected = onLibraryFilterSelected,
                        onOpen = onOpen, onToggleFavorite = onToggleFavorite, onToggleCooked = onToggleCooked,
                        onStartCooking = onStartCooking, onTagSelected = onTagSelected,
                        onOpenImports = { onTabSelected(Tab.Inbox) }, onAdd = { showAddSheet = true },
                        resumeRecipe = resumeRecipe, isLoading = isLibraryLoading
                    )
                    Tab.Inbox -> ImportsScreen(
                        drafts = draftRecipes, hasConnection = hasConnection,
                        retryingIds = retryingInstagramImportIds, onOpen = onOpen,
                        onSettings = { onTabSelected(Tab.Settings) }, onRetry = onRetryInstagramImport, onApprove = onApproveDraft,
                        onAdd = { showAddSheet = true }, isLoading = isLibraryLoading
                    )
                    Tab.Settings -> SettingsScreen(themeMode, onThemeModeChange, motionMode, onMotionModeChange)
                }
            } }

        }
    }

    if (showAddSheet) {
        AddRecipeSheet(
            onDismiss = { showAddSheet = false }, onImportLink = onImportLink,
            onImportText = onImportText, onImportVideo = onImportVideo, onImportScreenshot = onImportScreenshot, onWriteRecipe = onAdd,
            hasConnection = hasConnection,
            onSettings = { showAddSheet = false; onTabSelected(Tab.Settings) }
        )
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
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("PREFERENCES", style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.1.sp), color = MaterialTheme.colorScheme.primary)
                Text("Make it yours", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text("Appearance, recipe imports and local storage.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
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
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    runCatching {
                                        context.startActivity(
                                            Intent(Intent.ACTION_VIEW, Uri.parse("https://aistudio.google.com/apikey"))
                                        )
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Get key (AI Studio)")
                            }
                            OutlinedButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                    val pasted = clipboard?.primaryClip?.getItemAt(0)?.coerceToText(context)?.toString()?.trim().orEmpty()
                                    if (pasted.isBlank()) {
                                        testStatus = "Clipboard is empty. Copy your Gemini Auth key first."
                                    } else {
                                        apiKey = pasted
                                        isTesting = true
                                        testStatus = null
                                        coroutineScope.launch {
                                            val res = GeminiRecipeExtractor.testConnections(pasted, backupApiKey)
                                            if (res.isSuccess && !GeminiRecipeExtractor.setApiKeys(context, pasted, backupApiKey)) {
                                                testStatus = "Connection worked, but the keys could not be encrypted on this device."
                                                isTesting = false
                                                return@launch
                                            }
                                            isTesting = false
                                            testStatus = res.getOrElse { it.message ?: "Failed" }
                                        }
                                    }
                                },
                                enabled = !isTesting,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Paste & test")
                            }
                        }
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
            TextButton(
                onClick = { advancedExpanded = !advancedExpanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (advancedExpanded) "Hide advanced settings" else "Advanced settings")
            }
        }

        if (advancedExpanded) {
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

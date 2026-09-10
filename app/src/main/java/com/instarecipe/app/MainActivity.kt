package com.instarecipe.app

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import android.graphics.Bitmap
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.instarecipe.app.ui.components.CategoryFilter
import com.instarecipe.app.ui.components.CategoryFilterRow
import com.instarecipe.app.ui.components.CookingModeDialog
import com.instarecipe.app.ui.components.FilterIconType
import com.instarecipe.app.ui.components.ModernRecipeCard
import com.instarecipe.app.ui.components.ServingScaler
import com.instarecipe.app.ui.components.ServingScalerSelector
import com.instarecipe.app.ui.theme.AlertPaprika
import com.instarecipe.app.ui.theme.AlertPaprikaContainer
import com.instarecipe.app.ui.theme.BorderSubtle
import com.instarecipe.app.ui.theme.CharcoalSlate
import com.instarecipe.app.ui.theme.DeepBasil
import com.instarecipe.app.ui.theme.DeepBasilContainer
import com.instarecipe.app.ui.theme.GoldenHoney
import com.instarecipe.app.ui.theme.GoldenHoneyContainer
import com.instarecipe.app.ui.theme.HerbMuted
import com.instarecipe.app.ui.theme.HerbSubtle
import com.instarecipe.app.ui.theme.InstaRecipeTheme
import com.instarecipe.app.ui.theme.ThemeMode
import com.instarecipe.app.ui.theme.SuccessSage
import com.instarecipe.app.ui.theme.SuccessSageContainer
import com.instarecipe.app.ui.theme.ToastedSesame
import com.instarecipe.app.ui.theme.WarmSaffron
import com.instarecipe.app.ui.theme.WarmSaffronContainer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate

sealed interface SharePayload {
    data class Text(val content: String) : SharePayload
    data class Video(val uri: Uri) : SharePayload
}

data class ExtractionProgress(
    val isExtracting: Boolean = false,
    val stage: String = "",
    val sourceUrl: String? = null,
    val error: String? = null
)

private object ThemePreferences {
    private const val PREFERENCES = "instarecipe_appearance"
    private const val THEME_MODE = "theme_mode"

    fun load(context: Context): ThemeMode {
        val saved = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
            .getString(THEME_MODE, ThemeMode.System.name)
        return ThemeMode.entries.firstOrNull { it.name == saved } ?: ThemeMode.System
    }

    fun save(context: Context, mode: ThemeMode) {
        context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
            .edit()
            .putString(THEME_MODE, mode.name)
            .apply()
    }
}

class MainActivity : ComponentActivity() {
    private var sharePayloadState = mutableStateOf<SharePayload?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        sharePayloadState.value = intent.extractSharePayload()
        setContent {
            InstaRecipeApp(
                initialSharePayload = sharePayloadState.value,
                onShareHandled = { sharePayloadState.value = null }
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        sharePayloadState.value = intent.extractSharePayload()
    }
}

private fun Intent.extractSharePayload(): SharePayload? {
    if (action != Intent.ACTION_SEND) return null

    // Check for video share (gallery, downloaded reel, files)
    if (type?.startsWith("video/") == true) {
        val videoUri: Uri? = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
        } else {
            @Suppress("DEPRECATION")
            getParcelableExtra(Intent.EXTRA_STREAM)
        } ?: data

        if (videoUri != null) {
            return SharePayload.Video(videoUri)
        }
    }

    // Check for text/plain share (Instagram app link share)
    if (type == "text/plain") {
        val text = getStringExtra(Intent.EXTRA_TEXT)?.trim()?.takeIf { it.isNotBlank() }
        if (text != null) {
            return SharePayload.Text(text)
        }
    }

    return null
}

private enum class Tab(val label: String, val icon: ImageVector) {
    Library("Library", Icons.Default.Home),
    Inbox("Inbox", Icons.Default.Inbox),
    Search("Search", Icons.Default.Search),
    Settings("Settings", Icons.Default.Settings)
}

enum class RecipeStatus {
    Draft,
    Saved
}

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

@Composable
private fun InstaRecipeApp(
    initialSharePayload: SharePayload?,
    onShareHandled: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var themeMode by remember { mutableStateOf(ThemePreferences.load(context)) }
    val recipes = remember {
        mutableStateListOf<Recipe>().apply {
            addAll(RecipeStore.load(context).ifEmpty { demoRecipes() })
        }
    }
    var selectedTab by rememberSaveable { mutableStateOf(Tab.Library) }
    var editingRecipe by remember { mutableStateOf<Recipe?>(null) }
    var viewingRecipe by remember { mutableStateOf<Recipe?>(null) }
    var cookingRecipe by remember { mutableStateOf<Recipe?>(null) }

    var extractionProgress by remember { mutableStateOf(ExtractionProgress()) }

    LaunchedEffect(recipes.toList()) {
        RecipeStore.save(context, recipes)
    }

    // Clean any lingering temporary video files on startup to keep storage minimal
    LaunchedEffect(Unit) {
        InstagramResolver.cleanCachedReelVideos(context)
    }

    val importVideoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val apiKey = GeminiRecipeExtractor.getApiKey(context)
            if (apiKey.isBlank()) {
                extractionProgress = ExtractionProgress(
                    isExtracting = false,
                    error = "Gemini API key is required to analyze videos. Please add it in Settings."
                )
                return@rememberLauncherForActivityResult
            }
            coroutineScope.launch {
                extractionProgress = ExtractionProgress(
                    isExtracting = true,
                    stage = "Reading video file from gallery..."
                )
                var videoFile: File? = null
                try {
                    videoFile = copyUriToCacheFile(context, uri)
                        ?: throw RuntimeException("Could not read video file.")

                    val extracted = GeminiRecipeExtractor.extractRecipe(
                        context = context,
                        videoFile = videoFile,
                        textCaption = null,
                        sourceUrl = "Imported reel video",
                        creatorName = null,
                        onStatus = { stage ->
                            extractionProgress = extractionProgress.copy(stage = stage)
                        }
                    )

                    val newRecipe = Recipe(
                        id = (recipes.maxOfOrNull { it.id } ?: 0L) + 1L,
                        title = extracted.title,
                        sourceUrl = "",
                        creator = extracted.creator,
                        category = extracted.category,
                        tags = extracted.tags,
                        ingredients = extracted.ingredients,
                        steps = extracted.steps,
                        notes = extracted.notes,
                        favorite = false,
                        cooked = false,
                        status = RecipeStatus.Draft,
                        savedDate = LocalDate.now().toString()
                    )
                    recipes.add(0, newRecipe)
                    editingRecipe = newRecipe
                    extractionProgress = ExtractionProgress(isExtracting = false)
                } catch (e: Exception) {
                    extractionProgress = ExtractionProgress(
                        isExtracting = false,
                        error = "Video extraction failed: ${e.message}"
                    )
                } finally {
                    videoFile?.delete()
                    InstagramResolver.cleanCachedReelVideos(context)
                }
            }
        }
    }

    // Process incoming shares from Instagram
    LaunchedEffect(initialSharePayload) {
        val payload = initialSharePayload ?: return@LaunchedEffect
        onShareHandled()

        when (payload) {
            is SharePayload.Text -> {
                val instagramUrl = InstagramResolver.extractInstagramUrl(payload.content)
                val targetUrl = instagramUrl ?: payload.content

                // Check if already saved
                if (recipes.any { it.sourceUrl.isNotBlank() && it.sourceUrl == targetUrl }) {
                    selectedTab = Tab.Library
                    return@LaunchedEffect
                }

                selectedTab = Tab.Inbox
                val apiKey = GeminiRecipeExtractor.getApiKey(context)

                if (apiKey.isBlank()) {
                    // Create basic draft and prompt for API key
                    val draft = Recipe(
                        id = (recipes.maxOfOrNull { it.id } ?: 0L) + 1L,
                        title = "Instagram Recipe Draft",
                        sourceUrl = targetUrl,
                        creator = "",
                        category = "Saved to try",
                        tags = listOf("Instagram", "Needs review"),
                        ingredients = emptyList(),
                        steps = emptyList(),
                        notes = "${payload.content}\n\n[Add your Gemini API Key in Settings to automatically extract ingredients & steps from reels.]",
                        favorite = false,
                        cooked = false,
                        status = RecipeStatus.Draft,
                        savedDate = LocalDate.now().toString()
                    )
                    recipes.add(0, draft)
                    extractionProgress = ExtractionProgress(
                        isExtracting = false,
                        error = "Gemini API key is required for AI video extraction. You can set it up in Settings."
                    )
                    return@LaunchedEffect
                }

                // Start full AI resolution and extraction pipeline
                coroutineScope.launch {
                    extractionProgress = ExtractionProgress(
                        isExtracting = true,
                        stage = "Checking Instagram reel & resolving video...",
                        sourceUrl = targetUrl
                    )
                    var resolutionFile: File? = null
                    try {
                        val customResolver = GeminiRecipeExtractor.getCustomResolver(context).ifBlank { null }
                        val resolution = InstagramResolver.resolveAndDownload(
                            context = context,
                            instagramUrl = targetUrl,
                            customResolverUrl = customResolver,
                            supplementaryText = payload.content,
                            onStatusUpdate = { stage ->
                                extractionProgress = extractionProgress.copy(stage = stage)
                            }
                        )
                        resolutionFile = resolution.videoFile

                        val hasVideo = resolution.videoFile != null && resolution.videoFile.exists() && resolution.videoFile.length() > 0
                        val candidateCaption = resolution.caption.takeIf { GeminiRecipeExtractor.hasSubstantiveRecipeContent(it) }
                            ?: InstagramResolver.extractCaptionFromSharedText(payload.content, targetUrl)

                        // GUARD: Prevent hallucinating a steak recipe if no video or recipe caption is available
                        if (!hasVideo && candidateCaption.isNullOrBlank()) {
                            val draft = Recipe(
                                id = (recipes.maxOfOrNull { it.id } ?: 0L) + 1L,
                                title = resolution.creator?.let { "$it's Recipe Draft" } ?: "Instagram Recipe Draft",
                                sourceUrl = targetUrl,
                                creator = resolution.creator.orEmpty(),
                                category = "Saved to try",
                                tags = listOf("Instagram", "Needs Video/Caption"),
                                ingredients = emptyList(),
                                steps = emptyList(),
                                notes = "Source: $targetUrl\n\n[Instagram restricted automated video download. Tap 'Attach Video' to pick the downloaded reel, or paste the post caption to extract with Gemini AI.]",
                                favorite = false,
                                cooked = false,
                                status = RecipeStatus.Draft,
                                savedDate = LocalDate.now().toString()
                            )
                            recipes.add(0, draft)
                            editingRecipe = draft
                            extractionProgress = ExtractionProgress(
                                isExtracting = false,
                                error = "Instagram restricted automated video download. You can attach the saved reel video from your gallery or paste the post caption to extract."
                            )
                            return@launch
                        }

                        val extracted = GeminiRecipeExtractor.extractRecipe(
                            context = context,
                            videoFile = resolution.videoFile,
                            textCaption = candidateCaption,
                            sourceUrl = targetUrl,
                            creatorName = resolution.creator,
                            onStatus = { stage ->
                                extractionProgress = extractionProgress.copy(stage = stage)
                            }
                        )

                        val newRecipe = Recipe(
                            id = (recipes.maxOfOrNull { it.id } ?: 0L) + 1L,
                            title = extracted.title,
                            sourceUrl = extracted.sourceUrl,
                            creator = extracted.creator,
                            category = extracted.category,
                            tags = extracted.tags,
                            ingredients = extracted.ingredients,
                            steps = extracted.steps,
                            notes = extracted.notes,
                            favorite = false,
                            cooked = false,
                            status = RecipeStatus.Draft,
                            savedDate = LocalDate.now().toString()
                        )
                        recipes.add(0, newRecipe)
                        editingRecipe = newRecipe
                        extractionProgress = ExtractionProgress(isExtracting = false)
                    } catch (e: Exception) {
                        // Create fallback draft so user doesn't lose the share
                        val fallback = Recipe(
                            id = (recipes.maxOfOrNull { it.id } ?: 0L) + 1L,
                            title = "Instagram Recipe Draft",
                            sourceUrl = targetUrl,
                            creator = "",
                            category = "Saved to try",
                            tags = listOf("Instagram", "Manual review"),
                            ingredients = emptyList(),
                            steps = emptyList(),
                            notes = "${payload.content}\n\n[Extraction notice: ${e.message}]",
                            favorite = false,
                            cooked = false,
                            status = RecipeStatus.Draft,
                            savedDate = LocalDate.now().toString()
                        )
                        recipes.add(0, fallback)
                        editingRecipe = fallback
                        extractionProgress = ExtractionProgress(
                            isExtracting = false,
                            error = "Could not extract video automatically: ${e.message}. You can attach the video or paste the caption."
                        )
                    } finally {
                        resolutionFile?.delete()
                        InstagramResolver.cleanCachedReelVideos(context)
                    }
                }
            }

            is SharePayload.Video -> {
                selectedTab = Tab.Inbox
                val apiKey = GeminiRecipeExtractor.getApiKey(context)

                if (apiKey.isBlank()) {
                    extractionProgress = ExtractionProgress(
                        isExtracting = false,
                        error = "Gemini API key is required to analyze shared video files. Please add it in Settings."
                    )
                    return@LaunchedEffect
                }

                coroutineScope.launch {
                    extractionProgress = ExtractionProgress(
                        isExtracting = true,
                        stage = "Reading shared video file..."
                    )
                    var videoFile: File? = null
                    try {
                        videoFile = copyUriToCacheFile(context, payload.uri)
                            ?: throw RuntimeException("Could not read shared video file.")

                        val extracted = GeminiRecipeExtractor.extractRecipe(
                            context = context,
                            videoFile = videoFile,
                            textCaption = null,
                            sourceUrl = "Shared video file",
                            creatorName = null,
                            onStatus = { stage ->
                                extractionProgress = extractionProgress.copy(stage = stage)
                            }
                        )

                        val newRecipe = Recipe(
                            id = (recipes.maxOfOrNull { it.id } ?: 0L) + 1L,
                            title = extracted.title,
                            sourceUrl = "",
                            creator = extracted.creator,
                            category = extracted.category,
                            tags = extracted.tags,
                            ingredients = extracted.ingredients,
                            steps = extracted.steps,
                            notes = extracted.notes,
                            favorite = false,
                            cooked = false,
                            status = RecipeStatus.Draft,
                            savedDate = LocalDate.now().toString()
                        )
                        recipes.add(0, newRecipe)
                        editingRecipe = newRecipe
                        extractionProgress = ExtractionProgress(isExtracting = false)
                    } catch (e: Exception) {
                        extractionProgress = ExtractionProgress(
                            isExtracting = false,
                            error = "Video analysis failed: ${e.message}"
                        )
                    } finally {
                        videoFile?.delete()
                        InstagramResolver.cleanCachedReelVideos(context)
                    }
                }
            }
        }
    }

    InstaRecipeTheme(themeMode = themeMode) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            when {
                editingRecipe != null -> RecipeEditor(
                    recipe = editingRecipe,
                    onCancel = { editingRecipe = null },
                    onSave = { saved ->
                        val index = recipes.indexOfFirst { it.id == saved.id }
                        if (index >= 0) recipes[index] = saved else recipes.add(0, saved)
                        editingRecipe = null
                        viewingRecipe = saved
                    },
                    nextId = (recipes.maxOfOrNull { it.id } ?: 0L) + 1L,
                    onTriggerAiExtraction = { url, notes, onResult ->
                        coroutineScope.launch {
                            extractionProgress = ExtractionProgress(
                                isExtracting = true,
                                stage = "Extracting recipe details..."
                            )
                            var resolutionFile: File? = null
                            try {
                                val cleanUrl = InstagramResolver.extractInstagramUrl(url) ?: url
                                val customResolver = GeminiRecipeExtractor.getCustomResolver(context).ifBlank { null }
                                val resolution = InstagramResolver.resolveAndDownload(
                                    context = context,
                                    instagramUrl = cleanUrl,
                                    customResolverUrl = customResolver,
                                    supplementaryText = notes
                                )
                                resolutionFile = resolution.videoFile
                                val candidateCaption = resolution.caption.takeIf { GeminiRecipeExtractor.hasSubstantiveRecipeContent(it) }
                                    ?: notes.takeIf { GeminiRecipeExtractor.hasSubstantiveRecipeContent(it) }

                                val extracted = GeminiRecipeExtractor.extractRecipe(
                                    context = context,
                                    videoFile = resolution.videoFile,
                                    textCaption = candidateCaption,
                                    sourceUrl = cleanUrl,
                                    creatorName = resolution.creator
                                )
                                onResult(extracted)
                                extractionProgress = ExtractionProgress(isExtracting = false)
                            } catch (e: Exception) {
                                extractionProgress = ExtractionProgress(
                                    isExtracting = false,
                                    error = "AI Extraction: ${e.message}"
                                )
                            } finally {
                                resolutionFile?.delete()
                                InstagramResolver.cleanCachedReelVideos(context)
                            }
                        }
                    }
                )

                viewingRecipe != null -> RecipeDetail(
                    recipe = viewingRecipe!!,
                    onBack = { viewingRecipe = null },
                    onEdit = { editingRecipe = viewingRecipe },
                    onToggleFavorite = {
                        recipes.updateRecipe(viewingRecipe!!.id) { copy(favorite = !favorite) }
                        viewingRecipe = recipes.first { it.id == viewingRecipe!!.id }
                    },
                    onToggleCooked = {
                        recipes.updateRecipe(viewingRecipe!!.id) { copy(cooked = !cooked) }
                        viewingRecipe = recipes.first { it.id == viewingRecipe!!.id }
                    },
                    onStartCooking = { cookingRecipe = viewingRecipe }
                )

                else -> MainScaffold(
                    recipes = recipes,
                    selectedTab = selectedTab,
                    extractionProgress = extractionProgress,
                    onDismissError = { extractionProgress = extractionProgress.copy(error = null) },
                    onTabSelected = { selectedTab = it },
                    onOpen = { viewingRecipe = it },
                    onToggleFavorite = { recipe ->
                        recipes.updateRecipe(recipe.id) { copy(favorite = !favorite) }
                    },
                    onToggleCooked = { recipe ->
                        recipes.updateRecipe(recipe.id) { copy(cooked = !cooked) }
                    },
                    onStartCooking = { recipe -> cookingRecipe = recipe },
                    onAdd = {
                        editingRecipe = Recipe(
                            id = 0,
                            title = "",
                            sourceUrl = "",
                            creator = "",
                            category = "Saved to try",
                            tags = emptyList(),
                            ingredients = emptyList(),
                            steps = emptyList(),
                            notes = "",
                            favorite = false,
                            cooked = false,
                            status = RecipeStatus.Draft,
                            savedDate = LocalDate.now().toString()
                        )
                    },
                    onImportVideo = { importVideoLauncher.launch("video/*") },
                    themeMode = themeMode,
                    onThemeModeChange = { selectedMode ->
                        themeMode = selectedMode
                        ThemePreferences.save(context, selectedMode)
                    }
                )
            }

            // Interactive Cooking Mode Overlay
            if (cookingRecipe != null) {
                CookingModeDialog(
                    recipeTitle = cookingRecipe!!.title,
                    steps = cookingRecipe!!.steps,
                    ingredients = cookingRecipe!!.ingredients,
                    onClose = { cookingRecipe = null },
                    onFinishAndMarkCooked = {
                        recipes.updateRecipe(cookingRecipe!!.id) { copy(cooked = true) }
                        if (viewingRecipe?.id == cookingRecipe?.id) {
                            viewingRecipe = viewingRecipe?.copy(cooked = true)
                        }
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainScaffold(
    recipes: List<Recipe>,
    selectedTab: Tab,
    extractionProgress: ExtractionProgress,
    onDismissError: () -> Unit,
    onTabSelected: (Tab) -> Unit,
    onOpen: (Recipe) -> Unit,
    onToggleFavorite: (Recipe) -> Unit,
    onToggleCooked: (Recipe) -> Unit,
    onStartCooking: (Recipe) -> Unit,
    onAdd: () -> Unit,
    onImportVideo: () -> Unit,
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit
) {
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
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAdd,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Add Recipe", fontWeight = FontWeight.SemiBold) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(16.dp)
            )
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
                            Text("Gemini AI Video Extraction in Progress", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
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

            when (selectedTab) {
                Tab.Library -> LibraryTabScreen(
                    recipes = recipes.filter { it.status == RecipeStatus.Saved },
                    onOpen = onOpen,
                    onToggleFavorite = onToggleFavorite,
                    onToggleCooked = onToggleCooked,
                    onStartCooking = onStartCooking
                )

                Tab.Inbox -> InboxTabScreen(
                    drafts = recipes.filter { it.status == RecipeStatus.Draft },
                    onOpen = onOpen,
                    onToggleFavorite = onToggleFavorite,
                    onToggleCooked = onToggleCooked,
                    onStartCooking = onStartCooking,
                    onImportVideo = onImportVideo
                )

                Tab.Search -> SearchTabScreen(
                    recipes = recipes,
                    onOpen = onOpen,
                    onToggleFavorite = onToggleFavorite,
                    onToggleCooked = onToggleCooked,
                    onStartCooking = onStartCooking
                )

                Tab.Settings -> SettingsScreen(
                    themeMode = themeMode,
                    onThemeModeChange = onThemeModeChange
                )
            }
        }
    }
}

@Composable
private fun LibraryTabScreen(
    recipes: List<Recipe>,
    onOpen: (Recipe) -> Unit,
    onToggleFavorite: (Recipe) -> Unit,
    onToggleCooked: (Recipe) -> Unit,
    onStartCooking: (Recipe) -> Unit
) {
    var selectedFilterId by rememberSaveable { mutableStateOf("all") }

    val filters = remember(recipes) {
        listOf(
            CategoryFilter("all", "All"),
            CategoryFilter("favorites", "Favorites", FilterIconType.Favorite),
            CategoryFilter("quick", "Quick (<20m)", FilterIconType.Timer),
            CategoryFilter("protein", "High Protein"),
            CategoryFilter("veg", "Vegetarian"),
            CategoryFilter("cooked", "Cooked", FilterIconType.Check)
        )
    }

    val filteredRecipes = remember(selectedFilterId, recipes) {
        when (selectedFilterId) {
            "all" -> recipes
            "favorites" -> recipes.filter { it.favorite }
            "quick" -> recipes.filter { recipe ->
                recipe.notes.contains("min", ignoreCase = true) ||
                    recipe.tags.any { it.contains("quick", ignoreCase = true) }
            }
            "protein" -> recipes.filter { recipe ->
                recipe.tags.any { it.contains("protein", ignoreCase = true) } ||
                    recipe.title.contains("protein", ignoreCase = true)
            }
            "veg" -> recipes.filter { recipe ->
                recipe.tags.any { it.contains("veg", ignoreCase = true) }
            }
            "cooked" -> recipes.filter { it.cooked }
            else -> recipes.filter { it.category.equals(selectedFilterId, ignoreCase = true) }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 80.dp),
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
            }
        }

        // Horizontal Category Filter Pills
        item {
            CategoryFilterRow(
                filters = filters,
                selectedFilterId = selectedFilterId,
                onFilterSelected = { selectedFilterId = it }
            )
        }

        if (filteredRecipes.isEmpty()) {
            item {
                CulinaryEmptyState(
                    title = if (selectedFilterId == "all") "Your Cookbook is Fresh" else "No matching recipes",
                    message = if (selectedFilterId == "all") {
                        "Share any Instagram Reel or cooking video with InstaRecipe, and Gemini AI will extract ingredients and instructions for you!"
                    } else {
                        "Try switching back to 'All' or adjusting your filter pills."
                    }
                )
            }
        } else {
            items(filteredRecipes, key = { it.id }) { recipe ->
                Box(modifier = Modifier.padding(horizontal = 20.dp)) {
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
}

@Composable
private fun InboxTabScreen(
    drafts: List<Recipe>,
    onOpen: (Recipe) -> Unit,
    onToggleFavorite: (Recipe) -> Unit,
    onToggleCooked: (Recipe) -> Unit,
    onStartCooking: (Recipe) -> Unit,
    onImportVideo: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column {
                Text(
                    "Inbox & Drafts",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Reels shared from Instagram waiting for your review and approval.",
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
                        Text("Import Saved Reel Video", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Text(
                            "Have a saved reel in your gallery? Pick it for multimodal AI extraction of ingredients, audio & steps.",
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
                        Text("Pick Video")
                    }
                }
            }
        }

        if (drafts.isEmpty()) {
            item {
                CulinaryEmptyState(
                    title = "Inbox Zero!",
                    message = "When you tap 'Share to InstaRecipe' in Instagram, incoming reels land here for rapid review before entering your cookbook."
                )
            }
        } else {
            items(drafts, key = { it.id }) { draft ->
                ModernRecipeCard(
                    recipe = draft,
                    onOpen = { onOpen(draft) },
                    onToggleFavorite = { onToggleFavorite(draft) },
                    onToggleCooked = { onToggleCooked(draft) },
                    onStartCooking = { onStartCooking(draft) }
                )
            }
        }
    }
}

@Composable
private fun SearchTabScreen(
    recipes: List<Recipe>,
    onOpen: (Recipe) -> Unit,
    onToggleFavorite: (Recipe) -> Unit,
    onToggleCooked: (Recipe) -> Unit,
    onStartCooking: (Recipe) -> Unit
) {
    var query by rememberSaveable { mutableStateOf("") }
    val filtered = remember(query, recipes) {
        recipes.filter { recipe ->
            val haystack = buildString {
                append(recipe.title).append(" ")
                append(recipe.creator).append(" ")
                append(recipe.category).append(" ")
                append(recipe.tags.joinToString(" ")).append(" ")
                append(recipe.ingredients.joinToString(" ")).append(" ")
                append(recipe.steps.joinToString(" ")).append(" ")
                append(recipe.notes)
            }
            haystack.contains(query, ignoreCase = true)
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Pantry & Recipe Search", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Search by ingredient, dish name, or chef...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                    shape = RoundedCornerShape(16.dp)
                )
            }
        }

        if (query.isBlank()) {
            item {
                CulinaryEmptyState(
                    title = "What's in your fridge?",
                    message = "Type ingredients like 'paneer', 'garlic', 'quinoa', or dishes like 'pasta' to find recipes."
                )
            }
        } else if (filtered.isEmpty()) {
            item {
                CulinaryEmptyState(
                    title = "No recipes found",
                    message = "No recipes matched '$query'. Try another ingredient or save a new reel!"
                )
            }
        } else {
            items(filtered, key = { it.id }) { recipe ->
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
private fun InstagramLoginDialog(
    onDismiss: () -> Unit,
    onLoginSuccess: (userId: String?) -> Unit
) {
    var isLoading by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier
            .fillMaxWidth()
            .height(640.dp),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Instagram Login", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Official Instagram login page", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }
        },
        text = {
            Box(modifier = Modifier.fillMaxSize()) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        WebView(ctx).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            settings.apply {
                                javaScriptEnabled = true
                                domStorageEnabled = true
                                userAgentString = "Mozilla/5.0 (Linux; Android 11; SAMSUNG SM-G973U) AppleWebKit/537.36 (KHTML, like Gecko) SamsungBrowser/14.2 Chrome/87.0.4280.141 Mobile Safari/537.36"
                            }
                            val cookieManager = CookieManager.getInstance()
                            cookieManager.setAcceptCookie(true)
                            cookieManager.setAcceptThirdPartyCookies(this, true)

                            webViewClient = object : WebViewClient() {
                                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                    super.onPageStarted(view, url, favicon)
                                    isLoading = true
                                    checkCookies(ctx)
                                }

                                override fun onPageFinished(view: WebView?, url: String?) {
                                    super.onPageFinished(view, url)
                                    isLoading = false
                                    checkCookies(ctx)
                                }

                                private fun checkCookies(c: Context) {
                                    val cookies = cookieManager.getCookie("https://www.instagram.com")
                                    if (!cookies.isNullOrBlank() && cookies.contains("sessionid=") && cookies.contains("ds_user_id=")) {
                                        InstagramSessionManager.saveSession(c, cookies)
                                        val userId = InstagramSessionManager.getUserId(c)
                                        onLoginSuccess(userId)
                                    }
                                }
                            }

                            loadUrl("https://www.instagram.com/accounts/login/")
                        }
                    }
                )

                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        confirmButton = {}
    )
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
    var showApiKey by rememberSaveable { mutableStateOf(false) }
    var selectedModel by remember { mutableStateOf(GeminiRecipeExtractor.getSelectedModel(context)) }
    var customResolver by remember { mutableStateOf(GeminiRecipeExtractor.getCustomResolver(context)) }

    var isIgConnected by remember { mutableStateOf(InstagramSessionManager.isLoggedIn(context)) }
    var igUserId by remember { mutableStateOf(InstagramSessionManager.getUserId(context)) }
    var showIgLoginDialog by remember { mutableStateOf(false) }
    var igFeedbackMessage by remember { mutableStateOf<String?>(null) }
    var storageCleanedNotice by remember { mutableStateOf<String?>(null) }

    var testStatus by remember { mutableStateOf<String?>(null) }
    var isTesting by remember { mutableStateOf(false) }

    if (showIgLoginDialog) {
        InstagramLoginDialog(
            onDismiss = { showIgLoginDialog = false },
            onLoginSuccess = { uid ->
                isIgConnected = true
                igUserId = uid
                showIgLoginDialog = false
                igFeedbackMessage = "Connected to Instagram! Reels will now download automatically."
            }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text("Settings & Diagnostics", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("Configure AI multimodal extraction and video resolution.", color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                        Text("Google Gemini Multimodal AI", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        "Extracts structured cooking steps, ingredients with units, prep/cook times, and chef tips by analyzing video reels, voiceover audio, and captions.",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    OutlinedTextField(
                        value = apiKey,
                        onValueChange = {
                            apiKey = it
                            GeminiRecipeExtractor.setApiKey(context, it)
                        },
                        label = { Text("Gemini API Key") },
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

                    Text(
                        "Get a free API key at aistudio.google.com with high quota.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text("Gemini AI Model", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        GeminiRecipeExtractor.AVAILABLE_MODELS.forEach { model ->
                            FilterChip(
                                selected = selectedModel == model,
                                onClick = {
                                    selectedModel = model
                                    GeminiRecipeExtractor.setSelectedModel(context, model)
                                },
                                label = {
                                    Text(
                                        when (model) {
                                            GeminiRecipeExtractor.MODEL_GEMINI_2_5_FLASH -> "2.5 Flash (Recommended)"
                                            GeminiRecipeExtractor.MODEL_GEMINI_2_0_FLASH -> "2.0 Flash"
                                            GeminiRecipeExtractor.MODEL_GEMINI_1_5_FLASH -> "1.5 Flash"
                                            GeminiRecipeExtractor.MODEL_GEMINI_3_1_FLASH_LITE -> "3.1 Flash-Lite"
                                            GeminiRecipeExtractor.MODEL_GEMINI_3_8_FLASH -> "3.8 Flash"
                                            else -> model
                                        }
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Button(
                            onClick = {
                                isTesting = true
                                testStatus = null
                                coroutineScope.launch {
                                    val res = GeminiRecipeExtractor.testConnection(apiKey, selectedModel)
                                    isTesting = false
                                    testStatus = res.getOrElse { it.message ?: "Failed" }
                                }
                            },
                            enabled = apiKey.isNotBlank() && !isTesting,
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
                                Text("Test Connection")
                            }
                        }
                    }

                    if (testStatus != null) {
                        val isSuccess = testStatus?.contains("successfully", ignoreCase = true) == true
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

        // Instagram Account Session Section (Direct Downloads)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.PlayCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text("Instagram Session (Direct Download)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        "Connecting your Instagram session allows InstaRecipe to download reel MP4s directly without third-party resolvers. Your credentials stay on Instagram's official page.",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isIgConnected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                if (isIgConnected) Icons.Default.CheckCircle else Icons.Default.Warning,
                                contentDescription = null,
                                tint = if (isIgConnected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    if (isIgConnected) "Connected to Instagram" else "Not Connected (Fallback Mode)",
                                    fontWeight = FontWeight.Bold,
                                    color = if (isIgConnected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    if (isIgConnected) "User ID: ${igUserId ?: "Active Session"}" else "Unauthenticated requests may be blocked by Instagram.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    if (isIgConnected) {
                        OutlinedButton(
                            onClick = {
                                InstagramSessionManager.clearSession(context)
                                isIgConnected = false
                                igUserId = null
                                igFeedbackMessage = "Instagram session disconnected."
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.size(6.dp))
                            Text("Disconnect & Clear Session", fontWeight = FontWeight.SemiBold)
                        }
                    } else {
                        Button(
                            onClick = { showIgLoginDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.PlayCircle, contentDescription = null)
                            Spacer(Modifier.size(6.dp))
                            Text("Connect Instagram (In-App Login)", fontWeight = FontWeight.Bold)
                        }
                    }

                    if (igFeedbackMessage != null) {
                        Text(
                            text = igFeedbackMessage!!,
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

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
                        Text("Zero-Waste Storage Policy", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        "All temporary downloaded reel videos and imported cache files are automatically deleted immediately after AI extraction to keep your phone storage clean.",
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
                        Text("Clean Video Cache Now", fontWeight = FontWeight.SemiBold)
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
                        Text("Instagram Video Resolver", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        "InstaRecipe can use a custom Cobalt/resolver endpoint to download reel videos for Gemini multimodal analysis.",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    OutlinedTextField(
                        value = customResolver,
                        onValueChange = {
                            customResolver = it
                            GeminiRecipeExtractor.setCustomResolver(context, it)
                        },
                        label = { Text("Custom Resolver URL (Optional)") },
                        placeholder = { Text("https://my-cobalt-instance.example.com") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                    Text(
                        "Leave empty to use default public resolvers. If Instagram blocks anonymous downloads, attach the downloaded reel video directly!",
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
                    Text("How to Extract Recipes from Instagram", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                    Text("Option 1: Video Multimodal AI (Best & Most Accurate)", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                    Text("• In Instagram, tap the Share icon -> tap 'Download' to save the reel video to your phone.", style = MaterialTheme.typography.bodySmall)
                    Text("• In InstaRecipe, tap 'Pick Video' in Inbox or 'Attach Reel Video' in the editor.", style = MaterialTheme.typography.bodySmall)
                    Text("• Gemini AI watches the video, listens to the spoken steps, and reads on-screen text overlays!", style = MaterialTheme.typography.bodySmall)

                    Spacer(Modifier.height(4.dp))
                    Text("Option 2: Recipe Caption AI Extraction", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                    Text("• In Instagram, copy the post caption or recipe description.", style = MaterialTheme.typography.bodySmall)
                    Text("• In InstaRecipe, tap 'Paste Caption & Extract AI'. Gemini structures all ingredients, quantities, and steps automatically.", style = MaterialTheme.typography.bodySmall)

                    Spacer(Modifier.height(4.dp))
                    Text("Option 3: Direct Link Share", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                    Text("• Share any Reel link directly to InstaRecipe. The app inspects post captions and attempts video resolution automatically.", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun RecipeDetail(
    recipe: Recipe,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onToggleFavorite: () -> Unit,
    onToggleCooked: () -> Unit,
    onStartCooking: () -> Unit
) {
    val context = LocalContext.current
    var servingScale by rememberSaveable { mutableFloatStateOf(1.0f) }
    val checkedIngredients = remember { mutableStateListOf<String>() }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(recipe.title.ifBlank { "Recipe" }, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Bold) },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back", color = MaterialTheme.colorScheme.primary) } },
                actions = { TextButton(onClick = onEdit) { Text("Edit", color = MaterialTheme.colorScheme.primary) } },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Info Card
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    text = recipe.category.ifBlank { "Recipe" }.uppercase(),
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }

                            if (recipe.cooked) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                                        Text("Cooked Before", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        Text(
                            text = recipe.title,
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        if (recipe.creator.isNotBlank()) {
                            Text("By ${recipe.creator}", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                        }

                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            recipe.tags.forEach { AssistChip(onClick = {}, label = { Text(it) }) }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            FilledTonalButton(
                                onClick = onToggleFavorite,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    if (recipe.favorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    contentDescription = null,
                                    tint = if (recipe.favorite) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.size(6.dp))
                                Text(if (recipe.favorite) "Favorited" else "Favorite")
                            }

                            FilledTonalButton(
                                onClick = onToggleCooked,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Restaurant, contentDescription = null, tint = if (recipe.cooked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(Modifier.size(6.dp))
                                Text(if (recipe.cooked) "Cooked" else "Mark Cooked")
                            }
                        }
                    }
                }
            }

            // Primary Action: Start Hands-Free Cooking Mode
            if (recipe.steps.isNotEmpty()) {
                item {
                    Button(
                        onClick = onStartCooking,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(24.dp))
                        Spacer(Modifier.size(8.dp))
                        Text("Start Cooking Mode", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Instagram Reel Launcher Button
            if (recipe.sourceUrl.isNotBlank()) {
                item {
                    OutlinedButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            val uri = Uri.parse(recipe.sourceUrl)
                            context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.secondary),
                        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.secondary)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null)
                        Spacer(Modifier.size(8.dp))
                        Text("Watch Original Instagram Reel", fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // Ingredients Section with Serving Scaler & Interactive Checkbox
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Ingredients", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            ServingScalerSelector(
                                currentScale = servingScale,
                                onScaleSelected = { servingScale = it }
                            )
                        }

                        if (recipe.ingredients.isEmpty()) {
                            Text("No ingredients listed yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            recipe.ingredients.forEachIndexed { index, rawIngredient ->
                                val scaledIngredient = ServingScaler.scaleIngredient(rawIngredient, servingScale)
                                val key = "$index-$scaledIngredient"
                                val isChecked = checkedIngredients.contains(key)

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            if (isChecked) checkedIngredients.remove(key) else checkedIngredients.add(key)
                                        }
                                        .padding(vertical = 4.dp)
                                ) {
                                    Checkbox(
                                        checked = isChecked,
                                        onCheckedChange = { checked ->
                                            if (checked) checkedIngredients.add(key) else checkedIngredients.remove(key)
                                        },
                                        colors = CheckboxDefaults.colors(
                                            checkedColor = MaterialTheme.colorScheme.primary,
                                            checkmarkColor = MaterialTheme.colorScheme.onPrimary
                                        )
                                    )
                                    Spacer(Modifier.size(6.dp))
                                    Text(
                                        text = scaledIngredient,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = if (isChecked) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface,
                                        textDecoration = if (isChecked) TextDecoration.LineThrough else null
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Steps Section
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text("Instructions", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        if (recipe.steps.isEmpty()) {
                            Text("No steps listed yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            recipe.steps.forEachIndexed { index, step ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primaryContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "${index + 1}",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    Text(
                                        text = step,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Notes, Prep Times & Chef Tips Section
            if (recipe.notes.isNotBlank()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(18.dp))
                                Text("Chef Notes & Prep Times", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            }
                            Text(recipe.notes, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CulinaryEmptyState(
    title: String,
    message: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Restaurant,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            }
            Text(
                title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 22.sp
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecipeEditor(
    recipe: Recipe?,
    onCancel: () -> Unit,
    onSave: (Recipe) -> Unit,
    nextId: Long,
    onTriggerAiExtraction: ((url: String, notes: String, onResult: (ExtractedRecipeData) -> Unit) -> Unit)? = null
) {
    var title by rememberSaveable(recipe?.id) { mutableStateOf(recipe?.title.orEmpty()) }
    var sourceUrl by rememberSaveable(recipe?.id) { mutableStateOf(recipe?.sourceUrl.orEmpty()) }
    var creator by rememberSaveable(recipe?.id) { mutableStateOf(recipe?.creator.orEmpty()) }
    var category by rememberSaveable(recipe?.id) { mutableStateOf(recipe?.category ?: "Saved to try") }
    var tags by rememberSaveable(recipe?.id) { mutableStateOf(recipe?.tags?.joinToString(", ").orEmpty()) }
    var ingredients by rememberSaveable(recipe?.id) { mutableStateOf(recipe?.ingredients?.joinToString("\n").orEmpty()) }
    var steps by rememberSaveable(recipe?.id) { mutableStateOf(recipe?.steps?.joinToString("\n").orEmpty()) }
    var notes by rememberSaveable(recipe?.id) { mutableStateOf(recipe?.notes.orEmpty()) }

    var isExtractingFromEditor by remember { mutableStateOf(false) }
    var editorFeedbackMessage by remember { mutableStateOf<String?>(null) }
    var showPasteCaptionDialog by remember { mutableStateOf(false) }
    var captionDialogInput by remember { mutableStateOf("") }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val editorVideoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val apiKey = GeminiRecipeExtractor.getApiKey(context)
            if (apiKey.isBlank()) {
                editorFeedbackMessage = "Please add your Gemini API Key in Settings first."
                return@rememberLauncherForActivityResult
            }
            coroutineScope.launch {
                isExtractingFromEditor = true
                editorFeedbackMessage = "Watching video with Gemini AI..."
                var videoFile: File? = null
                try {
                    videoFile = copyUriToCacheFile(context, uri)
                        ?: throw RuntimeException("Could not read video file.")

                    val extracted = GeminiRecipeExtractor.extractRecipe(
                        context = context,
                        videoFile = videoFile,
                        textCaption = notes.takeIf { GeminiRecipeExtractor.hasSubstantiveRecipeContent(it) },
                        sourceUrl = sourceUrl.ifBlank { "Attached reel video" },
                        creatorName = creator.ifBlank { null }
                    )

                    title = extracted.title
                    if (creator.isBlank() || creator.startsWith("http")) creator = extracted.creator
                    category = extracted.category
                    tags = extracted.tags.joinToString(", ")
                    ingredients = extracted.ingredients.joinToString("\n")
                    steps = extracted.steps.joinToString("\n")
                    if (extracted.notes.isNotBlank()) notes = extracted.notes
                    editorFeedbackMessage = "Recipe extracted from video successfully!"
                } catch (e: Exception) {
                    editorFeedbackMessage = "Video extraction failed: ${e.message}"
                } finally {
                    videoFile?.delete()
                    InstagramResolver.cleanCachedReelVideos(context)
                    isExtractingFromEditor = false
                }
            }
        }
    }

    if (showPasteCaptionDialog) {
        AlertDialog(
            onDismissRequest = { showPasteCaptionDialog = false },
            title = { Text("Paste Recipe Caption") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Paste the Instagram reel caption or description. Gemini AI will parse the exact ingredients and steps.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = captionDialogInput,
                        onValueChange = { captionDialogInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 5,
                        placeholder = { Text("Paste caption text here...") }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val textToExtract = captionDialogInput.trim()
                        showPasteCaptionDialog = false
                        if (textToExtract.isNotBlank()) {
                            val apiKey = GeminiRecipeExtractor.getApiKey(context)
                            if (apiKey.isBlank()) {
                                editorFeedbackMessage = "Please add your Gemini API Key in Settings first."
                                return@Button
                            }
                            coroutineScope.launch {
                                isExtractingFromEditor = true
                                editorFeedbackMessage = "Extracting recipe from caption with Gemini AI..."
                                try {
                                    val extracted = GeminiRecipeExtractor.extractRecipe(
                                        context = context,
                                        videoFile = null,
                                        textCaption = textToExtract,
                                        sourceUrl = sourceUrl.ifBlank { "Pasted caption" },
                                        creatorName = creator.ifBlank { null }
                                    )
                                    title = extracted.title
                                    if (creator.isBlank() || creator.startsWith("http")) creator = extracted.creator
                                    category = extracted.category
                                    tags = extracted.tags.joinToString(", ")
                                    ingredients = extracted.ingredients.joinToString("\n")
                                    steps = extracted.steps.joinToString("\n")
                                    if (extracted.notes.isNotBlank()) {
                                        notes = if (notes.isNotBlank()) "$notes\n\n${extracted.notes}" else extracted.notes
                                    }
                                    editorFeedbackMessage = "Recipe extracted from caption successfully!"
                                } catch (e: Exception) {
                                    editorFeedbackMessage = "Caption extraction failed: ${e.message}"
                                } finally {
                                    isExtractingFromEditor = false
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Extract Recipe")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPasteCaptionDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Review Recipe", fontWeight = FontWeight.Bold) },
                navigationIcon = { TextButton(onClick = onCancel) { Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant) } },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Text(
                    "Review and adjust the extracted ingredients and steps before saving to your cookbook.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            if (editorFeedbackMessage != null) {
                item {
                    val isErr = editorFeedbackMessage!!.contains("failed", ignoreCase = true) ||
                        editorFeedbackMessage!!.contains("error", ignoreCase = true)
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isErr) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                editorFeedbackMessage!!,
                                color = if (isErr) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { editorFeedbackMessage = null }) {
                                Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = if (isErr) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }

            // Quick AI Tools Toolbar
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("AI Extraction Tools", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { editorVideoPickerLauncher.launch("video/*") },
                                enabled = !isExtractingFromEditor,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.PlayCircle, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.size(4.dp))
                                Text("Attach Video", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }

                            OutlinedButton(
                                onClick = {
                                    captionDialogInput = notes
                                    showPasteCaptionDialog = true
                                },
                                enabled = !isExtractingFromEditor,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.size(4.dp))
                                Text("Paste Caption", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }

                        if (onTriggerAiExtraction != null && sourceUrl.isNotBlank()) {
                            Button(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = {
                                    isExtractingFromEditor = true
                                    editorFeedbackMessage = "Re-extracting from Instagram link..."
                                    onTriggerAiExtraction(sourceUrl, notes) { extracted ->
                                        isExtractingFromEditor = false
                                        title = extracted.title
                                        if (creator.isBlank() || creator.startsWith("http")) creator = extracted.creator
                                        category = extracted.category
                                        tags = extracted.tags.joinToString(", ")
                                        ingredients = extracted.ingredients.joinToString("\n")
                                        steps = extracted.steps.joinToString("\n")
                                        if (extracted.notes.isNotBlank()) notes = extracted.notes
                                        editorFeedbackMessage = "Extracted successfully!"
                                    }
                                },
                                enabled = !isExtractingFromEditor,
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                if (isExtractingFromEditor) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                                    Spacer(Modifier.size(8.dp))
                                    Text("Extracting with AI...")
                                } else {
                                    Icon(Icons.Default.Refresh, contentDescription = null)
                                    Spacer(Modifier.size(6.dp))
                                    Text("Re-extract Link with AI", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            item { Field("Title", title) { title = it } }
            item { Field("Instagram Link", sourceUrl) { sourceUrl = it } }
            item { Field("Creator / Chef", creator) { creator = it } }
            item { Field("Category", category) { category = it } }
            item { Field("Tags (comma separated)", tags) { tags = it } }
            item { Field("Ingredients (one per line)", ingredients, minLines = 5) { ingredients = it } }
            item { Field("Cooking Steps (one per line)", steps, minLines = 5) { steps = it } }
            item { Field("Notes, Tips, & Prep Times", notes, minLines = 4) { notes = it } }

            item {
                Button(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(16.dp),
                    onClick = {
                        onSave(
                            Recipe(
                                id = recipe?.id?.takeIf { it != 0L } ?: nextId,
                                title = title.ifBlank { "Untitled Recipe" },
                                sourceUrl = sourceUrl.trim(),
                                creator = creator.trim(),
                                category = category.ifBlank { "Saved to try" },
                                tags = tags.split(",").map { it.trim() }.filter { it.isNotBlank() },
                                ingredients = ingredients.lines().map { it.trim() }.filter { it.isNotBlank() },
                                steps = steps.lines().map { it.trim() }.filter { it.isNotBlank() },
                                notes = notes.trim(),
                                favorite = recipe?.favorite ?: false,
                                cooked = recipe?.cooked ?: false,
                                status = RecipeStatus.Saved,
                                savedDate = recipe?.savedDate ?: LocalDate.now().toString()
                            )
                        )
                    }
                ) {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text("Approve & Save to Cookbook", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun Field(label: String, value: String, minLines: Int = 1, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        minLines = minLines,
        shape = RoundedCornerShape(12.dp)
    )
}

private fun copyUriToCacheFile(context: Context, uri: Uri): File? {
    return runCatching {
        val cacheDir = File(context.cacheDir, "shared_reels").apply { mkdirs() }
        val file = File(cacheDir, "shared_video_${System.currentTimeMillis()}.mp4")
        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(file).use { output ->
                input.copyTo(output)
            }
        }
        if (file.exists() && file.length() > 0) file else null
    }.getOrNull()
}

private fun MutableList<Recipe>.updateRecipe(id: Long, transform: Recipe.() -> Recipe) {
    val index = indexOfFirst { it.id == id }
    if (index >= 0) this[index] = this[index].transform()
}

private object RecipeStore {
    private const val PREFS = "insta_recipe_store"
    private const val KEY_RECIPES = "recipes"

    fun load(context: Context): List<Recipe> {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_RECIPES, null)
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            List(array.length()) { index -> array.getJSONObject(index).toRecipe() }
        }.getOrDefault(emptyList())
    }

    fun save(context: Context, recipes: List<Recipe>) {
        val array = JSONArray()
        recipes.forEach { array.put(it.toJson()) }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_RECIPES, array.toString())
            .apply()
    }
}

private fun Recipe.toJson(): JSONObject = JSONObject()
    .put("id", id)
    .put("title", title)
    .put("sourceUrl", sourceUrl)
    .put("creator", creator)
    .put("category", category)
    .put("tags", JSONArray(tags))
    .put("ingredients", JSONArray(ingredients))
    .put("steps", JSONArray(steps))
    .put("notes", notes)
    .put("favorite", favorite)
    .put("cooked", cooked)
    .put("status", status.name)
    .put("savedDate", savedDate)

private fun JSONObject.toRecipe(): Recipe = Recipe(
    id = optLong("id"),
    title = optString("title"),
    sourceUrl = optString("sourceUrl"),
    creator = optString("creator"),
    category = optString("category", "Saved to try"),
    tags = optJSONArray("tags").toStringList(),
    ingredients = optJSONArray("ingredients").toStringList(),
    steps = optJSONArray("steps").toStringList(),
    notes = optString("notes"),
    favorite = optBoolean("favorite"),
    cooked = optBoolean("cooked"),
    status = runCatching { RecipeStatus.valueOf(optString("status")) }.getOrDefault(RecipeStatus.Draft),
    savedDate = optString("savedDate", LocalDate.now().toString())
)

private fun JSONArray?.toStringList(): List<String> {
    if (this == null) return emptyList()
    return List(length()) { index -> optString(index) }.filter { it.isNotBlank() }
}

private fun demoRecipes(): List<Recipe> = listOf(
    Recipe(
        id = 1,
        title = "Paneer Pepper Toast",
        sourceUrl = "https://www.instagram.com/reel/example",
        creator = "Demo Chef",
        category = "Snacks",
        tags = listOf("Quick", "Vegetarian", "High protein"),
        ingredients = listOf("200g paneer cubes", "4 bread slices", "1 diced bell pepper", "1 tsp chilli flakes"),
        steps = listOf("Toast the bread until golden.", "Sauté paneer cubes and bell peppers with seasoning.", "Assemble on toast and serve hot."),
        notes = "Prep time: 5 mins. Cook time: 10 mins.",
        favorite = true,
        cooked = false,
        status = RecipeStatus.Saved,
        savedDate = LocalDate.now().toString()
    )
)

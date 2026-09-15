package com.instarecipe.app

import android.content.Context
import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.BackHandler
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.instarecipe.app.ui.components.CookingModeDialog
import com.instarecipe.app.ui.screens.MainScaffold
import com.instarecipe.app.ui.screens.RecipeDetail
import com.instarecipe.app.ui.screens.RecipeEditor
import com.instarecipe.app.ui.theme.InstaRecipeTheme
import com.instarecipe.app.ui.theme.ThemeMode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDate

sealed interface SharePayload {
    data class Text(val content: String) : SharePayload
    data class Video(val uri: Uri) : SharePayload
}


internal object ThemePreferences {
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
    private val appViewModel by viewModels<InstaRecipeViewModel> {
        InstaRecipeViewModel.Factory(application)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        sharePayloadState.value = intent.extractSharePayload()
        setContent {
            InstaRecipeApp(
                viewModel = appViewModel,
                initialSharePayload = sharePayloadState.value,
                onShareHandled = {
                    sharePayloadState.value = null
                    setIntent(Intent(this@MainActivity, MainActivity::class.java))
                }
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        sharePayloadState.value = intent.extractSharePayload()
    }
}

internal fun shouldQueueInstagramExtraction(recipes: List<Recipe>, targetUrl: String): Boolean =
    recipes.none { recipe ->
        recipe.sourceUrl.isNotBlank() && recipe.sourceUrl == targetUrl && recipe.status == RecipeStatus.Saved
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

        if (videoUri?.scheme == ContentResolver.SCHEME_CONTENT) {
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

internal enum class Tab(val label: String, val icon: ImageVector) {
    Library("Cookbook", Icons.Default.Home),
    Inbox("Imports", Icons.Default.Inbox),
    Settings("Settings", Icons.Default.Settings)
}

private val recipeStateSaver = Saver<MutableState<Recipe?>, Bundle>(
    save = { state ->
        Bundle().apply {
            state.value?.let { recipe ->
                putBoolean("present", true)
                putLong("id", recipe.id)
                putString("title", recipe.title)
                putString("sourceUrl", recipe.sourceUrl)
                putString("creator", recipe.creator)
                putString("category", recipe.category)
                putStringArrayList("tags", ArrayList(recipe.tags))
                putStringArrayList("ingredients", ArrayList(recipe.ingredients))
                putStringArrayList("steps", ArrayList(recipe.steps))
                putString("notes", recipe.notes)
                putBoolean("favorite", recipe.favorite)
                putBoolean("cooked", recipe.cooked)
                putString("status", recipe.status.name)
                putString("savedDate", recipe.savedDate)
            }
        }
    },
    restore = { bundle ->
        mutableStateOf(
            if (!bundle.getBoolean("present")) null else Recipe(
                id = bundle.getLong("id"),
                title = bundle.getString("title").orEmpty(),
                sourceUrl = bundle.getString("sourceUrl").orEmpty(),
                creator = bundle.getString("creator").orEmpty(),
                category = bundle.getString("category").orEmpty(),
                tags = bundle.getStringArrayList("tags").orEmpty(),
                ingredients = bundle.getStringArrayList("ingredients").orEmpty(),
                steps = bundle.getStringArrayList("steps").orEmpty(),
                notes = bundle.getString("notes").orEmpty(),
                favorite = bundle.getBoolean("favorite"),
                cooked = bundle.getBoolean("cooked"),
                status = runCatching { RecipeStatus.valueOf(bundle.getString("status").orEmpty()) }
                    .getOrDefault(RecipeStatus.Draft),
                savedDate = bundle.getString("savedDate").orEmpty()
            )
        )
    }
)

@Composable
private fun InstaRecipeApp(
    viewModel: InstaRecipeViewModel,
    initialSharePayload: SharePayload?,
    onShareHandled: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var themeMode by remember { mutableStateOf(ThemePreferences.load(context)) }
    val recipes by viewModel.recipes.collectAsStateWithLifecycle()
    val videoExtractionState by viewModel.videoExtractionState.collectAsStateWithLifecycle()
    var selectedTab by rememberSaveable { mutableStateOf(Tab.Library) }
    var libraryFilterId by rememberSaveable { mutableStateOf("all") }
    var editingRecipe by rememberSaveable(saver = recipeStateSaver) { mutableStateOf<Recipe?>(null) }
    var viewingRecipe by rememberSaveable(saver = recipeStateSaver) { mutableStateOf<Recipe?>(null) }
    var cookingRecipe by rememberSaveable(saver = recipeStateSaver) { mutableStateOf<Recipe?>(null) }

    var extractionProgress by remember { mutableStateOf(ExtractionProgress()) }
    var pendingSharePayload by remember { mutableStateOf<SharePayload?>(null) }
    var acceptedSharedText by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(initialSharePayload) {
        if (initialSharePayload != null) pendingSharePayload = initialSharePayload
    }

    LaunchedEffect(videoExtractionState.completedRecipe?.id) {
        videoExtractionState.completedRecipe?.let { persisted ->
            editingRecipe = persisted
            viewModel.acknowledgeCompletedVideoRecipe(persisted.id)
        }
    }

    pendingSharePayload?.let { pending ->
        AlertDialog(
            onDismissRequest = {
                pendingSharePayload = null
                onShareHandled()
            },
            title = { Text("Import shared recipe?") },
            text = {
                Text(
                    when (pending) {
                        is SharePayload.Text ->
                            "InstaRecipe will read the shared link and create an editable recipe from its caption or video."
                        is SharePayload.Video ->
                            "InstaRecipe will temporarily upload this video to create an editable recipe."
                    }
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        when (pending) {
                            is SharePayload.Text -> acceptedSharedText = pending.content
                            is SharePayload.Video -> {
                                selectedTab = Tab.Inbox
                                viewModel.importSharedVideo(pending.uri)
                                onShareHandled()
                            }
                        }
                        pendingSharePayload = null
                    }
                ) { Text("Continue") }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        pendingSharePayload = null
                        onShareHandled()
                    }
                ) { Text("Cancel") }
            }
        )
    }

    BackHandler(enabled = cookingRecipe != null || editingRecipe != null || viewingRecipe != null) {
        when {
            cookingRecipe != null -> cookingRecipe = null
            editingRecipe != null -> editingRecipe = null
            viewingRecipe != null -> viewingRecipe = null
        }
    }

    // Clean any lingering temporary video files on startup to keep storage minimal
    LaunchedEffect(Unit) {
        InstagramResolver.cleanCachedReelVideos(context)
    }

    val importVideoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) viewModel.importGalleryVideo(uri)
    }

    // Process accepted text shares; video shares transfer to the ViewModel synchronously on acceptance.
    LaunchedEffect(acceptedSharedText) {
        val sharedText = acceptedSharedText ?: return@LaunchedEffect

        val targetUrl = InstagramResolver.extractInstagramUrl(sharedText)
        if (targetUrl == null) {
            extractionProgress = ExtractionProgress(
                error = "The shared text does not contain a valid HTTPS Instagram post or reel link."
            )
            onShareHandled()
            acceptedSharedText = null
            return@LaunchedEffect
        }

        // Saved recipes are deduplicated; failed/pending drafts may be reclaimed by WorkManager.
        if (!shouldQueueInstagramExtraction(recipes, targetUrl)) {
            selectedTab = Tab.Library
            onShareHandled()
            acceptedSharedText = null
            return@LaunchedEffect
        }

        selectedTab = Tab.Inbox
        if (GeminiRecipeExtractor.getApiKeys(context).isEmpty()) {
            // Create basic draft and prompt for API key
            val draft = Recipe(
                id = 0L,
                title = "Instagram Recipe Draft",
                sourceUrl = targetUrl,
                creator = "",
                category = "Saved to try",
                tags = listOf("Instagram", "Needs review"),
                ingredients = emptyList(),
                steps = emptyList(),
                notes = "$sharedText\n\n[Add a primary or backup Gemini API key in Settings, then share this link again to extract it automatically.]",
                favorite = false,
                cooked = false,
                status = RecipeStatus.Draft,
                savedDate = LocalDate.now().toString()
            )
            try {
                viewModel.upsertAndAwait(draft)
                onShareHandled()
                extractionProgress = ExtractionProgress(
                    isExtracting = false,
                    error = "A Gemini API key is required for AI extraction. Add a primary or backup key in Settings."
                )
                acceptedSharedText = null
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                extractionProgress = ExtractionProgress(
                    error = "The shared recipe could not be saved. Tap Continue to retry."
                )
                pendingSharePayload = SharePayload.Text(sharedText)
                acceptedSharedText = null
            }
            return@LaunchedEffect
        }

        try {
            InstagramExtractionWork.enqueue(
                context = context,
                sourceUrl = targetUrl,
                sharedText = sharedText
            )
            onShareHandled()
            acceptedSharedText = null
            extractionProgress = ExtractionProgress(isExtracting = false)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            extractionProgress = ExtractionProgress(
                error = "Background extraction could not be scheduled. Tap Continue to retry."
            )
            pendingSharePayload = SharePayload.Text(sharedText)
            acceptedSharedText = null
        }
    }

    val displayedExtractionProgress = if (
        videoExtractionState.progress.isExtracting || videoExtractionState.progress.error != null
    ) {
        videoExtractionState.progress
    } else {
        extractionProgress
    }

    InstaRecipeTheme(themeMode = themeMode) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            when {
                editingRecipe != null -> RecipeEditor(
                    recipe = editingRecipe,
                    onCancel = { editingRecipe = null },
                    onSave = { saved ->
                        viewModel.upsert(saved) { persisted ->
                            editingRecipe = null
                            viewingRecipe = persisted
                        }
                    },
                    onTriggerAiExtraction = { url, notes, onResult ->
                        coroutineScope.launch {
                            extractionProgress = ExtractionProgress(
                                isExtracting = true,
                                stage = "Extracting recipe details..."
                            )
                            var resolutionFile: File? = null
                            try {
                                val cleanUrl = InstagramResolver.extractInstagramUrl(url)
                                    ?: throw IllegalArgumentException("Enter a valid HTTPS Instagram post or reel link.")
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
                                onResult(Result.success(extracted))
                                extractionProgress = ExtractionProgress(isExtracting = false)
                            } catch (cancelled: CancellationException) {
                                throw cancelled
                            } catch (e: Exception) {
                                onResult(Result.failure(e))
                                extractionProgress = ExtractionProgress(
                                    isExtracting = false,
                                    error = "Couldn't create recipe: ${e.message}"
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
                    onDelete = {
                        viewModel.delete(viewingRecipe!!.id)
                        viewingRecipe = null
                    },
                    onToggleFavorite = {
                        val updated = viewingRecipe!!.copy(favorite = !viewingRecipe!!.favorite)
                        viewModel.upsert(updated)
                        viewingRecipe = updated
                    },
                    onToggleCooked = {
                        val updated = viewingRecipe!!.copy(cooked = !viewingRecipe!!.cooked)
                        viewModel.upsert(updated)
                        viewingRecipe = updated
                    },
                    onStartCooking = { cookingRecipe = viewingRecipe },
                    onTagSelected = { tag ->
                        libraryFilterId = "tag:${TagNormalizer.key(tag)}"
                        viewingRecipe = null
                        selectedTab = Tab.Library
                    }
                )

                else -> MainScaffold(
                    recipes = recipes,
                    selectedTab = selectedTab,
                    extractionProgress = displayedExtractionProgress,
                    onDismissError = {
                        if (videoExtractionState.progress.error != null) {
                            viewModel.dismissVideoExtractionError()
                        } else {
                            extractionProgress = extractionProgress.copy(error = null)
                        }
                    },
                    onTabSelected = { selectedTab = it },
                    libraryFilterId = libraryFilterId,
                    onLibraryFilterSelected = { libraryFilterId = it },
                    onTagSelected = { tag ->
                        libraryFilterId = "tag:${TagNormalizer.key(tag)}"
                        selectedTab = Tab.Library
                    },
                    onOpen = { viewingRecipe = it },
                    onToggleFavorite = { recipe ->
                        viewModel.update(recipe.id) { copy(favorite = !favorite) }
                    },
                    onToggleCooked = { recipe ->
                        viewModel.update(recipe.id) { copy(cooked = !cooked) }
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
                        viewModel.update(cookingRecipe!!.id) { copy(cooked = true) }
                        if (viewingRecipe?.id == cookingRecipe?.id) {
                            viewingRecipe = viewingRecipe?.copy(cooked = true)
                        }
                    }
                )
            }
        }
    }
}

@Composable
internal fun CulinaryEmptyState(
    title: String,
    message: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = MaterialTheme.shapes.medium,
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

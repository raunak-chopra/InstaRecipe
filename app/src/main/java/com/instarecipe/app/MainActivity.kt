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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Person
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
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
import com.instarecipe.app.ui.components.CookPhotoStore
import com.instarecipe.app.ui.screens.MainScaffold
import com.instarecipe.app.ui.screens.RecipeDetail
import com.instarecipe.app.ui.screens.RecipeEditor
import com.instarecipe.app.ui.theme.InstaRecipeTheme
import com.instarecipe.app.ui.theme.MotionMode
import com.instarecipe.app.ui.theme.ThemeMode
import com.instarecipe.app.ui.theme.defaultMotionMode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
            .getString(THEME_MODE, ThemeMode.Light.name)
        return ThemeMode.entries.firstOrNull { it.name == saved } ?: ThemeMode.Light
    }

    fun save(context: Context, mode: ThemeMode) {
        context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
            .edit()
            .putString(THEME_MODE, mode.name)
            .apply()
    }
}

internal object MotionPreferences {
    private const val PREFERENCES = "instarecipe_appearance"
    private const val MOTION_MODE = "motion_mode"

    fun load(context: Context): MotionMode {
        val preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
        if (!preferences.contains(MOTION_MODE)) return defaultMotionMode()
        val saved = preferences.getString(MOTION_MODE, null)
        return MotionMode.entries.firstOrNull { it.name == saved } ?: defaultMotionMode()
    }

    fun save(context: Context, mode: MotionMode) {
        context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
            .edit()
            .putString(MOTION_MODE, mode.name)
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

internal fun shouldQueueInstagramExtraction(recipes: List<Recipe>, targetUrl: String): Boolean {
    val normalizedTarget = normalizedSourceUrl(targetUrl) ?: return true
    return recipes.none { recipe ->
        recipe.status == RecipeStatus.Saved && normalizedSourceUrl(recipe.sourceUrl) == normalizedTarget
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
    Home("Shelf", Icons.Default.Home),
    /** Kept only so previously saved activity state can be restored after upgrading. */
    Explore("Explore", Icons.Default.Search),
    Collections("Collections", Icons.AutoMirrored.Filled.MenuBook),
    Saved("Saved", Icons.Default.Bookmark),
    /** A focused sub-route: imports remain reachable but are not a sixth primary destination. */
    Inbox("Imports", Icons.Default.Inbox),
    Settings("You", Icons.Default.Person)
}

@Composable
private fun InstaRecipeApp(
    viewModel: InstaRecipeViewModel,
    initialSharePayload: SharePayload?,
    onShareHandled: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var themeMode by remember { mutableStateOf(ThemePreferences.load(context)) }
    var motionMode by remember { mutableStateOf(MotionPreferences.load(context)) }
    val recipes by viewModel.recipes.collectAsStateWithLifecycle()
    val videoExtractionState by viewModel.videoExtractionState.collectAsStateWithLifecycle()
    val instagramRetryPhases by viewModel.instagramRetryPhases.collectAsStateWithLifecycle()
    val instagramRetryError by viewModel.instagramRetryError.collectAsStateWithLifecycle()
    var selectedTab by rememberSaveable { mutableStateOf(Tab.Home) }
    var libraryFilterId by rememberSaveable { mutableStateOf("all") }
    // Persist only route identity. Detail and cooking screens resolve the current Room row,
    // while RecipeEditor owns its unsaved field draft through its own rememberSaveable state.
    var editingRecipeId by rememberSaveable { mutableStateOf<Long?>(null) }
    var viewingRecipeId by rememberSaveable { mutableStateOf<Long?>(null) }
    var cookingRecipeId by rememberSaveable { mutableStateOf<Long?>(null) }
    val mainStateHolder = rememberSaveableStateHolder()
    var captureRecipe by remember { mutableStateOf<Recipe?>(null) }
    var pendingCookPhotoFile by remember { mutableStateOf<File?>(null) }
    var cookPhotoError by remember { mutableStateOf<String?>(null) }
    val retryingInstagramImportIds = instagramRetryPhases.keys
    val blankRecipe = remember {
        Recipe(
            id = 0,
            title = "",
            sourceUrl = "",
            creator = "",
            category = "",
            tags = emptyList(),
            ingredients = emptyList(),
            steps = emptyList(),
            notes = "",
            favorite = false,
            cooked = false,
            status = RecipeStatus.Draft,
            savedDate = LocalDate.now().toString()
        )
    }
    val currentEditingRecipe = editingRecipeId?.let { id ->
        if (id == 0L) blankRecipe else recipes.firstOrNull { it.id == id }
    }
    val currentViewingRecipe = viewingRecipeId?.let { id -> recipes.firstOrNull { it.id == id } }
    val currentCookingRecipe = cookingRecipeId?.let { id -> recipes.firstOrNull { it.id == id } }

    LaunchedEffect(editingRecipeId, viewingRecipeId, cookingRecipeId, recipes) {
        if (editingRecipeId != null && editingRecipeId != 0L && currentEditingRecipe == null) {
            editingRecipeId = null
        }
        if (viewingRecipeId != null && currentViewingRecipe == null) {
            viewingRecipeId = null
        }
        if (cookingRecipeId != null && currentCookingRecipe == null) {
            cookingRecipeId = null
        }
    }

    var extractionProgress by remember { mutableStateOf(ExtractionProgress()) }
    var pendingSharePayload by remember { mutableStateOf<SharePayload?>(null) }
    var acceptedSharedText by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingReviewRecipeId by rememberSaveable { mutableStateOf<Long?>(null) }

    LaunchedEffect(initialSharePayload) {
        if (initialSharePayload != null) pendingSharePayload = initialSharePayload
    }

    LaunchedEffect(viewModel) {
        viewModel.instagramRetryEffects.collect { effect ->
            when (effect) {
                InstagramRetryEffect.OpenSettings -> {
                    viewingRecipeId = null
                    selectedTab = Tab.Settings
                }
                is InstagramRetryEffect.OpenReview -> {
                    pendingReviewRecipeId = effect.recipeId
                }
            }
        }
    }

    LaunchedEffect(pendingReviewRecipeId, recipes) {
        val recipeId = pendingReviewRecipeId ?: return@LaunchedEffect
        val candidate = recipes.firstOrNull { it.id == recipeId }
        if (editingRecipeId == null && viewingRecipeId == null &&
            candidate != null && candidate.status == RecipeStatus.Draft &&
            INSTAGRAM_PROCESSING_TAG !in candidate.tags
        ) {
            viewingRecipeId = null
            editingRecipeId = candidate.id
            pendingReviewRecipeId = null
        }
    }

    LaunchedEffect(videoExtractionState.completedRecipe?.id) {
        videoExtractionState.completedRecipe?.let { persisted ->
            editingRecipeId = persisted.id
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
                ) { Text("Import") }
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

    BackHandler(enabled = cookingRecipeId != null || editingRecipeId != null || viewingRecipeId != null || selectedTab != Tab.Home) {
        when {
            cookingRecipeId != null -> cookingRecipeId = null
            editingRecipeId != null -> editingRecipeId = null
            viewingRecipeId != null -> viewingRecipeId = null
            selectedTab != Tab.Home -> selectedTab = Tab.Home
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

    fun markCooked(recipe: Recipe) {
        if (recipe.cooked) {
            viewModel.update(recipe.id) { copy(cooked = false, cookedAt = null) }
        } else {
            val cookedDate = LocalDate.now().toString()
            viewModel.update(recipe.id) { copy(cooked = true, cookedAt = cookedDate) }
            captureRecipe = recipe.copy(cooked = true, cookedAt = cookedDate)
        }
    }

    fun persistCookPhoto(recipe: Recipe, copyPhoto: suspend () -> String?) {
        coroutineScope.launch {
            val savedPath = withContext(Dispatchers.IO) { copyPhoto() }
            if (savedPath == null) {
                cookPhotoError = "We couldn't save that photo. Try another image or take it again."
                return@launch
            }
            val oldPath = recipe.cookPhotoPath
            val cookedDate = recipe.cookedAt ?: LocalDate.now().toString()
            viewModel.update(recipe.id) {
                copy(cooked = true, cookedAt = cookedDate, cookPhotoPath = savedPath)
            }
            if (oldPath.isNotBlank() && oldPath != savedPath) {
                withContext(Dispatchers.IO) { CookPhotoStore.delete(oldPath) }
            }
            captureRecipe = null
        }
    }

    val chooseCookPhotoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        captureRecipe?.let { recipe ->
            if (uri != null) persistCookPhoto(recipe) { CookPhotoStore.copyFromUri(context, uri) }
        }
    }

    val takeCookPhotoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { captured ->
        val file = pendingCookPhotoFile
        pendingCookPhotoFile = null
        captureRecipe?.let { recipe ->
            if (captured && file != null) {
                persistCookPhoto(recipe) {
                    CookPhotoStore.copyFromFile(context, file).also { file.delete() }
                }
            } else {
                file?.delete()
            }
        }
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
            selectedTab = Tab.Home
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
                category = "Other",
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
                    error = "The shared recipe could not be saved. Try the import again."
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
                error = "Background extraction could not be scheduled. Try the import again."
            )
            pendingSharePayload = SharePayload.Text(sharedText)
            acceptedSharedText = null
        }
    }

    val displayedExtractionProgress = when {
        videoExtractionState.progress.isExtracting || videoExtractionState.progress.error != null ->
            videoExtractionState.progress
        instagramRetryError != null -> ExtractionProgress(error = instagramRetryError?.message)
        else -> extractionProgress
    }

    InstaRecipeTheme(themeMode = themeMode, motionMode = motionMode) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            when {
                currentEditingRecipe != null -> RecipeEditor(
                    recipe = currentEditingRecipe,
                    onCancel = { editingRecipeId = null },
                    onSave = { saved ->
                        viewModel.upsert(saved) { persisted ->
                            editingRecipeId = null
                            viewingRecipeId = persisted.id
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
                                val instagramProfile = GeminiRecipeExtractor.getInstagramProfileUrl(context).ifBlank { null }
                                val resolution = InstagramResolver.resolveAndDownload(
                                    context = context,
                                    instagramUrl = cleanUrl,
                                    customResolverUrl = customResolver,
                                    preferredProfileUrl = instagramProfile,
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

                currentViewingRecipe != null -> RecipeDetail(
                    recipe = currentViewingRecipe,
                    onBack = { viewingRecipeId = null },
                    onEdit = { editingRecipeId = currentViewingRecipe.id },
                    onDelete = {
                        viewModel.delete(currentViewingRecipe.id)
                        viewingRecipeId = null
                    },
                    onToggleFavorite = {
                        val updated = currentViewingRecipe.copy(favorite = !currentViewingRecipe.favorite)
                        viewModel.upsert(updated)
                    },
                    onToggleCooked = {
                        markCooked(currentViewingRecipe)
                    },
                    onStartCooking = { cookingRecipeId = currentViewingRecipe.id },
                    onRequestCookPhoto = { captureRecipe = currentViewingRecipe },
                    onRemoveCookPhoto = {
                        val oldPath = currentViewingRecipe.cookPhotoPath
                        viewModel.update(currentViewingRecipe.id) { copy(cookPhotoPath = "") }
                        coroutineScope.launch(Dispatchers.IO) { CookPhotoStore.delete(oldPath) }
                    },
                    onRetryInstagramImport = if (currentViewingRecipe.canExtractFromLinkAgain()) {
                        { viewModel.retryInstagramImport(currentViewingRecipe) }
                    } else null,
                    isRetryingInstagramImport = currentViewingRecipe.id in retryingInstagramImportIds,
                    instagramRetryError = instagramRetryError
                        ?.takeIf { it.recipeId == currentViewingRecipe.id }
                        ?.message,
                    onDismissInstagramRetryError = viewModel::dismissInstagramRetryError,
                    onTagSelected = { tag ->
                        libraryFilterId = "tag:${TagNormalizer.key(tag)}"
                        viewingRecipeId = null
                        selectedTab = Tab.Home
                    }
                )

                else -> mainStateHolder.SaveableStateProvider("main-scaffold") {
                    MainScaffold(
                    recipes = recipes,
                    selectedTab = selectedTab,
                    extractionProgress = displayedExtractionProgress,
                    onDismissError = {
                        if (videoExtractionState.progress.error != null) {
                            viewModel.dismissVideoExtractionError()
                        } else if (instagramRetryError != null) {
                            viewModel.dismissInstagramRetryError()
                        } else {
                            extractionProgress = extractionProgress.copy(error = null)
                        }
                    },
                    onTabSelected = { selectedTab = it },
                    libraryFilterId = libraryFilterId,
                    onLibraryFilterSelected = { libraryFilterId = it },
                    onTagSelected = { tag ->
                        libraryFilterId = "tag:${TagNormalizer.key(tag)}"
                        selectedTab = Tab.Home
                    },
                    onOpen = { viewingRecipeId = it.id },
                    onToggleFavorite = { recipe ->
                        viewModel.update(recipe.id) { copy(favorite = !favorite) }
                    },
                    onToggleCooked = { recipe ->
                        markCooked(recipe)
                    },
                    onStartCooking = { recipe -> cookingRecipeId = recipe.id },
                    onAdd = {
                        editingRecipeId = 0L
                    },
                    onImportVideo = { importVideoLauncher.launch("video/*") },
                    onRetryInstagramImport = viewModel::retryInstagramImport,
                    retryingInstagramImportIds = retryingInstagramImportIds,
                    themeMode = themeMode,
                    onThemeModeChange = { selectedMode ->
                        themeMode = selectedMode
                        ThemePreferences.save(context, selectedMode)
                    },
                    motionMode = motionMode,
                    onMotionModeChange = { selectedMode ->
                        motionMode = selectedMode
                        MotionPreferences.save(context, selectedMode)
                    }
                    )
                }
            }

            // Interactive Cooking Mode Overlay
            currentCookingRecipe?.let { cookingRecipe ->
                CookingModeDialog(
                    recipeTitle = cookingRecipe.title,
                    steps = cookingRecipe.steps,
                    ingredients = cookingRecipe.ingredients,
                    onClose = { cookingRecipeId = null },
                    onFinishAndMarkCooked = {
                        val cookedDate = LocalDate.now().toString()
                        viewModel.update(cookingRecipe.id) { copy(cooked = true, cookedAt = cookedDate) }
                        captureRecipe = cookingRecipe.copy(cooked = true, cookedAt = cookedDate)
                    }
                )
            }

            captureRecipe?.let { recipe ->
                AlertDialog(
                    onDismissRequest = { captureRecipe = null },
                    title = { Text("You made it.") },
                    text = {
                        Text(
                            "Capture your cook for this private recipe shelf. It stays separate from the original recipe imagery."
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                val file = CookPhotoStore.createCaptureFile(context)
                                pendingCookPhotoFile = file
                                takeCookPhotoLauncher.launch(CookPhotoStore.captureUri(context, file))
                            }
                        ) { Text("Take a photo") }
                    },
                    dismissButton = {
                        Row {
                            TextButton(onClick = { chooseCookPhotoLauncher.launch("image/*") }) {
                                Text("Choose photo")
                            }
                            TextButton(onClick = { captureRecipe = null }) { Text("Not now") }
                        }
                    }
                )
            }

            cookPhotoError?.let { message ->
                AlertDialog(
                    onDismissRequest = { cookPhotoError = null },
                    title = { Text("Photo not saved") },
                    text = { Text(message) },
                    confirmButton = { TextButton(onClick = { cookPhotoError = null }) { Text("OK") } }
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
        Row(
            modifier = Modifier.padding(24.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(width = 4.dp, height = 76.dp)
                    .background(MaterialTheme.colorScheme.primary)
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

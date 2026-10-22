package com.instarecipe.app.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.BackHandler
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.instarecipe.app.*
import com.instarecipe.app.ui.theme.AppRadii
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDate

private val PresetCategories = listOf(
    "Dinner",
    "Lunch",
    "Breakfast",
    "Quick recipes",
    "Snack",
    "Sauces & condiments",
    "Fresh & green",
    "Sweet things"
)

private val PresetSkillLevels = listOf("Easy", "Medium", "Project")

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun RecipeEditor(
    recipe: Recipe?,
    onCancel: () -> Unit,
    onSave: suspend (Recipe) -> Unit,
    onTriggerAiExtraction: (suspend (url: String, notes: String) -> ExtractedRecipeData)? = null
) {
    var title by rememberSaveable(recipe?.id) { mutableStateOf(recipe?.title.orEmpty()) }
    var sourceUrl by rememberSaveable(recipe?.id) { mutableStateOf(recipe?.sourceUrl.orEmpty()) }
    var creator by rememberSaveable(recipe?.id) { mutableStateOf(recipe?.creator.orEmpty()) }
    var category by rememberSaveable(recipe?.id) {
        mutableStateOf(recipe?.category?.takeUnless { it.equals("Saved to try", ignoreCase = true) }.orEmpty())
    }
    var dietType by rememberSaveable(recipe?.id) { mutableStateOf(recipe?.dietType ?: DietType.Unknown) }
    var tags by rememberSaveable(recipe?.id) { mutableStateOf(recipe?.tags.orEmpty()) }
    var tagDraft by rememberSaveable(recipe?.id) { mutableStateOf("") }
    var ingredients by rememberSaveable(recipe?.id) { mutableStateOf(recipe?.ingredients?.joinToString("\n").orEmpty()) }
    var steps by rememberSaveable(recipe?.id) { mutableStateOf(recipe?.steps?.joinToString("\n").orEmpty()) }
    var notes by rememberSaveable(recipe?.id) { mutableStateOf(recipe?.notes.orEmpty()) }
    var totalTimeMinutes by rememberSaveable(recipe?.id) { mutableStateOf(recipe?.totalTimeMinutes?.toString().orEmpty()) }
    var activeTimeMinutes by rememberSaveable(recipe?.id) { mutableStateOf(recipe?.activeTimeMinutes?.toString().orEmpty()) }
    var recipeYield by rememberSaveable(recipe?.id) { mutableStateOf(recipe?.yield.orEmpty()) }
    var skillLevel by rememberSaveable(recipe?.id) { mutableStateOf(recipe?.skillLevel.orEmpty()) }

    var isExtractingFromEditor by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var showExitConfirmation by rememberSaveable { mutableStateOf(false) }
    var detailsExpanded by rememberSaveable(recipe?.id) { mutableStateOf(TagNormalizer.matches(recipe?.tags.orEmpty(), PARTIAL_RECIPE_TAG)) }
    var importToolsExpanded by rememberSaveable(recipe?.id) {
        mutableStateOf(recipe != null && recipe.sourceUrl.isNotBlank() && recipe.ingredients.isEmpty() && recipe.steps.isEmpty())
    }
    var editorFeedbackMessage by remember { mutableStateOf<String?>(null) }
    var editorFeedbackIsError by remember { mutableStateOf(false) }
    var attemptedSave by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    LaunchedEffect(editorFeedbackMessage) {
        if (editorFeedbackMessage != null && editorFeedbackIsError) listState.animateScrollToItem(0)
    }
    var showPasteCaptionDialog by remember { mutableStateOf(false) }
    var captionDialogInput by remember { mutableStateOf("") }
    var structuredIngredientsMode by rememberSaveable { mutableStateOf(false) }
    var structuredStepsMode by rememberSaveable { mutableStateOf(false) }

    fun applyExtractedRecipe(extracted: ExtractedRecipeData) {
        title = extracted.title
        if (creator.isBlank() || creator.startsWith("http")) creator = extracted.creator
        category = extracted.category
        dietType = extracted.dietType
        tags = TagNormalizer.normalizeAll(extracted.tags)
        ingredients = extracted.ingredients.joinToString("\n")
        steps = extracted.steps.joinToString("\n")
        extracted.totalTimeMinutes?.let { totalTimeMinutes = it.toString() }
        if (extracted.notes.isNotBlank()) {
            notes = listOf(stripInstagramImportFailureStatus(notes).trim(), extracted.notes)
                .filter(String::isNotBlank).distinct().joinToString("\n\n")
        }
    }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    fun buildRecipe(status: RecipeStatus): Recipe = Recipe(
        id = recipe?.id?.takeIf { it != 0L } ?: 0L,
        title = title.trim(), sourceUrl = sourceUrl.trim(), creator = creator.trim(),
        category = category.trim().ifBlank { "Other" },
        tags = if (status == RecipeStatus.Saved) reviewedRecipeTags(tags + TagNormalizer.parse(tagDraft))
            else TagNormalizer.normalizeAll(tags + TagNormalizer.parse(tagDraft)),
        ingredients = ingredients.lines().map(String::trim).filter(String::isNotBlank),
        steps = steps.lines().map(String::trim).filter(String::isNotBlank),
        notes = if (status == RecipeStatus.Saved) stripInstagramImportFailureStatus(notes.trim()) else notes.trim(),
        favorite = recipe?.favorite ?: false, cooked = recipe?.cooked ?: false,
        status = status, savedDate = recipe?.savedDate ?: LocalDate.now().toString(),
        totalTimeMinutes = totalTimeMinutes.toIntOrNull()?.takeIf { it > 0 },
        activeTimeMinutes = activeTimeMinutes.toIntOrNull()?.takeIf { it > 0 },
        yield = recipeYield.trim(), skillLevel = skillLevel.trim(),
        cookPhotoPath = recipe?.cookPhotoPath.orEmpty(), cookedAt = recipe?.cookedAt, dietType = dietType
    )

    fun commitSave(status: RecipeStatus = RecipeStatus.Saved) {
        if (isSaving || isExtractingFromEditor) return
        val candidate = buildRecipe(status)
        val quality = assessRecipeQuality(candidate.title, candidate.ingredients, candidate.steps)
        if (status == RecipeStatus.Saved && !quality.isValid) {
            attemptedSave = true
            editorFeedbackIsError = true
            editorFeedbackMessage = "${quality.failure?.userMessage()} Complete the essentials, or save a draft to finish later."
            return
        }
        if (status == RecipeStatus.Draft && candidate.title.isBlank() && candidate.sourceUrl.isBlank() &&
            candidate.notes.isBlank() && candidate.ingredients.isEmpty() && candidate.steps.isEmpty()) {
            editorFeedbackIsError = true
            editorFeedbackMessage = "Add a title, source or recipe details before keeping a draft."
            return
        }
        isSaving = true
        coroutineScope.launch {
            try {
                onSave(candidate)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                editorFeedbackIsError = true
                editorFeedbackMessage = "Couldn't save this recipe. Your edits are still here. Try again."
            } finally {
                isSaving = false
            }
        }
    }

    val originalCategory = recipe?.category?.takeUnless { it.equals("Saved to try", true) }.orEmpty()
    val hasUnsavedChanges = title != recipe?.title.orEmpty() || sourceUrl != recipe?.sourceUrl.orEmpty() ||
        creator != recipe?.creator.orEmpty() || category != originalCategory || dietType != (recipe?.dietType ?: DietType.Unknown) ||
        tags != recipe?.tags.orEmpty() || tagDraft.isNotBlank() || notes != recipe?.notes.orEmpty() ||
        ingredients != recipe?.ingredients?.joinToString("\n").orEmpty() || steps != recipe?.steps?.joinToString("\n").orEmpty() ||
        totalTimeMinutes != recipe?.totalTimeMinutes?.toString().orEmpty() || activeTimeMinutes != recipe?.activeTimeMinutes?.toString().orEmpty() ||
        recipeYield != recipe?.yield.orEmpty() || skillLevel != recipe?.skillLevel.orEmpty()

    fun requestClose() {
        if (isSaving) return
        if (hasUnsavedChanges || isExtractingFromEditor) showExitConfirmation = true else onCancel()
    }
    BackHandler { requestClose() }
    if (showExitConfirmation) AlertDialog(
        onDismissRequest = { showExitConfirmation = false },
        title = { Text("Keep your changes?") },
        text = { Text("Keep the current details as a draft to finish later. Leaving stops any extraction running in this editor.") },
        confirmButton = { TextButton(onClick = { showExitConfirmation = false; commitSave(RecipeStatus.Draft) }, enabled = !isSaving && !isExtractingFromEditor) { Text("Keep draft") } },
        dismissButton = {
            Column {
                TextButton(onClick = { showExitConfirmation = false }) { Text("Keep editing") }
                TextButton(onClick = { showExitConfirmation = false; onCancel() }) { Text("Discard changes", color = MaterialTheme.colorScheme.error) }
            }
        }
    )

    val editorVideoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            if (GeminiRecipeExtractor.getApiKeys(context).isEmpty()) {
                editorFeedbackIsError = true
                editorFeedbackMessage = "Connect recipe imports in Settings first."
                return@rememberLauncherForActivityResult
            }
            coroutineScope.launch {
                isExtractingFromEditor = true
                editorFeedbackIsError = false
                editorFeedbackMessage = "Reading the video and creating your recipe…"
                var videoFile: File? = null
                try {
                    videoFile = VideoFileStore.copyToCache(context, uri)

                    val extracted = GeminiRecipeExtractor.extractRecipe(
                        context = context,
                        videoFile = videoFile,
                        textCaption = notes.takeIf { GeminiRecipeExtractor.hasSubstantiveRecipeContent(it) },
                        sourceUrl = sourceUrl.ifBlank { "Attached reel video" },
                        creatorName = creator.ifBlank { null }
                    )

                    applyExtractedRecipe(extracted)
                    editorFeedbackIsError = false
                    editorFeedbackMessage = "Your video is now a recipe."
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (e: Exception) {
                    editorFeedbackIsError = true
                    editorFeedbackMessage = "We couldn’t read that video: ${e.message}"
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
            title = { Text("Paste recipe text") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Paste the Reel caption or recipe description. InstaRecipe will organize the ingredients and steps.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = captionDialogInput,
                        onValueChange = { captionDialogInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 5,
                        placeholder = { Text("Paste the caption or recipe text here…") }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val textToExtract = captionDialogInput.trim()
                        showPasteCaptionDialog = false
                        if (textToExtract.isNotBlank()) {
                            if (GeminiRecipeExtractor.getApiKeys(context).isEmpty()) {
                                editorFeedbackIsError = true
                                editorFeedbackMessage = "Connect recipe imports in Settings first."
                                return@Button
                            }
                            coroutineScope.launch {
                                isExtractingFromEditor = true
                                editorFeedbackIsError = false
                                editorFeedbackMessage = "Creating a recipe from the caption…"
                                try {
                                    val extracted = GeminiRecipeExtractor.extractRecipe(
                                        context = context,
                                        videoFile = null,
                                        textCaption = textToExtract,
                                        sourceUrl = sourceUrl.ifBlank { "Pasted caption" },
                                        creatorName = creator.ifBlank { null }
                                    )
                                    applyExtractedRecipe(extracted)
                                    editorFeedbackIsError = false
                                    editorFeedbackMessage = "Your text is now a recipe."
                                } catch (cancelled: CancellationException) {
                                    throw cancelled
                                } catch (e: Exception) {
                                    editorFeedbackIsError = true
                                    editorFeedbackMessage = "We couldn’t read that text: ${e.message}"
                                } finally {
                                    isExtractingFromEditor = false
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Build recipe")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPasteCaptionDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    val needsSource = sourceUrl.isNotBlank() && ingredients.isBlank() && steps.isBlank()
    val importToolsContent: @Composable () -> Unit = {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(4.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Complete the source details", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                        Text(
                            "Add a video or paste the caption if the draft needs more recipe detail.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
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
                                Text("Video", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }

                            OutlinedButton(
                                onClick = {
                                    captionDialogInput = stripInstagramImportFailureStatus(notes)
                                    showPasteCaptionDialog = true
                                },
                                enabled = !isExtractingFromEditor,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.size(4.dp))
                                Text("Paste text", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }

                        if (onTriggerAiExtraction != null && sourceUrl.isNotBlank()) {
                            OutlinedButton(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = {
                                    isExtractingFromEditor = true
                                    editorFeedbackIsError = false
                                    editorFeedbackMessage = "Refreshing from Instagram…"
                                    coroutineScope.launch {
                                        try {
                                            val extracted = onTriggerAiExtraction(sourceUrl, notes)
                                            applyExtractedRecipe(extracted)
                                            editorFeedbackIsError = false
                                            editorFeedbackMessage = "Recipe details found. Review them before saving."
                                        } catch (cancelled: CancellationException) {
                                            throw cancelled
                                        } catch (error: Exception) {
                                            editorFeedbackIsError = true
                                            editorFeedbackMessage = error.message ?: "Extraction failed. Please try again."
                                        } finally {
                                            isExtractingFromEditor = false
                                        }
                                    }
                                },
                                enabled = !isExtractingFromEditor,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                if (isExtractingFromEditor) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.primary)
                                    Spacer(Modifier.size(8.dp))
                                    Text("Creating recipe…")
                                } else {
                                    Icon(Icons.Default.Refresh, contentDescription = null)
                                    Spacer(Modifier.size(6.dp))
                                    Text("Refresh from link", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(if (recipe?.status == RecipeStatus.Saved) "Edit recipe" else if (recipe?.id == 0L) "New recipe" else "Review import", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    TextButton(onClick = { requestClose() }, enabled = !isSaving) {
                        Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                actions = {
                    TextButton(onClick = { commitSave() }, enabled = !isSaving && !isExtractingFromEditor) {
                        Text("Save", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .imePadding()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    Button(
                        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(12.dp),
                        onClick = { commitSave() },
                        enabled = !isSaving && !isExtractingFromEditor
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null)
                        Spacer(Modifier.size(8.dp))
                        Text(if (isSaving) "Saving…" else "Save to recipes", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = { commitSave(RecipeStatus.Draft) },
                        enabled = !isSaving && !isExtractingFromEditor,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                    ) { Text("Save draft") }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
                .padding(padding)
                .imePadding(),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text("Check ingredients, quantities and cooking steps before saving.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (TagNormalizer.matches(tags, PARTIAL_RECIPE_TAG)) {
                    Text("Some source details are missing. Review the notes and complete the recipe, or keep a draft.", color = MaterialTheme.colorScheme.error)
                }
            }

            if (editorFeedbackMessage != null) {
                item {
                    val isErr = editorFeedbackIsError
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isErr) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant
                        ),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                editorFeedbackMessage!!,
                                color = if (isErr) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { editorFeedbackMessage = null }) {
                                Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = if (isErr) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }

            if (needsSource) item { importToolsContent() }

            // Section 1: Recipe Identity
            item {
                EditorSectionHeader("RECIPE", "Title")
                Spacer(Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Field("Title", title, isError = attemptedSave && title.isBlank()) { title = it }
                    if (detailsExpanded) Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        Field("Creator / Chef", creator) { creator = it }
                        Field("Instagram Link", sourceUrl) { sourceUrl = it }
                    }
                }
            }

            // Section 3: Ingredients (with Row-by-Row or Bulk Text toggle)
            item {
                val ingredientLines = remember(ingredients) {
                    ingredients.lines().let { list -> if (list.isEmpty()) listOf("") else list }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    EditorSectionHeader("INGREDIENTS", "${ingredientLines.count { it.isNotBlank() }} items")
                    TextButton(onClick = { structuredIngredientsMode = !structuredIngredientsMode }) {
                        Text(if (structuredIngredientsMode) "Bulk text mode" else "Row-by-row mode")
                    }
                }
                Spacer(Modifier.height(6.dp))
                if (structuredIngredientsMode) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        ingredientLines.forEachIndexed { idx, line ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                OutlinedTextField(
                                    value = line,
                                    onValueChange = { updatedLine ->
                                        val mutable = ingredientLines.toMutableList()
                                        mutable[idx] = updatedLine
                                        ingredients = mutable.joinToString("\n")
                                    },
                                    modifier = Modifier.weight(1f),
                                    placeholder = { Text("e.g. 2 tbsp olive oil") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                IconButton(
                                    onClick = {
                                        val mutable = ingredientLines.toMutableList()
                                        if (mutable.size > 1) {
                                            mutable.removeAt(idx)
                                        } else {
                                            mutable[0] = ""
                                        }
                                        ingredients = mutable.joinToString("\n")
                                    }
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Remove ingredient row", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                        OutlinedButton(
                            onClick = {
                                ingredients = if (ingredients.isBlank()) "\n" else "$ingredients\n"
                            },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Add ingredient row")
                        }
                    }
                } else {
                    Field("Ingredients (one per line)", ingredients, minLines = 5) { ingredients = it }
                }
            }

            // Section 4: Cooking Steps (with Row-by-Row or Bulk Text toggle)
            item {
                val stepLines = remember(steps) {
                    steps.lines().let { list -> if (list.isEmpty()) listOf("") else list }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    EditorSectionHeader("METHOD", "${stepLines.count { it.isNotBlank() }} steps")
                    TextButton(onClick = { structuredStepsMode = !structuredStepsMode }) {
                        Text(if (structuredStepsMode) "Bulk text mode" else "Step-by-step mode")
                    }
                }
                Spacer(Modifier.height(6.dp))
                if (structuredStepsMode) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        stepLines.forEachIndexed { idx, line ->
                            Row(
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .padding(top = 14.dp)
                                        .size(26.dp)
                                        .clip(RoundedCornerShape(AppRadii.surface))
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "${idx + 1}",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                                OutlinedTextField(
                                    value = line,
                                    onValueChange = { updatedLine ->
                                        val mutable = stepLines.toMutableList()
                                        mutable[idx] = updatedLine
                                        steps = mutable.joinToString("\n")
                                    },
                                    modifier = Modifier.weight(1f),
                                    placeholder = { Text("Describe step ${idx + 1}…") },
                                    minLines = 2,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                IconButton(
                                    onClick = {
                                        val mutable = stepLines.toMutableList()
                                        if (mutable.size > 1) {
                                            mutable.removeAt(idx)
                                        } else {
                                            mutable[0] = ""
                                        }
                                        steps = mutable.joinToString("\n")
                                    },
                                    modifier = Modifier.padding(top = 6.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Remove step row", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                        OutlinedButton(
                            onClick = {
                                steps = if (steps.isBlank()) "\n" else "$steps\n"
                            },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Add cooking step")
                        }
                    }
                } else {
                    Field("Cooking Steps (one per line)", steps, minLines = 5) { steps = it }
                }
            }

            item {
                TextButton(onClick = { detailsExpanded = !detailsExpanded }) {
                    Text(if (detailsExpanded) "Hide optional details" else "Category, timing, source & notes")
                }
            }
            if (detailsExpanded) {
            // Section 2: Chapter, Diet & Quick Facts (2x2 Grid)
            item {
                EditorSectionHeader("DETAILS", "Optional")
                Spacer(Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Category", style = MaterialTheme.typography.labelLarge)
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        PresetCategories.forEach { preset ->
                            FilterChip(
                                selected = category.equals(preset, ignoreCase = true),
                                onClick = { category = preset },
                                label = { Text(preset) }
                            )
                        }
                    }
                    Field("Category (or type custom)", category) { category = it }

                    Text("Diet", style = MaterialTheme.typography.labelLarge)
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            DietType.Unknown to "Not set",
                            DietType.Vegetarian to "Veg",
                            DietType.NonVegetarian to "Non-veg",
                            DietType.Vegan to "Vegan"
                        ).forEach { (value, label) ->
                            InputChip(
                                selected = dietType == value,
                                onClick = { dietType = value },
                                label = { Text(label) },
                                modifier = Modifier.heightIn(min = 48.dp)
                            )
                        }
                    }

                    // 2x2 Compact Metadata Grid
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = totalTimeMinutes,
                            onValueChange = { totalTimeMinutes = it.filter(Char::isDigit) },
                            modifier = Modifier.weight(1f),
                            label = { Text("Total time (min)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                        OutlinedTextField(
                            value = activeTimeMinutes,
                            onValueChange = { activeTimeMinutes = it.filter(Char::isDigit) },
                            modifier = Modifier.weight(1f),
                            label = { Text("Active time (min)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = recipeYield,
                            onValueChange = { recipeYield = it },
                            modifier = Modifier.weight(1f),
                            label = { Text("Yield / servings") },
                            placeholder = { Text("e.g. 4 servings") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                        OutlinedTextField(
                            value = skillLevel,
                            onValueChange = { skillLevel = it },
                            modifier = Modifier.weight(1f),
                            label = { Text("Skill level") },
                            placeholder = { Text("Easy, Medium…") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PresetSkillLevels.forEach { level ->
                            FilterChip(
                                selected = skillLevel.equals(level, ignoreCase = true),
                                onClick = { skillLevel = if (skillLevel.equals(level, ignoreCase = true)) "" else level },
                                label = { Text(level) }
                            )
                        }
                    }
                    TagEditor(
                        tags = tags,
                        draft = tagDraft,
                        onDraftChange = { tagDraft = it },
                        onTagsChange = { tags = it }
                    )
                }
            }

            // Section 5: Notes
            item {
                EditorSectionHeader("COOK'S NOTES", "Tips, Substitutions & Storage")
                Spacer(Modifier.height(8.dp))
                Field("Notes, Tips, & Prep Times", notes, minLines = 4) { notes = it }
            }
            }
            if (!needsSource) item {
                TextButton(onClick = { importToolsExpanded = !importToolsExpanded }) {
                    Text(if (importToolsExpanded) "Hide import tools" else "Add video, recipe text or refresh link")
                }
            }
            if (importToolsExpanded && !needsSource) {
                item { importToolsContent() }
            }

        }
    }
}

@Composable
private fun EditorSectionHeader(eyebrow: String, subtitle: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
        Spacer(Modifier.height(6.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                eyebrow,
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Text(
                "·  $subtitle",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun Field(label: String, value: String, minLines: Int = 1, isError: Boolean = false, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        minLines = minLines,
        isError = isError,
        shape = RoundedCornerShape(12.dp)
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TagEditor(
    tags: List<String>,
    draft: String,
    onDraftChange: (String) -> Unit,
    onTagsChange: (List<String>) -> Unit
) {
    fun addDraft() {
        val additions = TagNormalizer.parse(draft)
        if (additions.isNotEmpty()) {
            onTagsChange(TagNormalizer.normalizeAll(tags + additions))
            onDraftChange("")
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = draft,
            onValueChange = { value ->
                if (value.contains(',') || value.contains('\n')) {
                    val segments = value.split(',', '\n')
                    onTagsChange(TagNormalizer.normalizeAll(tags + segments.dropLast(1)))
                    onDraftChange(segments.last())
                } else {
                    onDraftChange(value)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Add tags") },
            placeholder = { Text("e.g. Vegetarian, Quick") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { addDraft() }),
            trailingIcon = {
                if (draft.isNotBlank()) {
                    TextButton(onClick = { addDraft() }) { Text("Add") }
                }
            },
            shape = RoundedCornerShape(12.dp)
        )
        if (tags.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                tags.forEach { tag ->
                    InputChip(
                        selected = false,
                        onClick = { onTagsChange(tags.filterNot { TagNormalizer.key(it) == TagNormalizer.key(tag) }) },
                        label = { Text(tag) },
                        trailingIcon = { Icon(Icons.Default.Close, contentDescription = "Remove $tag", modifier = Modifier.size(16.dp)) }
                    )
                }
            }
        }
        Text("Press comma or Done to add a tag. Tap a tag to remove it.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

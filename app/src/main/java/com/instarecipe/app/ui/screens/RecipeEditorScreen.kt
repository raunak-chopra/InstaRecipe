package com.instarecipe.app.ui.screens

import android.content.Context
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDate
import com.instarecipe.app.*

private enum class ExtractedNotesPolicy { Replace, Append }

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun RecipeEditor(
    recipe: Recipe?,
    onCancel: () -> Unit,
    onSave: (Recipe) -> Unit,
    onTriggerAiExtraction: ((url: String, notes: String, onResult: (Result<ExtractedRecipeData>) -> Unit) -> Unit)? = null
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
    var editorFeedbackMessage by remember { mutableStateOf<String?>(null) }
    var showPasteCaptionDialog by remember { mutableStateOf(false) }
    var captionDialogInput by remember { mutableStateOf("") }

    fun applyExtractedRecipe(extracted: ExtractedRecipeData, notesPolicy: ExtractedNotesPolicy) {
        title = extracted.title
        if (creator.isBlank() || creator.startsWith("http")) creator = extracted.creator
        category = extracted.category
        dietType = extracted.dietType
        tags = TagNormalizer.normalizeAll(extracted.tags)
        ingredients = extracted.ingredients.joinToString("\n")
        steps = extracted.steps.joinToString("\n")
        extracted.totalTimeMinutes?.let { totalTimeMinutes = it.toString() }
        if (extracted.notes.isNotBlank()) {
            notes = when (notesPolicy) {
                ExtractedNotesPolicy.Replace -> extracted.notes
                ExtractedNotesPolicy.Append -> if (notes.isNotBlank()) "$notes\n\n${extracted.notes}" else extracted.notes
            }
        }
    }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val editorVideoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            if (GeminiRecipeExtractor.getApiKeys(context).isEmpty()) {
                editorFeedbackMessage = "Connect recipe import in You first."
                return@rememberLauncherForActivityResult
            }
            coroutineScope.launch {
                isExtractingFromEditor = true
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

                    applyExtractedRecipe(extracted, ExtractedNotesPolicy.Replace)
                    editorFeedbackMessage = "Your video is now a recipe."
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (e: Exception) {
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
                                editorFeedbackMessage = "Connect recipe import in You first."
                                return@Button
                            }
                            coroutineScope.launch {
                                isExtractingFromEditor = true
                                editorFeedbackMessage = "Creating a recipe from the caption…"
                                try {
                                    val extracted = GeminiRecipeExtractor.extractRecipe(
                                        context = context,
                                        videoFile = null,
                                        textCaption = textToExtract,
                                        sourceUrl = sourceUrl.ifBlank { "Pasted caption" },
                                        creatorName = creator.ifBlank { null }
                                    )
                                    applyExtractedRecipe(extracted, ExtractedNotesPolicy.Append)
                                    editorFeedbackMessage = "Your text is now a recipe."
                                } catch (cancelled: CancellationException) {
                                    throw cancelled
                                } catch (e: Exception) {
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

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Review Recipe", fontWeight = FontWeight.Bold) },
                navigationIcon = { TextButton(onClick = onCancel) { Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
                .padding(padding)
                .imePadding(),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        "MAKE IT YOURS",
                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text("A recipe, before it joins your shelf.", style = MaterialTheme.typography.headlineMedium)
                    Text(
                        "Check the essentials first. Everything else can be refined when you have the recipe in front of you.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            if (editorFeedbackMessage != null) {
                item {
                    val isErr = editorFeedbackMessage!!.contains("failed", ignoreCase = true) ||
                        editorFeedbackMessage!!.contains("error", ignoreCase = true) ||
                        editorFeedbackMessage!!.contains("couldn't", ignoreCase = true) ||
                        editorFeedbackMessage!!.contains("could not", ignoreCase = true) ||
                        editorFeedbackMessage!!.contains("try again", ignoreCase = true) ||
                        editorFeedbackMessage!!.contains("no usable", ignoreCase = true) ||
                        editorFeedbackMessage!!.contains("required", ignoreCase = true) ||
                        editorFeedbackMessage!!.contains("placeholder", ignoreCase = true)
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isErr) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant
                        ),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                editorFeedbackMessage!!,
                                color = if (isErr) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
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

            // A quiet recovery area rather than a generic stack of equal-weight utility buttons.
            item {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(4.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Need another pass?", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
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
                                    captionDialogInput = notes
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
                                    editorFeedbackMessage = "Refreshing from Instagram…"
                                    onTriggerAiExtraction(sourceUrl, notes) { result ->
                                        isExtractingFromEditor = false
                                        result.onSuccess { extracted ->
                                            applyExtractedRecipe(extracted, ExtractedNotesPolicy.Replace)
                                            editorFeedbackMessage = "Recipe details found. Review them before saving."
                                        }.onFailure { error ->
                                            editorFeedbackMessage = error.message ?: "Extraction failed. Please try again."
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

            item { Field("Title", title) { title = it } }
            item { Field("Instagram Link", sourceUrl) { sourceUrl = it } }
            item { Field("Creator / Chef", creator) { creator = it } }
            item { Field("Category (optional)", category) { category = it } }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                }
            }
            item { Field("Total time in minutes (when supplied)", totalTimeMinutes) { totalTimeMinutes = it.filter(Char::isDigit) } }
            item { Field("Active time in minutes (when supplied)", activeTimeMinutes) { activeTimeMinutes = it.filter(Char::isDigit) } }
            item { Field("Yield / servings (when supplied)", recipeYield) { recipeYield = it } }
            item { Field("Skill level (when supplied)", skillLevel) { skillLevel = it } }
            item {
                TagEditor(
                    tags = tags,
                    draft = tagDraft,
                    onDraftChange = { tagDraft = it },
                    onTagsChange = { tags = it }
                )
            }
            item { Field("Ingredients (one per line)", ingredients, minLines = 5) { ingredients = it } }
            item { Field("Cooking Steps (one per line)", steps, minLines = 5) { steps = it } }
            item { Field("Notes, Tips, & Prep Times", notes, minLines = 4) { notes = it } }

            item {
                Button(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(12.dp),
                    onClick = {
                        val cleanTitle = title.trim()
                        val cleanIngredients = ingredients.lines().map(String::trim).filter(String::isNotBlank)
                        val cleanSteps = steps.lines().map(String::trim).filter(String::isNotBlank)
                        val qualityFailure = assessRecipeQuality(cleanTitle, cleanIngredients, cleanSteps).failure
                        if (qualityFailure != null) {
                            editorFeedbackMessage = "${qualityFailure.userMessage()} Add the missing details before saving."
                            return@Button
                        }
                        onSave(
                            Recipe(
                                id = recipe?.id?.takeIf { it != 0L } ?: 0L,
                                title = cleanTitle,
                                sourceUrl = sourceUrl.trim(),
                                creator = creator.trim(),
                                category = category.trim().ifBlank { "Other" },
                                tags = TagNormalizer.normalizeAll(tags + TagNormalizer.parse(tagDraft)),
                                ingredients = cleanIngredients,
                                steps = cleanSteps,
                                notes = notes.trim(),
                                favorite = recipe?.favorite ?: false,
                                cooked = recipe?.cooked ?: false,
                                status = RecipeStatus.Saved,
                                savedDate = recipe?.savedDate ?: LocalDate.now().toString(),
                                totalTimeMinutes = totalTimeMinutes.toIntOrNull()?.takeIf { it > 0 },
                                activeTimeMinutes = activeTimeMinutes.toIntOrNull()?.takeIf { it > 0 },
                                yield = recipeYield.trim(),
                                skillLevel = skillLevel.trim(),
                                cookPhotoPath = recipe?.cookPhotoPath.orEmpty(),
                                cookedAt = recipe?.cookedAt,
                                dietType = dietType
                            )
                        )
                    }
                ) {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text("Save recipe", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
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

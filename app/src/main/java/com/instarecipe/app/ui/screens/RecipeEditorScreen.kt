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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
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
    var category by rememberSaveable(recipe?.id) { mutableStateOf(recipe?.category ?: "Saved to try") }
    var tags by rememberSaveable(recipe?.id) { mutableStateOf(recipe?.tags.orEmpty()) }
    var tagDraft by rememberSaveable(recipe?.id) { mutableStateOf("") }
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
            if (GeminiRecipeExtractor.getApiKeys(context).isEmpty()) {
                editorFeedbackMessage = "Please add a primary or backup Gemini API key in Settings first."
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

                    title = extracted.title
                    if (creator.isBlank() || creator.startsWith("http")) creator = extracted.creator
                    category = extracted.category
                    tags = TagNormalizer.normalizeAll(extracted.tags)
                    ingredients = extracted.ingredients.joinToString("\n")
                    steps = extracted.steps.joinToString("\n")
                    if (extracted.notes.isNotBlank()) notes = extracted.notes
                    editorFeedbackMessage = "Recipe extracted from video successfully!"
                } catch (cancelled: CancellationException) {
                    throw cancelled
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
                        "Paste the Reel caption or recipe description. InstaRecipe will organize the ingredients and steps.",
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
                            if (GeminiRecipeExtractor.getApiKeys(context).isEmpty()) {
                                editorFeedbackMessage = "Please add a primary or backup Gemini API key in Settings first."
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
                                    title = extracted.title
                                    if (creator.isBlank() || creator.startsWith("http")) creator = extracted.creator
                                    category = extracted.category
                                    tags = TagNormalizer.normalizeAll(extracted.tags)
                                    ingredients = extracted.ingredients.joinToString("\n")
                                    steps = extracted.steps.joinToString("\n")
                                    if (extracted.notes.isNotBlank()) {
                                        notes = if (notes.isNotBlank()) "$notes\n\n${extracted.notes}" else extracted.notes
                                    }
                                    editorFeedbackMessage = "Recipe extracted from caption successfully!"
                                } catch (cancelled: CancellationException) {
                                    throw cancelled
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
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
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
                        Text("Import tools", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
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
                                    onTriggerAiExtraction(sourceUrl, notes) { result ->
                                        isExtractingFromEditor = false
                                        result.onSuccess { extracted ->
                                            title = extracted.title
                                            if (creator.isBlank() || creator.startsWith("http")) creator = extracted.creator
                                            category = extracted.category
                                            tags = TagNormalizer.normalizeAll(extracted.tags)
                                            ingredients = extracted.ingredients.joinToString("\n")
                                            steps = extracted.steps.joinToString("\n")
                                            if (extracted.notes.isNotBlank()) notes = extracted.notes
                                            editorFeedbackMessage = "Extracted successfully!"
                                        }.onFailure { error ->
                                            editorFeedbackMessage = error.message ?: "Extraction failed. Please try again."
                                        }
                                    }
                                },
                                enabled = !isExtractingFromEditor,
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                if (isExtractingFromEditor) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                                    Spacer(Modifier.size(8.dp))
                                    Text("Creating recipe…")
                                } else {
                                    Icon(Icons.Default.Refresh, contentDescription = null)
                                    Spacer(Modifier.size(6.dp))
                                    Text("Import from link again", fontWeight = FontWeight.Bold)
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
                    shape = RoundedCornerShape(16.dp),
                    onClick = {
                        onSave(
                            Recipe(
                                id = recipe?.id?.takeIf { it != 0L } ?: 0L,
                                title = title.ifBlank { "Untitled Recipe" },
                                sourceUrl = sourceUrl.trim(),
                                creator = creator.trim(),
                                category = category.ifBlank { "Saved to try" },
                                tags = TagNormalizer.normalizeAll(tags + TagNormalizer.parse(tagDraft)),
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

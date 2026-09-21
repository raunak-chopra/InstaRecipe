package com.instarecipe.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.instarecipe.app.Recipe
import com.instarecipe.app.displayCategory
import com.instarecipe.app.displayTitle
import com.instarecipe.app.InstagramResolver
import com.instarecipe.app.ui.components.PersonalCookPhoto
import com.instarecipe.app.ui.components.ServingScaler
import com.instarecipe.app.ui.components.ServingScalerSelector
import com.instarecipe.app.ui.theme.AppRadii
import com.instarecipe.app.ui.theme.BorderSoft
import com.instarecipe.app.ui.theme.FeedbackSuccess
import com.instarecipe.app.ui.theme.LocalRecipeTypeScale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun RecipeDetail(
    recipe: Recipe,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleFavorite: () -> Unit,
    onToggleCooked: () -> Unit,
    onStartCooking: () -> Unit,
    onRequestCookPhoto: () -> Unit,
    onRemoveCookPhoto: () -> Unit,
    onTagSelected: (String) -> Unit,
    onRetryInstagramImport: (() -> Unit)? = null,
    isRetryingInstagramImport: Boolean = false,
    instagramRetryError: String? = null,
    onDismissInstagramRetryError: () -> Unit = {}
) {
    val context = LocalContext.current
    var showDeleteConfirmation by rememberSaveable { mutableStateOf(false) }
    var servingScale by rememberSaveable { mutableFloatStateOf(1.0f) }
    val checkedIngredients = remember { mutableStateListOf<String>() }
    val trustedSourceUrl = remember(recipe.sourceUrl) {
        InstagramResolver.extractInstagramUrl(recipe.sourceUrl)
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWideLayout = maxWidth >= 760.dp
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Recipe", style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showDeleteConfirmation = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete recipe", tint = MaterialTheme.colorScheme.error)
                    }
                    TextButton(onClick = onEdit) { Text("Edit") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().widthIn(max = 1200.dp).align(Alignment.TopCenter).padding(padding),
            contentPadding = PaddingValues(start = 20.dp, top = 8.dp, end = 20.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (instagramRetryError != null) {
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite },
                        shape = RoundedCornerShape(AppRadii.surface)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                instagramRetryError,
                                modifier = Modifier.weight(1f),
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodySmall
                            )
                            TextButton(onClick = onDismissInstagramRetryError) { Text("Dismiss") }
                        }
                    }
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (recipe.cookPhotoPath.isNotBlank()) {
                        PersonalCookPhoto(
                            path = recipe.cookPhotoPath,
                            contentDescription = "Your photo of ${recipe.title}",
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 190.dp, max = 280.dp)
                                .clip(RoundedCornerShape(AppRadii.surface))
                        )
                    } else {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(AppRadii.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                "FROM YOUR RECIPE SHELF",
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                    Text(
                        recipe.displayCategory(),
                        style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 0.9.sp),
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        recipe.displayTitle(),
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (recipe.creator.isNotBlank()) {
                        Text("By ${recipe.creator}", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                    }
                    if (recipe.tags.isNotEmpty()) {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            recipe.tags.take(4).forEach { tag ->
                                AssistChip(
                                    onClick = { onTagSelected(tag) },
                                    label = { Text(tag) },
                                    border = BorderStroke(1.dp, BorderSoft),
                                    shape = RoundedCornerShape(AppRadii.surface)
                                )
                            }
                        }
                    }
                    DetailDecisionStrip(recipe)
                }
            }

            if (onRetryInstagramImport != null) {
                item {
                    OutlinedButton(
                        onClick = onRetryInstagramImport,
                        enabled = !isRetryingInstagramImport,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .semantics {
                                stateDescription = if (isRetryingInstagramImport) {
                                    "Extracting from link"
                                } else {
                                    "Ready to extract from link again"
                                }
                                liveRegion = LiveRegionMode.Polite
                            },
                        shape = RoundedCornerShape(AppRadii.control)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(Modifier.size(8.dp))
                        Text(if (isRetryingInstagramImport) "Extracting from link…" else "Extract from link again")
                    }
                }
            }

            item {
                Button(
                    onClick = onStartCooking,
                    enabled = recipe.steps.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(AppRadii.control)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.size(8.dp))
                    Text("Start cooking", style = MaterialTheme.typography.titleMedium)
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledTonalButton(
                        onClick = onToggleFavorite,
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                        shape = RoundedCornerShape(AppRadii.control)
                    ) {
                        Icon(
                            if (recipe.favorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = null,
                            tint = if (recipe.favorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.size(6.dp))
                        Text(if (recipe.favorite) "Saved" else "Save")
                    }
                    FilledTonalButton(
                        onClick = onToggleCooked,
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                        shape = RoundedCornerShape(AppRadii.control)
                    ) {
                        Icon(Icons.Default.Restaurant, contentDescription = null)
                        Spacer(Modifier.size(6.dp))
                        Text(if (recipe.cooked) "Cooked" else "Mark cooked")
                    }
                }
            }

            item {
                if (recipe.cookPhotoPath.isBlank()) {
                    OutlinedButton(
                        onClick = onRequestCookPhoto,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                        shape = RoundedCornerShape(AppRadii.control)
                    ) {
                        Text("Add your cook photo")
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onRequestCookPhoto,
                            modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                            shape = RoundedCornerShape(AppRadii.control)
                        ) { Text("Replace photo") }
                        TextButton(
                            onClick = onRemoveCookPhoto,
                            modifier = Modifier.heightIn(min = 48.dp)
                        ) { Text("Remove") }
                    }
                }
            }

            if (trustedSourceUrl != null) {
                item {
                    OutlinedButton(
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                        onClick = {
                            runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(trustedSourceUrl))) }
                        },
                        shape = RoundedCornerShape(AppRadii.control),
                        colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null)
                        Spacer(Modifier.size(8.dp))
                        Text("Watch original Reel")
                    }
                }
            }

            if (isWideLayout) {
                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(18.dp), verticalAlignment = Alignment.Top) {
                        RecipeIngredientsPanel(recipe, servingScale, { servingScale = it }, checkedIngredients, Modifier.weight(0.9f))
                        RecipeMethodPanel(recipe, Modifier.weight(1.1f))
                    }
                }
            } else {
                item { RecipeIngredientsPanel(recipe, servingScale, { servingScale = it }, checkedIngredients) }
                item { RecipeMethodPanel(recipe) }
            }

            if (recipe.notes.isNotBlank()) {
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        shape = RoundedCornerShape(AppRadii.surface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Default.Lightbulb, contentDescription = null, tint = MaterialTheme.colorScheme.onTertiaryContainer, modifier = Modifier.size(18.dp))
                                Text("Cook's note", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onTertiaryContainer)
                            }
                            Text(recipe.notes, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onTertiaryContainer)
                        }
                    }
                }
            }
        }
    }
    }

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Delete recipe?") },
            text = { Text("${recipe.title} will be permanently removed from this device.") },
            confirmButton = { TextButton(onClick = onDelete) { Text("Delete", color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton(onClick = { showDeleteConfirmation = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun RecipeIngredientsPanel(
    recipe: Recipe,
    servingScale: Float,
    onServingScaleChange: (Float) -> Unit,
    checkedIngredients: MutableList<String>,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(AppRadii.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Ingredients", style = LocalRecipeTypeScale.current.sectionTitle)
                ServingScalerSelector(currentScale = servingScale, onScaleSelected = onServingScaleChange)
            }
            if (recipe.ingredients.isEmpty()) {
                Text("No ingredients listed yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                recipe.ingredients.forEachIndexed { index, rawIngredient ->
                    val scaledIngredient = ServingScaler.scaleIngredient(rawIngredient, servingScale)
                    val key = index.toString()
                    val isChecked = checkedIngredients.contains(key)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .toggleable(
                                value = isChecked,
                                role = Role.Checkbox,
                                onValueChange = {
                                    if (isChecked) checkedIngredients.remove(key) else checkedIngredients.add(key)
                                }
                            )
                    ) {
                        Checkbox(
                            checked = isChecked,
                            onCheckedChange = null,
                            colors = CheckboxDefaults.colors(
                                checkedColor = MaterialTheme.colorScheme.primary,
                                checkmarkColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                        Spacer(Modifier.size(6.dp))
                        Text(
                            scaledIngredient,
                            style = LocalRecipeTypeScale.current.ingredient,
                            color = if (isChecked) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                            textDecoration = if (isChecked) TextDecoration.LineThrough else null
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RecipeMethodPanel(recipe: Recipe, modifier: Modifier = Modifier) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(AppRadii.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Method", style = LocalRecipeTypeScale.current.sectionTitle)
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
                                .clip(RoundedCornerShape(AppRadii.surface))
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "${index + 1}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Text(
                            step,
                            style = LocalRecipeTypeScale.current.body,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailDecisionStrip(recipe: Recipe) {
    val facts = buildList {
        recipe.totalTimeMinutes?.let { add("Total time" to "$it min") }
        recipe.activeTimeMinutes?.let { add("Active time" to "$it min") }
        recipe.yield.takeIf(String::isNotBlank)?.let { add("Yield" to it) }
        recipe.skillLevel.takeIf(String::isNotBlank)?.let { add("Skill" to it) }
        if (isEmpty()) {
            add("Ingredients" to recipe.ingredients.size.toString())
            add("Steps" to recipe.steps.size.toString())
        }
        add("Status" to if (recipe.cooked) "Cooked" else "To cook")
    }.take(4)
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(AppRadii.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            facts.forEach { (label, value) -> DecisionStat(label, value) }
        }
    }
}

@Composable
private fun DecisionStat(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(value, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

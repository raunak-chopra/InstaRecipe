package com.instarecipe.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Check
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
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.instarecipe.app.DietType
import com.instarecipe.app.InstagramResolver
import com.instarecipe.app.Recipe
import com.instarecipe.app.displayCategory
import com.instarecipe.app.displayLabel
import com.instarecipe.app.displayTitle
import com.instarecipe.app.effectiveDietType
import com.instarecipe.app.isCookbookReady
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import kotlinx.coroutines.launch
import com.instarecipe.app.ui.components.CookbookArtwork
import com.instarecipe.app.ui.components.PersonalCookPhoto
import com.instarecipe.app.ui.components.ServingScaler
import com.instarecipe.app.ui.components.ServingScalerSelector
import com.instarecipe.app.ui.theme.AccentLeaf
import com.instarecipe.app.ui.theme.AccentTomatoDeep
import com.instarecipe.app.ui.theme.AppMotion
import com.instarecipe.app.ui.theme.AppRadii
import com.instarecipe.app.ui.theme.BorderSoft
import com.instarecipe.app.ui.theme.FeedbackSuccess
import com.instarecipe.app.ui.theme.LocalMotionMode
import com.instarecipe.app.ui.theme.LocalRecipeTypeScale
import com.instarecipe.app.ui.theme.LocalReducedMotion
import com.instarecipe.app.ui.theme.MotionMode
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun RecipeDetail(
    recipe: Recipe,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleFavorite: () -> Unit,
    onToggleCooked: () -> Unit,
    onStartCooking: (servingScale: Float, checkedIngredientsCsv: String) -> Unit,
    onRequestCookPhoto: () -> Unit,
    onRemoveCookPhoto: () -> Unit,
    onTagSelected: (String) -> Unit,
    onRetryInstagramImport: (() -> Unit)? = null,
    isRetryingInstagramImport: Boolean = false,
    instagramRetryError: String? = null,
    onDismissInstagramRetryError: () -> Unit = {},
    hasCookingSession: Boolean = false
) {
    val context = LocalContext.current
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val latestToggleCooked by rememberUpdatedState(onToggleCooked)
    var showDeleteConfirmation by rememberSaveable { mutableStateOf(false) }
    var showActions by remember { mutableStateOf(false) }
    var servingScale by rememberSaveable(recipe.id) { mutableFloatStateOf(1.0f) }
    var checkedIngredientsCsv by rememberSaveable(recipe.id) { mutableStateOf("") }
    val checkedSet = remember(checkedIngredientsCsv) {
        checkedIngredientsCsv.split(",").filter(String::isNotBlank).toSet()
    }
    fun toggleIngredient(key: String) {
        val updated = if (key in checkedSet) checkedSet - key else checkedSet + key
        checkedIngredientsCsv = updated.joinToString(",")
    }

    val trustedSourceUrl = remember(recipe.sourceUrl) {
        InstagramResolver.extractInstagramUrl(recipe.sourceUrl)
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWideLayout = maxWidth >= 760.dp
        Scaffold(
            snackbarHost = { SnackbarHost(snackbar) },
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("RECIPE JOURNAL", style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.1.sp), color = MaterialTheme.colorScheme.primary)
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        TextButton(onClick = onEdit) { Text("Edit", fontWeight = FontWeight.SemiBold) }
                        Box {
                            IconButton(onClick = { showActions = true }) { Icon(Icons.Default.MoreVert, contentDescription = "More recipe actions") }
                            DropdownMenu(expanded = showActions, onDismissRequest = { showActions = false }) {
                                DropdownMenuItem(text = { Text("Delete recipe") }, onClick = { showActions = false; showDeleteConfirmation = true })
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
                )
            },
            bottomBar = {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(0.85f)) {
                            val checkedCount = checkedSet.size
                            val totalIngredients = recipe.ingredients.size
                            Text(
                                if (checkedCount > 0 && totalIngredients > 0) {
                                    "$checkedCount of $totalIngredients prepped"
                                } else if (recipe.cooked) {
                                    "Cooked & kept"
                                } else {
                                    if (recipe.isCookbookReady()) "Ready to cook" else "Complete this recipe first"
                                },
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = if (checkedCount > 0) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                if (servingScale != 1f) "Scaled to ${servingScale.toInt()}x servings"
                                else "${recipe.steps.size} cooking steps",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Button(
                            onClick = { onStartCooking(servingScale, checkedIngredientsCsv) },
                            enabled = recipe.isCookbookReady(),
                            modifier = Modifier.weight(1.15f).heightIn(min = 50.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(AppRadii.control)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.size(6.dp))
                            Text(if (hasCookingSession) "Resume cooking" else "Start cooking", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        ) { padding ->
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 1200.dp)
                    .align(Alignment.TopCenter)
                    .padding(padding),
                contentPadding = PaddingValues(start = 20.dp, top = 8.dp, end = 20.dp, bottom = 36.dp),
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

                // Editorial Header & Visual Identity
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (recipe.cookPhotoPath.isNotBlank()) {
                            Box {
                                PersonalCookPhoto(
                                    path = recipe.cookPhotoPath,
                                    contentDescription = "Your photo of ${recipe.title}",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                    .heightIn(min = 112.dp, max = 148.dp)
                                        .clip(RoundedCornerShape(AppRadii.surface))
                                )
                                Surface(
                                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                                    shape = RoundedCornerShape(AppRadii.hairline),
                                    modifier = Modifier.align(Alignment.BottomStart).padding(12.dp)
                                ) {
                                    Text(
                                        "YOUR COOK PHOTO${recipe.cookedAt?.let { " · $it" }.orEmpty()}",
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.8.sp),
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val diet = recipe.effectiveDietType()
                            if (diet != DietType.Unknown) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(if (diet == DietType.NonVegetarian) AccentTomatoDeep else AccentLeaf)
                                )
                            }
                            Text(
                                text = buildString {
                                    append(recipe.displayCategory().uppercase())
                                    if (diet != DietType.Unknown) {
                                        append(" · ")
                                        append(diet.displayLabel().uppercase())
                                    }
                                },
                                style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 0.9.sp),
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Text(
                            recipe.displayTitle(),
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (recipe.creator.isNotBlank()) {
                            Text(
                                "By ${recipe.creator}",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        if (recipe.tags.isNotEmpty()) {
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                recipe.tags.take(6).forEach { tag ->
                                    AssistChip(
                                        onClick = { onTagSelected(tag) },
                                        label = { Text(tag) },
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                        shape = RoundedCornerShape(AppRadii.surface)
                                    )
                                }
                            }
                        }
                        DetailDecisionStrip(recipe)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("Ingredients", "Method", "Notes").forEachIndexed { index, label ->
                                if (index != 2 || recipe.notes.isNotBlank()) TextButton(onClick = {
                                    val first = 1 + (if (instagramRetryError != null) 1 else 0) + (if (onRetryInstagramImport != null) 1 else 0)
                                    val target = when (index) {
                                        0 -> first
                                        1 -> first + if (isWideLayout) 0 else 1
                                        else -> first + if (isWideLayout) 1 else 2
                                    }
                                    scope.launch { listState.scrollToItem(target) }
                                }, modifier = Modifier.heightIn(min = 48.dp)) { Text(label) }
                            }
                        }

                        // Compact Editorial Action Strip (replaces the 5 stacked full-width buttons)
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            FilterChip(
                                selected = recipe.favorite,
                                onClick = onToggleFavorite,
                                label = { Text(if (recipe.favorite) "Favorited" else "Favorite") },
                                leadingIcon = {
                                    Icon(
                                        if (recipe.favorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = if (recipe.favorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                },
                                shape = RoundedCornerShape(AppRadii.control),
                                modifier = Modifier.heightIn(min = 48.dp)
                            )

                            FilterChip(
                                selected = recipe.cooked,
                                onClick = {
                                    onToggleCooked()
                                    snackbar.currentSnackbarData?.dismiss()
                                    scope.launch {
                                        if (snackbar.showSnackbar(if (recipe.cooked) "Marked as not cooked" else "Marked cooked", actionLabel = "Undo") == SnackbarResult.ActionPerformed) latestToggleCooked()
                                    }
                                },
                                label = { Text(if (recipe.cooked) "Cooked" else "Mark cooked") },
                                leadingIcon = {
                                    Icon(
                                        if (recipe.cooked) Icons.Default.Check else Icons.Default.Restaurant,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = if (recipe.cooked) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onSurface
                                ),
                                shape = RoundedCornerShape(AppRadii.control),
                                modifier = Modifier.heightIn(min = 48.dp)
                            )

                            FilterChip(
                                selected = recipe.cookPhotoPath.isNotBlank(),
                                onClick = onRequestCookPhoto,
                                label = { Text(if (recipe.cookPhotoPath.isBlank()) "Add cook photo" else "Replace photo") },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.AddAPhoto,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                shape = RoundedCornerShape(AppRadii.control),
                                modifier = Modifier.heightIn(min = 48.dp)
                            )

                            if (recipe.cookPhotoPath.isNotBlank()) {
                                TextButton(
                                    onClick = onRemoveCookPhoto,
                                    modifier = Modifier.heightIn(min = 48.dp)
                                ) {
                                    Text("Remove photo")
                                }
                            }

                            if (trustedSourceUrl != null) {
                                FilterChip(
                                    selected = false,
                                    onClick = {
                                        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(trustedSourceUrl))) }
                                    },
                                    label = { Text("Watch original Reel") },
                                    leadingIcon = {
                                        Icon(
                                            Icons.AutoMirrored.Filled.OpenInNew,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                            tint = MaterialTheme.colorScheme.secondary
                                        )
                                    },
                                    shape = RoundedCornerShape(AppRadii.control),
                                    modifier = Modifier.heightIn(min = 48.dp)
                                )
                            }
                        }
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

                // Immediate visibility of Ingredients & Method above the fold!
                if (isWideLayout) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(18.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            RecipeIngredientsPanel(
                                recipe = recipe,
                                servingScale = servingScale,
                                onServingScaleChange = { servingScale = it },
                                checkedSet = checkedSet,
                                onToggleIngredient = ::toggleIngredient,
                                onClearChecked = { checkedIngredientsCsv = "" },
                                modifier = Modifier.weight(0.95f)
                            )
                            RecipeMethodPanel(recipe, Modifier.weight(1.05f))
                        }
                    }
                } else {
                    item {
                        RecipeIngredientsPanel(
                            recipe = recipe,
                            servingScale = servingScale,
                            onServingScaleChange = { servingScale = it },
                            checkedSet = checkedSet,
                            onToggleIngredient = ::toggleIngredient,
                            onClearChecked = { checkedIngredientsCsv = "" }
                        )
                    }
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
    checkedSet: Set<String>,
    onToggleIngredient: (String) -> Unit,
    onClearChecked: () -> Unit,
    modifier: Modifier = Modifier
) {
    val motionMode = LocalMotionMode.current
    val reducedMotion = LocalReducedMotion.current
    var highlightQuantities by remember { mutableStateOf(false) }

    LaunchedEffect(servingScale) {
        if (servingScale != 1.0f && motionMode == MotionMode.Standard && !reducedMotion) {
            highlightQuantities = true
            delay(450)
            highlightQuantities = false
        }
    }

    val quantityColor by animateColorAsState(
                targetValue = MaterialTheme.colorScheme.primary,
        animationSpec = if (motionMode == MotionMode.Standard && !reducedMotion) tween(AppMotion.content) else snap(),
        label = "quantity scale highlight"
    )

    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(AppRadii.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Ingredients", style = LocalRecipeTypeScale.current.sectionTitle)
                    if (checkedSet.isNotEmpty()) {
                        TextButton(
                            onClick = onClearChecked,
                            contentPadding = PaddingValues(0.dp),
                            modifier = Modifier.heightIn(min = 28.dp)
                        ) {
                            Text("Reset prep checklist (${checkedSet.size})", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
                ServingScalerSelector(currentScale = servingScale, onScaleSelected = onServingScaleChange)
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
            if (recipe.ingredients.isEmpty()) {
                Text("No ingredients listed yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                recipe.ingredients.forEachIndexed { index, rawIngredient ->
                    val scaledIngredient = ServingScaler.scaleIngredient(rawIngredient, servingScale)
                    val (qtyPart, descPart) = remember(scaledIngredient) {
                        ServingScaler.splitIngredientParts(scaledIngredient)
                    }
                    val key = index.toString()
                    val isChecked = checkedSet.contains(key)
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .toggleable(
                                    value = isChecked,
                                    role = Role.Checkbox,
                                    onValueChange = { onToggleIngredient(key) }
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
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = buildAnnotatedString {
                                    if (qtyPart != null) {
                                        withStyle(
                                            SpanStyle(
                                                fontWeight = FontWeight.Bold,
                                                color = if (isChecked) MaterialTheme.colorScheme.onSurfaceVariant else quantityColor
                                            )
                                        ) {
                                            append(qtyPart)
                                            append("  ")
                                        }
                                    }
                                    append(descPart)
                                },
                                style = LocalRecipeTypeScale.current.ingredient,
                                color = if (isChecked) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                                textDecoration = if (isChecked) TextDecoration.LineThrough else null,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (index < recipe.ingredients.lastIndex) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                        }
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
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
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

@OptIn(ExperimentalLayoutApi::class)
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
        FlowRow(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
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

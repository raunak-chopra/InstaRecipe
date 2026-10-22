package com.instarecipe.app.ui.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.instarecipe.app.DietType
import com.instarecipe.app.Recipe
import com.instarecipe.app.displayCategory
import com.instarecipe.app.displayLabel
import com.instarecipe.app.displayTitle
import com.instarecipe.app.effectiveDietType
import com.instarecipe.app.isPlaceholderRecipeText
import com.instarecipe.app.ui.theme.AccentLeaf
import com.instarecipe.app.ui.theme.AccentTomatoDeep
import com.instarecipe.app.ui.theme.AppElevation
import com.instarecipe.app.ui.theme.AppMotion
import com.instarecipe.app.ui.theme.AppRadii
import com.instarecipe.app.ui.theme.AppSpacing
import com.instarecipe.app.ui.theme.BorderSoft
import com.instarecipe.app.ui.theme.FeedbackSuccess
import com.instarecipe.app.ui.theme.LocalMotionMode
import com.instarecipe.app.ui.theme.LocalReducedMotion
import com.instarecipe.app.ui.theme.MotionMode
import com.instarecipe.app.ui.theme.SuccessContainer
import com.instarecipe.app.ui.theme.TextSecondary
import com.instarecipe.app.ui.theme.TomatoContainer

private val CookTimePattern = Regex(
    """(?i)(?:cook|prep|total)?\s*time:?\s*(\d+\s*(?:mins?|minutes?|hours?|hrs?))"""
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ModernRecipeCard(
    recipe: Recipe,
    onOpen: () -> Unit,
    onToggleFavorite: () -> Unit,
    onToggleCooked: () -> Unit,
    onStartCooking: () -> Unit,
    modifier: Modifier = Modifier,
    onTagClick: (String) -> Unit = {},
    footerActionLabel: String? = null,
    onFooterAction: (() -> Unit)? = null,
    footerActionEnabled: Boolean = true
) {
    val displayTitle = recipe.displayTitle()
    Card(
        onClick = onOpen,
        shape = RoundedCornerShape(AppRadii.card),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = AppElevation.resting),
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                role = Role.Button
                contentDescription = "$displayTitle. Open recipe"
            }
    ) {
        CompactRecipeSummary(
            recipe = recipe,
            onToggleFavorite = onToggleFavorite,
            onToggleCooked = onToggleCooked,
            onStartCooking = onStartCooking,
            onTagClick = onTagClick,
            footerActionLabel = footerActionLabel,
            onFooterAction = onFooterAction,
            footerActionEnabled = footerActionEnabled
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RecipeSummary(
    recipe: Recipe,
    onToggleFavorite: () -> Unit,
    onToggleCooked: () -> Unit,
    onStartCooking: () -> Unit,
    onTagClick: (String) -> Unit,
    footerActionLabel: String?,
    onFooterAction: (() -> Unit)?,
    footerActionEnabled: Boolean
) {
    val reducedMotion = LocalReducedMotion.current
    val motionMode = LocalMotionMode.current
    val displayTitle = recipe.displayTitle()
    val favoriteScale by animateFloatAsState(
        targetValue = if (recipe.favorite && !reducedMotion) 1.08f else 1f,
        animationSpec = if (motionMode == MotionMode.Standard && !reducedMotion) {
            tween(AppMotion.state)
        } else {
            snap()
        },
        label = "favorite feedback"
    )
    val cookTime = remember(recipe.totalTimeMinutes, recipe.notes) {
        recipe.totalTimeMinutes?.let { "$it min" } ?: extractTimeFromNotes(recipe.notes)
    }
    var showIngredients by rememberSaveable(recipe.id) { mutableStateOf(false) }

    val flipRotation by animateFloatAsState(
        targetValue = if (showIngredients) 180f else 0f,
        animationSpec = if (motionMode == MotionMode.Standard && !reducedMotion) {
            tween(durationMillis = AppMotion.editorial, easing = AppMotion.standardEasing)
        } else {
            snap()
        },
        label = "index card flip"
    )

    Column {
        if (recipe.cookPhotoPath.isNotBlank() && !showIngredients) {
            Box {
                PersonalCookPhoto(
                    path = recipe.cookPhotoPath,
                    contentDescription = "Your photo of ${recipe.title}",
                    modifier = Modifier.fillMaxWidth().height(148.dp)
                )
                Surface(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                    shape = RoundedCornerShape(AppRadii.hairline),
                    modifier = Modifier.align(Alignment.BottomStart).padding(10.dp)
                ) {
                    Text(
                        "YOUR COOK",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.8.sp),
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
        Column(
            modifier = Modifier.padding(AppSpacing.md),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)
        ) {
            // Consolidated Editorial Eyebrow Row + Quick Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    val effectiveDietType = recipe.effectiveDietType()
                    if (effectiveDietType != DietType.Unknown) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(
                                    if (effectiveDietType == DietType.NonVegetarian) AccentTomatoDeep
                                    else AccentLeaf
                                )
                        )
                    }
                    Text(
                        text = buildString {
                            append(recipe.displayCategory().uppercase())
                            if (effectiveDietType != DietType.Unknown) {
                                append(" · ")
                                append(effectiveDietType.displayLabel().uppercase())
                            }
                            cookTime?.let {
                                append(" · ")
                                append(it.uppercase())
                            }
                        },
                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.9.sp),
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Row {
                    IconButton(
                        onClick = { showIngredients = !showIngredients },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.List,
                            contentDescription = if (showIngredients) "Show recipe summary" else "Show ingredients",
                            tint = if (showIngredients) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = onToggleCooked,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = if (recipe.cooked) "Mark as not cooked" else "Mark as cooked",
                            tint = if (recipe.cooked) FeedbackSuccess else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = onToggleFavorite,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            if (recipe.favorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = if (recipe.favorite) "Remove from favorites" else "Add to favorites",
                            tint = if (recipe.favorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.scale(favoriteScale)
                        )
                    }
                }
            }

            val use3dFlip = motionMode == MotionMode.Standard && !reducedMotion
            Box(
                modifier = if (use3dFlip) {
                    Modifier.graphicsLayer {
                        val angle = if (flipRotation <= 90f) flipRotation else flipRotation - 180f
                        rotationY = angle
                        cameraDistance = 14f * density
                    }
                } else Modifier
            ) {
                Crossfade(
                    targetState = showIngredients,
                    animationSpec = if (motionMode == MotionMode.Off || reducedMotion) snap() else tween(AppMotion.state),
                    label = "card face"
                ) { isBackShowing ->
                    if (isBackShowing) {
                        IngredientCardBack(recipe = recipe, onShowSummary = { showIngredients = false })
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
                            Text(
                                displayTitle,
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    color = if (recipe.cooked) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.primaryContainer,
                                    shape = RoundedCornerShape(AppRadii.hairline)
                                ) {
                                    Text(
                                        if (recipe.cooked) "COOKED" else "READY TO COOK",
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.7.sp),
                                        color = if (recipe.cooked) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onPrimaryContainer,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                Text(
                                    "${recipe.ingredients.size} ingredients · ${recipe.steps.size} steps",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            recipe.creator.takeIf { it.isNotBlank() && !it.isPlaceholderRecipeText() }?.let {
                                Text("By $it", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }

                            if (recipe.tags.isNotEmpty()) {
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    recipe.tags.take(3).forEach { tag ->
                                        AssistChip(
                                            onClick = { onTagClick(tag) },
                                            label = { Text(tag) },
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                            shape = RoundedCornerShape(AppRadii.surface)
                                        )
                                    }
                                }
                            }

                            if (footerActionLabel != null && onFooterAction != null) {
                                Button(
                                    onClick = onFooterAction,
                                    enabled = footerActionEnabled,
                                    shape = RoundedCornerShape(AppRadii.control),
                                    contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 48.dp)
                                        .semantics {
                                            stateDescription = footerActionLabel
                                            liveRegion = LiveRegionMode.Polite
                                        }
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(footerActionLabel)
                                }
                            } else {
                                HorizontalDivider(
                                    modifier = Modifier.padding(top = 4.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    TextButton(
                                        onClick = { showIngredients = true },
                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp),
                                        modifier = Modifier.heightIn(min = 48.dp)
                                    ) {
                                        Text(
                                            "Flip for ingredients (${recipe.ingredients.size})",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    if (recipe.steps.isNotEmpty()) {
                                        Button(
                                            onClick = onStartCooking,
                                            shape = RoundedCornerShape(AppRadii.control),
                                            contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
                                            modifier = Modifier.heightIn(min = 48.dp)
                                        ) {
                                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text("Cook now")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun IngredientCardBack(recipe: Recipe, onShowSummary: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        shape = RoundedCornerShape(AppRadii.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "INDEX CARD · INGREDIENTS",
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    recipe.displayTitle(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(start = 8.dp).weight(1f, fill = false)
                )
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
            if (recipe.ingredients.isEmpty()) {
                Text(
                    "No ingredients listed yet. Open this recipe to add them.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            } else {
                recipe.ingredients.take(8).forEach { ingredient ->
                    val (qty, desc) = remember(ingredient) { ServingScaler.splitIngredientParts(ingredient) }
                    Column {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs),
                            verticalAlignment = Alignment.Top,
                            modifier = Modifier.padding(vertical = 3.dp)
                        ) {
                            Text("•", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyMedium)
                            Text(
                                text = buildAnnotatedString {
                                    if (qty != null) {
                                        withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)) {
                                            append(qty)
                                            append(" ")
                                        }
                                    }
                                    append(desc)
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
                    }
                }
                if (recipe.ingredients.size > 8) {
                    Text(
                        "+${recipe.ingredients.size - 8} more in full recipe",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
            TextButton(onClick = onShowSummary, modifier = Modifier.heightIn(min = 48.dp)) {
                Text("Flip back to summary")
            }
        }
    }
}

private fun extractTimeFromNotes(notes: String): String? =
    notes.takeIf(String::isNotBlank)?.let { CookTimePattern.find(it)?.groupValues?.getOrNull(1) }

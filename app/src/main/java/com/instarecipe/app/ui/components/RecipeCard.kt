package com.instarecipe.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.instarecipe.app.Recipe
import com.instarecipe.app.DietType
import com.instarecipe.app.displayCategory
import com.instarecipe.app.displayLabel
import com.instarecipe.app.displayTitle
import com.instarecipe.app.effectiveDietType
import com.instarecipe.app.homeCategoryKey
import com.instarecipe.app.isPlaceholderRecipeText
import com.instarecipe.app.ui.theme.AppElevation
import com.instarecipe.app.ui.theme.AppMotion
import com.instarecipe.app.ui.theme.AppRadii
import com.instarecipe.app.ui.theme.AppSpacing
import com.instarecipe.app.ui.theme.BorderSoft
import com.instarecipe.app.ui.theme.FeedbackSuccess
import com.instarecipe.app.ui.theme.SuccessContainer
import com.instarecipe.app.ui.theme.TomatoContainer
import com.instarecipe.app.ui.theme.LocalMotionMode
import com.instarecipe.app.ui.theme.LocalReducedMotion
import com.instarecipe.app.ui.theme.TextSecondary

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
        shape = RoundedCornerShape(AppRadii.surface),
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
        RecipeSummary(
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
        animationSpec = if (motionMode == com.instarecipe.app.ui.theme.MotionMode.Standard && !reducedMotion) {
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

    Column {
        if (recipe.cookPhotoPath.isNotBlank()) {
            PersonalCookPhoto(
                path = recipe.cookPhotoPath,
                contentDescription = "Your photo of ${recipe.title}",
                modifier = Modifier.fillMaxWidth().height(132.dp)
            )
        }
        Column(
            modifier = Modifier.padding(AppSpacing.md),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = when (recipe.homeCategoryKey()) {
                        "veg" -> MaterialTheme.colorScheme.secondaryContainer
                        "nonveg" -> MaterialTheme.colorScheme.errorContainer
                        "dinner" -> if (recipe.effectiveDietType() == DietType.NonVegetarian) {
                            MaterialTheme.colorScheme.errorContainer
                        } else {
                            MaterialTheme.colorScheme.secondaryContainer
                        }
                        "breakfast", "snack" -> MaterialTheme.colorScheme.tertiaryContainer
                        "lunch" -> MaterialTheme.colorScheme.primaryContainer
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    },
                    shape = RoundedCornerShape(AppRadii.surface)
                ) {
                    Text(
                        recipe.displayCategory(),
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                val effectiveDietType = recipe.effectiveDietType()
                if (effectiveDietType != DietType.Unknown) {
                    Surface(
                        color = if (effectiveDietType == DietType.NonVegetarian) {
                            MaterialTheme.colorScheme.errorContainer
                        } else {
                            MaterialTheme.colorScheme.tertiaryContainer
                        },
                        shape = RoundedCornerShape(AppRadii.surface)
                    ) {
                        Text(
                            effectiveDietType.displayLabel(),
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                Spacer(Modifier.weight(1f))
                Row {
                    IconButton(
                        onClick = { showIngredients = !showIngredients },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.List,
                            contentDescription = if (showIngredients) "Show recipe summary" else "Show ingredients"
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
            if (showIngredients) {
                IngredientCardBack(recipe = recipe, onShowSummary = { showIngredients = false })
            } else {
                Text(
                    displayTitle,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "${recipe.ingredients.size} ingredients  ·  ${recipe.steps.size} steps",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Surface(
                    color = if (recipe.cooked) SuccessContainer else TomatoContainer,
                    shape = RoundedCornerShape(AppRadii.surface)
                ) {
                    Text(
                        if (recipe.cooked) "COOKED" else "READY TO COOK",
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.8.sp),
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    recipe.creator.takeIf { it.isNotBlank() && !it.isPlaceholderRecipeText() }?.let {
                        Text("By $it", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                    }
                    cookTime?.let { time ->
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                            Icon(Icons.Default.Timer, contentDescription = null, modifier = Modifier.size(14.dp))
                            Text(time, style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                        }
                    }
                }
                if (recipe.tags.isNotEmpty()) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        recipe.tags.take(3).forEach { tag ->
                            AssistChip(
                                onClick = { onTagClick(tag) },
                                label = { Text(tag) },
                                border = BorderStroke(1.dp, BorderSoft),
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Open recipe", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
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

@Composable
private fun IngredientCardBack(recipe: Recipe, onShowSummary: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
        Text("Ingredients", style = MaterialTheme.typography.titleLarge)
        if (recipe.ingredients.isEmpty()) {
            Text("No ingredients listed yet. Edit this recipe to add them.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            recipe.ingredients.take(8).forEach { ingredient ->
                Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm), verticalAlignment = Alignment.Top) {
                    Text("•", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleMedium)
                    Text(ingredient, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                }
            }
            if (recipe.ingredients.size > 8) {
                Text("+${recipe.ingredients.size - 8} more in the recipe", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
            }
        }
        TextButton(onClick = onShowSummary, modifier = Modifier.heightIn(min = 48.dp)) {
            Text("Back to recipe summary")
        }
    }
}

private fun extractTimeFromNotes(notes: String): String? =
    notes.takeIf(String::isNotBlank)?.let { CookTimePattern.find(it)?.groupValues?.getOrNull(1) }

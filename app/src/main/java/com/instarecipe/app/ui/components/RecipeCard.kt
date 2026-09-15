package com.instarecipe.app.ui.components

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.instarecipe.app.Recipe
import com.instarecipe.app.ui.theme.AppElevation
import com.instarecipe.app.ui.theme.AppMotion
import com.instarecipe.app.ui.theme.AppSpacing
import com.instarecipe.app.ui.theme.LocalRecipeTypeScale
import com.instarecipe.app.ui.theme.LocalReducedMotion
import com.instarecipe.app.ui.theme.SuccessSage

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
    onTagClick: (String) -> Unit = {}
) {
    Card(
        onClick = onOpen,
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = AppElevation.resting),
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                role = Role.Button
                contentDescription = "${recipe.title}. Open recipe"
            }
    ) {
        RecipeSummary(recipe, onToggleFavorite, onToggleCooked, onStartCooking, onTagClick)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RecipeSummary(
    recipe: Recipe,
    onToggleFavorite: () -> Unit,
    onToggleCooked: () -> Unit,
    onStartCooking: () -> Unit,
    onTagClick: (String) -> Unit
) {
    val type = LocalRecipeTypeScale.current
    val reducedMotion = LocalReducedMotion.current
    val favoriteScale by animateFloatAsState(
        targetValue = if (recipe.favorite && !reducedMotion) 1.08f else 1f,
        animationSpec = if (reducedMotion) snap() else tween(AppMotion.state),
        label = "favorite feedback"
    )
    val cookedTint by animateColorAsState(
        targetValue = if (recipe.cooked) SuccessSage else MaterialTheme.colorScheme.outline,
        animationSpec = if (reducedMotion) snap() else tween(AppMotion.state),
        label = "cooked feedback"
    )
    val cookTime = remember(recipe.notes) { extractTimeFromNotes(recipe.notes) }
    Column(modifier = Modifier.padding(AppSpacing.md), verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val isSavedToTry = recipe.category.equals("Saved to try", ignoreCase = true)
            Surface(
                shape = MaterialTheme.shapes.extraSmall,
                color = if (isSavedToTry) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant
            ) {
                Text(
                    recipe.category.ifBlank { "Recipe" }.uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.7.sp),
                    fontWeight = FontWeight.Bold,
                    color = if (isSavedToTry) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = AppSpacing.xs, vertical = AppSpacing.xxs)
                )
            }
            Row {
                IconButton(onClick = onToggleCooked, modifier = Modifier.size(48.dp)) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = if (recipe.cooked) "Mark as not cooked" else "Mark as cooked",
                        tint = cookedTint
                    )
                }
                IconButton(onClick = onToggleFavorite, modifier = Modifier.size(48.dp)) {
                    Icon(
                        if (recipe.favorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = if (recipe.favorite) "Remove from favorites" else "Add to favorites",
                        tint = if (recipe.favorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                        modifier = Modifier.scale(favoriteScale)
                    )
                }
            }
        }
        Text(
            recipe.title.ifBlank { "Untitled Recipe" },
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
            if (recipe.creator.isNotBlank()) {
                Text("By ${recipe.creator}", style = type.metadata, color = MaterialTheme.colorScheme.primary)
            }
            cookTime?.let { time ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.xxs)) {
                    Icon(Icons.Default.Timer, contentDescription = null, modifier = Modifier.size(14.dp))
                    Text(time, style = type.metadata)
                }
            }
        }
        Text(
            "${recipe.ingredients.size} ingredients  •  ${recipe.steps.size} steps",
            style = type.metadata,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (recipe.tags.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                recipe.tags.take(3).forEach { tag ->
                    Surface(
                        onClick = { onTagClick(tag) },
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(tag, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp))
                    }
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Tap to open recipe", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
                if (recipe.steps.isNotEmpty()) {
                    Button(
                        onClick = onStartCooking,
                        shape = MaterialTheme.shapes.small,
                        contentPadding = ButtonDefaults.ButtonWithIconContentPadding
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Cook")
                    }
                }
            }
        }
    }
}

private fun extractTimeFromNotes(notes: String): String? =
    notes.takeIf(String::isNotBlank)?.let { CookTimePattern.find(it)?.groupValues?.getOrNull(1) }

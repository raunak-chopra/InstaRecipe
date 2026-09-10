package com.instarecipe.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Restaurant
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.instarecipe.app.Recipe

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
    var flipped by rememberSaveable(recipe.id) { mutableStateOf(false) }
    val rotation by animateFloatAsState(
        targetValue = if (flipped) 180f else 0f,
        animationSpec = spring(stiffness = 420f, dampingRatio = 0.82f),
        label = "recipe card flip"
    )

    Card(
        onClick = { flipped = !flipped },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 278.dp)
            .graphicsLayer {
                rotationY = rotation
                cameraDistance = 14f * density
            }
            .semantics {
                role = Role.Button
                contentDescription = if (flipped) {
                    "${recipe.title}, ingredients shown. Tap to show recipe summary"
                } else {
                    "${recipe.title}. Tap to show main ingredients"
                }
            }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { rotationY = if (rotation > 90f) 180f else 0f }
        ) {
            if (rotation <= 90f) {
                RecipeCardFront(recipe, onOpen, onToggleFavorite, onToggleCooked, onStartCooking, onTagClick)
            } else {
                RecipeCardBack(recipe, onOpen)
            }
        }
    }
}
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RecipeCardFront(
    recipe: Recipe,
    onOpen: () -> Unit,
    onToggleFavorite: () -> Unit,
    onToggleCooked: () -> Unit,
    onStartCooking: () -> Unit,
    onTagClick: (String) -> Unit
) {
    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                Text(
                    recipe.category.ifBlank { "Recipe" }.uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.8.sp, fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
            Row {
                IconButton(onClick = onToggleCooked, modifier = Modifier.size(48.dp)) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = if (recipe.cooked) "Mark as not cooked" else "Mark as cooked",
                        tint = if (recipe.cooked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                    )
                }
                IconButton(onClick = onToggleFavorite, modifier = Modifier.size(48.dp)) {
                    Icon(
                        if (recipe.favorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = if (recipe.favorite) "Remove from favorites" else "Add to favorites",
                        tint = if (recipe.favorite) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline
                    )
                }
            }
        }

        Text(
            recipe.title.ifBlank { "Untitled Recipe" },
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, fontSize = 20.sp),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (recipe.creator.isNotBlank()) {
                Text("By ${recipe.creator}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
            }
            extractTimeFromNotes(recipe.notes)?.let { time ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Default.Timer, contentDescription = null, modifier = Modifier.size(14.dp))
                    Text(time, style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        Text(
            "${recipe.ingredients.size} ingredients  •  ${recipe.steps.size} steps",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium
        )

        if (recipe.tags.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                recipe.tags.take(4).forEach { tag ->
                    Surface(
                        onClick = { onTagClick(tag) },
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.padding(vertical = 2.dp)
                    ) {
                        Text(tag, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp))
                    }
                }
            }
        }

        Spacer(Modifier.weight(1f, fill = false))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Tap card for ingredients", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (recipe.steps.isNotEmpty()) {
                    Button(onClick = onStartCooking, shape = RoundedCornerShape(12.dp), contentPadding = ButtonDefaults.ButtonWithIconContentPadding) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Cook")
                    }
                }
                Button(
                    onClick = onOpen,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Text("View")
                    Spacer(Modifier.width(4.dp))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
private fun RecipeCardBack(recipe: Recipe, onOpen: () -> Unit) {
    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Default.Restaurant, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text("Main ingredients", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
        Text(recipe.title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (recipe.ingredients.isEmpty()) {
            Text("No ingredients have been added yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            recipe.ingredients.take(6).forEach { ingredient ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
                    Text("•", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    Text(ingredient, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
            if (recipe.ingredients.size > 6) {
                Text("+${recipe.ingredients.size - 6} more", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            }
        }
        Spacer(Modifier.weight(1f, fill = false))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Tap card to flip back", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Button(onClick = onOpen, shape = RoundedCornerShape(12.dp)) {
                Text("View recipe")
                Spacer(Modifier.width(6.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
            }
        }
    }
}

private fun extractTimeFromNotes(notes: String): String? {
    if (notes.isBlank()) return null
    return Regex("""(?i)(?:cook|prep|total)?\s*time:?\s*(\d+\s*(?:mins?|minutes?|hours?|hrs?))""")
        .find(notes)?.groupValues?.getOrNull(1)
}

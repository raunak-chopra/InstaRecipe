package com.instarecipe.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import com.instarecipe.app.*

@Composable
internal fun CompactRecipeSummary(
    recipe: Recipe,
    onToggleFavorite: () -> Unit,
    onToggleCooked: () -> Unit,
    onStartCooking: () -> Unit,
    onTagClick: (String) -> Unit,
    footerActionLabel: String?,
    onFooterAction: (() -> Unit)?,
    footerActionEnabled: Boolean
) {
    var showMenu by remember { mutableStateOf(false) }
    val title = recipe.displayTitle()
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(title, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
                Text(
                    buildList {
                        recipe.totalTimeMinutes?.takeIf { it > 0 }?.let { add("$it min") }
                        add(recipe.displayCategory())
                        if (recipe.cooked) add("Cooked")
                    }.joinToString(" · "),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                recipe.creator.takeIf { it.isNotBlank() && !it.isPlaceholderRecipeText() }?.let {
                    Text("By $it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            IconButton(
                onClick = onToggleFavorite,
                modifier = Modifier.size(48.dp).semantics { stateDescription = if (recipe.favorite) "Favorited" else "Not favorited" }
            ) {
                Icon(
                    if (recipe.favorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = if (recipe.favorite) "Remove from favorites" else "Add to favorites",
                    tint = if (recipe.favorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Box {
                IconButton(onClick = { showMenu = true }, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Default.MoreVert, contentDescription = "More actions for $title")
                }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(text = { Text(if (recipe.cooked) "Mark as not cooked" else "Mark as cooked") }, onClick = {
                        showMenu = false
                        onToggleCooked()
                    })
                    if (recipe.isCookbookReady()) {
                        DropdownMenuItem(text = { Text("Cook now") }, onClick = { showMenu = false; onStartCooking() })
                    }
                    recipe.tags.filterNot { TagNormalizer.key(it) in setOf("instagram", "needs review", "processing") }.take(3).forEach { tag ->
                        DropdownMenuItem(text = { Text("Find more: $tag") }, onClick = { showMenu = false; onTagClick(tag) })
                    }
                }
            }
        }
        if (recipe.cookPhotoPath.isNotBlank()) {
            PersonalCookPhoto(path = recipe.cookPhotoPath, contentDescription = "Your photo of $title", modifier = Modifier.fillMaxWidth().height(112.dp))
            Text("Your cook photo", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (footerActionLabel != null && onFooterAction != null) {
            OutlinedButton(
                onClick = onFooterAction, enabled = footerActionEnabled,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).semantics {
                    stateDescription = footerActionLabel
                    liveRegion = LiveRegionMode.Polite
                }
            ) { Text(footerActionLabel) }
        }
    }
}

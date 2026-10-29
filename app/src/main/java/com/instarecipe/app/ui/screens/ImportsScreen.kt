package com.instarecipe.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.instarecipe.app.*
import com.instarecipe.app.ui.components.ImportCard

@Composable
internal fun ImportsScreen(
    drafts: List<Recipe>,
    hasConnection: Boolean,
    retryingIds: Set<Long>,
    onOpen: (Recipe) -> Unit,
    onSettings: () -> Unit,
    onRetry: (Recipe) -> Unit,
    onAdd: () -> Unit,
    isLoading: Boolean = false,
    onApprove: suspend (Recipe) -> Unit
) {
    val scope = rememberCoroutineScope()
    var approvingIds by remember { mutableStateOf(emptySet<Long>()) }
    var approvalMessage by remember { mutableStateOf<String?>(null) }
    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            Modifier.fillMaxSize().widthIn(max = 800.dp).align(Alignment.TopCenter),
            contentPadding = PaddingValues(start = 20.dp, top = 12.dp, end = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { Text("Your links and unfinished recipes stay here until you save them to Recipes.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            approvalMessage?.let { message -> item {
                Text(message, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                    .then(Modifier.semantics { liveRegion = LiveRegionMode.Polite }))
                TextButton(onClick = { approvalMessage = null }) { Text("Dismiss") }
            } }
            if (isLoading) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
            if (!isLoading && drafts.isEmpty()) item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("All caught up", style = MaterialTheme.typography.headlineSmall)
                    Text("Share a cooking Reel or paste a link when you find something worth making.")
                    Button(onClick = onAdd) { Text("Add a recipe") }
                }
            }
            items(drafts.sortedBy { if (it.importStage(hasConnection, it.id in retryingIds) == ImportStage.Working) 1 else 0 }, key = { it.id }) { recipe ->
                ImportCard(recipe, hasConnection, recipe.id in retryingIds, { onOpen(recipe) }, onSettings, { onRetry(recipe) },
                    isApproving = recipe.id in approvingIds,
                    onApprove = {
                        if (recipe.id !in approvingIds) {
                            approvingIds = approvingIds + recipe.id
                            scope.launch {
                                try {
                                    onApprove(recipe)
                                    approvalMessage = "Saved to Recipes: ${recipe.displayTitle()}"
                                } catch (cancelled: CancellationException) { throw cancelled }
                                catch (_: Exception) {
                                    approvalMessage = "Couldn't approve this recipe. Review the current draft and try again."
                                }
                                finally { approvingIds = approvingIds - recipe.id }
                            }
                        }
                    })
            }
        }
    }
}

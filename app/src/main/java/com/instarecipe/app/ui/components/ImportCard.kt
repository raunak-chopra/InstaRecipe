package com.instarecipe.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import com.instarecipe.app.*
import com.instarecipe.app.ui.theme.AppRadii

@Composable
internal fun ImportCard(
    recipe: Recipe,
    hasConnection: Boolean,
    isRetrying: Boolean,
    onReview: () -> Unit,
    onSettings: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    val stage = recipe.importStage(hasConnection, isRetrying)
    Card(
        modifier = modifier.fillMaxWidth().semantics {
            stateDescription = stage.label
            liveRegion = LiveRegionMode.Polite
        },
        shape = RoundedCornerShape(AppRadii.card),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stage.label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            if (stage == ImportStage.Working) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            Text(recipe.displayTitle(), style = MaterialTheme.typography.titleLarge)
            Text(recipe.importDescription(stage), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Button(
                onClick = if (stage == ImportStage.Connection) onSettings else onReview,
                enabled = stage != ImportStage.Working,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
            ) { Text(stage.action) }
            if (stage == ImportStage.Connection) {
                TextButton(onClick = onReview, modifier = Modifier.heightIn(min = 48.dp)) { Text("Edit draft manually") }
            }
            if (stage != ImportStage.Working && stage != ImportStage.Connection && recipe.canExtractFromLinkAgain()) {
                TextButton(onClick = onRetry, modifier = Modifier.heightIn(min = 48.dp)) { Text("Try link again") }
            }
        }
    }
}

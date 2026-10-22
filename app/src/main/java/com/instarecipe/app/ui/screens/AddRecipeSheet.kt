package com.instarecipe.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.instarecipe.app.recipeImportUrl

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun AddRecipeSheet(
    onDismiss: () -> Unit,
    onImportLink: (String) -> Unit,
    onImportText: (String) -> Unit,
    onImportVideo: () -> Unit,
    onWriteRecipe: () -> Unit,
    hasConnection: Boolean,
    onSettings: () -> Unit
) {
    var source by rememberSaveable { mutableStateOf("") }
    var mode by rememberSaveable { mutableStateOf("link") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().imePadding().verticalScroll(rememberScrollState()).padding(start = 20.dp, end = 20.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Add a recipe", style = MaterialTheme.typography.headlineSmall)
            Text("Keep the source. Review the ingredients before cooking.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(mode == "link", { mode = "link"; error = null }, label = { Text("Instagram link") })
                FilterChip(mode == "text", { mode = "text"; error = null }, label = { Text("Recipe text") })
            }
            OutlinedTextField(
                value = source, onValueChange = { source = it; error = null }, modifier = Modifier.fillMaxWidth(),
                label = { Text(if (mode == "link") "Paste Instagram link" else "Paste recipe text") },
                minLines = if (mode == "text") 4 else 1,
                isError = error != null,
                supportingText = { error?.let { Text(it) } }
            )
            Button(
                onClick = {
                    if (mode == "link" && recipeImportUrl(source) == null) {
                        error = "Enter an HTTPS Instagram post or Reel link."
                    } else if (source.isBlank()) {
                        error = "Paste the recipe text first."
                    } else {
                        if (mode == "link") onImportLink(source.trim()) else onImportText(source.trim())
                        onDismiss()
                    }
                }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
            ) { Text(if (mode == "link") "Import link" else if (hasConnection) "Read recipe text" else "Keep text as draft") }
            if (!hasConnection) {
                Text("AI imports use your Gemini key. Your source can be kept as a draft while you connect.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                TextButton(onClick = onSettings) { Text("Connection settings") }
            }
            HorizontalDivider()
            OutlinedButton(onClick = { onImportVideo(); onDismiss() }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Choose a cooking video") }
            OutlinedButton(onClick = { onWriteRecipe(); onDismiss() }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Write a recipe manually") }
            Text("Manual recipes work without a connection. AI imports send your chosen text or video to Google Gemini.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

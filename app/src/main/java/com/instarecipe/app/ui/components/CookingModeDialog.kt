package com.instarecipe.app.ui.components

import android.app.Activity
import android.view.WindowManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.ScreenLockPortrait
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.instarecipe.app.ui.theme.BorderSubtle
import com.instarecipe.app.ui.theme.CharcoalSlate
import com.instarecipe.app.ui.theme.DeepBasil
import com.instarecipe.app.ui.theme.DeepBasilContainer
import com.instarecipe.app.ui.theme.GoldenHoney
import com.instarecipe.app.ui.theme.HerbMuted
import com.instarecipe.app.ui.theme.SuccessSage
import com.instarecipe.app.ui.theme.ToastedSesame
import com.instarecipe.app.ui.theme.WarmSaffron
import com.instarecipe.app.ui.theme.AppMotion
import com.instarecipe.app.ui.theme.LocalRecipeTypeScale
import com.instarecipe.app.ui.theme.LocalReducedMotion
import kotlinx.coroutines.delay

@Composable
fun CookingModeDialog(
    recipeTitle: String,
    steps: List<String>,
    ingredients: List<String>,
    onClose: () -> Unit,
    onFinishAndMarkCooked: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity

    // Keep screen on while cooking with hands full
    DisposableEffect(Unit) {
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    var currentStepIndex by rememberSaveable { mutableIntStateOf(0) }
    var showIngredientsSummary by rememberSaveable { mutableStateOf(false) }

    val totalSteps = steps.size.coerceAtLeast(1)
    val progress = (currentStepIndex + 1).toFloat() / totalSteps.toFloat()

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Header
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Restaurant,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Text(
                                "Cooking Mode",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        IconButton(onClick = onClose) {
                            Icon(Icons.Default.Close, contentDescription = "Exit Cooking Mode", tint = MaterialTheme.colorScheme.onSurface)
                        }
                    }

                    Spacer(Modifier.height(4.dp))
                    Text(
                        recipeTitle,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )

                    Spacer(Modifier.height(12.dp))

                    // Progress bar & step counter
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Step ${currentStepIndex + 1} of $totalSteps",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Default.ScreenLockPortrait,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "Screen Awake",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.outlineVariant
                    )
                }

                // Middle: Main Step Instruction Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(vertical = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = MaterialTheme.shapes.medium,
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                "INSTRUCTION",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            Spacer(Modifier.height(16.dp))
                            val reducedMotion = LocalReducedMotion.current
                            Crossfade(
                                targetState = currentStepIndex,
                                animationSpec = if (reducedMotion) snap() else tween(
                                    durationMillis = AppMotion.content,
                                    easing = AppMotion.standardEasing
                                ),
                                label = "cooking step"
                            ) { stepIndex ->
                                Text(
                                    text = if (steps.isNotEmpty()) steps[stepIndex] else "No steps specified for this recipe.",
                                    style = LocalRecipeTypeScale.current.instruction,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        // Toggle ingredients sheet
                        Column {
                            FilledTonalButton(
                                onClick = { showIngredientsSummary = !showIngredientsSummary },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(if (showIngredientsSummary) "Hide Ingredients" else "Peek Ingredients (${ingredients.size})")
                            }

                            AnimatedVisibility(visible = showIngredientsSummary) {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(140.dp)
                                        .padding(top = 8.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    LazyColumn(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(10.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        items(ingredients, contentType = { "ingredient" }) { ing ->
                                            Text(
                                                "• $ing",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Bottom Section: Built-in Kitchen Timer & Navigation Controls
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    KitchenTimer()

                    // Navigation Actions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { if (currentStepIndex > 0) currentStepIndex -= 1 },
                            enabled = currentStepIndex > 0,
                            modifier = Modifier
                                .weight(1f)
                                .height(54.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                            Spacer(Modifier.size(6.dp))
                            Text("Previous")
                        }

                        if (currentStepIndex < totalSteps - 1) {
                            Button(
                                onClick = { currentStepIndex += 1 },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(54.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Text("Next Step")
                                Spacer(Modifier.size(6.dp))
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                            }
                        } else {
                            Button(
                                onClick = {
                                    onFinishAndMarkCooked()
                                    onClose()
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(54.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null)
                                Spacer(Modifier.size(6.dp))
                                Text("Finish & Mark Cooked")
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Owns ticking state so one-second updates do not invalidate the full cooking dialog. */
@Composable
private fun KitchenTimer() {
    val locale = LocalConfiguration.current.locales[0]
    var secondsRemaining by rememberSaveable { mutableIntStateOf(0) }
    var isRunning by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(isRunning) {
        while (isRunning && secondsRemaining > 0) {
            delay(1000L)
            secondsRemaining -= 1
        }
        if (secondsRemaining == 0) isRunning = false
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = MaterialTheme.shapes.medium,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(
                    Icons.Default.Timer,
                    contentDescription = null,
                    tint = if (isRunning) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
                )
                Text(
                    text = String.format(locale, "%02d:%02d", secondsRemaining / 60, secondsRemaining % 60),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (secondsRemaining > 0) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                TextButton(onClick = { secondsRemaining += 60; isRunning = true }) { Text("+1m", fontWeight = FontWeight.SemiBold) }
                TextButton(onClick = { secondsRemaining += 300; isRunning = true }) { Text("+5m", fontWeight = FontWeight.SemiBold) }
                if (secondsRemaining > 0) {
                    IconButton(onClick = { isRunning = !isRunning }) {
                        Icon(
                            if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isRunning) "Pause timer" else "Resume timer",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = { secondsRemaining = 0; isRunning = false }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Reset timer", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

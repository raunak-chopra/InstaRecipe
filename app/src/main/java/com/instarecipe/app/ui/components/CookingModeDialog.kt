package com.instarecipe.app.ui.components

import android.app.Activity
import android.media.AudioManager
import android.media.ToneGenerator
import android.view.WindowManager
import android.os.SystemClock
import com.instarecipe.app.CookingSession
import com.instarecipe.app.CookingSessions
import com.instarecipe.app.CookingTimerState
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.ScreenLockPortrait
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.instarecipe.app.R
import com.instarecipe.app.ui.theme.AppMotion
import com.instarecipe.app.ui.theme.AppRadii
import com.instarecipe.app.ui.theme.LocalMotionMode
import com.instarecipe.app.ui.theme.LocalRecipeTypeScale
import com.instarecipe.app.ui.theme.LocalReducedMotion
import com.instarecipe.app.ui.theme.MotionMode
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal data class StepDurationMention(val label: String, val totalSeconds: Int)

private val StepTimeRegex = Regex(
    """(?i)\b(\d+)\s*(?:to|-|–)\s*(\d+)\s*(hours?|hrs?|hr|minutes?|mins?|min|seconds?|secs?|sec)\b|\b(\d+)\s*(hours?|hrs?|hr|minutes?|mins?|min|seconds?|secs?|sec)\b"""
)

internal fun extractStepDurations(stepText: String): List<StepDurationMention> {
    return StepTimeRegex.findAll(stepText).mapNotNull { match ->
        val rangeEnd = match.groups[2]?.value?.toIntOrNull()
        val singleVal = match.groups[4]?.value?.toIntOrNull()
        val unitRaw = (match.groups[3]?.value ?: match.groups[5]?.value).orEmpty().lowercase()
        val amount = rangeEnd ?: singleVal ?: return@mapNotNull null
        if (amount <= 0) return@mapNotNull null
        val seconds = when {
            unitRaw.startsWith("h") -> amount * 3600
            unitRaw.startsWith("m") -> amount * 60
            unitRaw.startsWith("s") -> amount
            else -> return@mapNotNull null
        }
        val unitShort = when {
            unitRaw.startsWith("h") -> "hr"
            unitRaw.startsWith("m") -> "min"
            else -> "sec"
        }
        StepDurationMention(label = "$amount $unitShort", totalSeconds = seconds)
    }.distinctBy { it.totalSeconds }.take(3).toList()
}

private val StopWords = setOf(
    "with", "from", "into", "until", "about", "fresh", "large", "medium", "small", "finely",
    "chopped", "sliced", "diced", "minced", "grated", "ground", "tablespoon", "tablespoons",
    "teaspoon", "teaspoons", "tbsp", "cups", "grams", "ounces", "cloves", "pinch", "taste",
    "water", "warm", "cold", "room", "temperature", "optional", "divided", "halved", "peeled"
)

internal fun matchIngredientsForStep(stepText: String, ingredients: List<String>): List<String> {
    if (stepText.isBlank() || ingredients.isEmpty()) return emptyList()
    val stepLower = stepText.lowercase()
    return ingredients.filter { ingredient ->
        val (_, desc) = ServingScaler.splitIngredientParts(ingredient)
        val tokens = desc.lowercase()
            .split(Regex("[^a-z]+"))
            .filter { it.length >= 4 && it !in StopWords }
        tokens.any { token ->
            val root = if (token.endsWith("es") && token.length > 5) token.removeSuffix("es")
            else if (token.endsWith("s") && token.length > 4) token.removeSuffix("s")
            else token
            Regex("\\b${Regex.escape(root)}\\w*\\b").containsMatchIn(stepLower)
        }
    }.take(5)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CookingModeDialog(
    recipeTitle: String,
    steps: List<String>,
    ingredients: List<String>,
    initialServingScale: Float = 1.0f,
    initialCheckedIngredientsCsv: String = "",
    onClose: () -> Unit,
    onFinishAndMarkCooked: () -> Unit,
    recipeId: Long = 0L
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val coroutineScope = rememberCoroutineScope()

    // Keep screen on while cooking with hands full
    DisposableEffect(Unit) {
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    val restoredSession = remember(recipeId) { CookingSessions.load(context, recipeId) }
    var timerState by remember(recipeId) { mutableStateOf(restoredSession?.timer ?: CookingTimerState()) }
    val totalSteps = steps.size.coerceAtLeast(1)
    val pagerState = rememberPagerState(initialPage = (restoredSession?.step ?: 0).coerceIn(0, totalSteps - 1), pageCount = { totalSteps })
    val currentStepIndex = pagerState.currentPage
    var showIngredientsSummary by rememberSaveable { mutableStateOf(false) }
    var servingScale by rememberSaveable(initialServingScale) { mutableFloatStateOf(restoredSession?.servingScale ?: initialServingScale) }
    var checkedIngredientsCsv by rememberSaveable(initialCheckedIngredientsCsv) { mutableStateOf(restoredSession?.checkedIngredientsCsv ?: initialCheckedIngredientsCsv) }
    val checkedSet = remember(checkedIngredientsCsv) {
        checkedIngredientsCsv.split(",").filter(String::isNotBlank).toSet()
    }
    fun toggleIngredient(key: String) {
        val updated = if (key in checkedSet) checkedSet - key else checkedSet + key
        checkedIngredientsCsv = updated.joinToString(",")
    }
    val scaledIngredients = remember(ingredients, servingScale) {
        ingredients.map { ServingScaler.scaleIngredient(it, servingScale) }
    }
    var presetTimerRequest by remember { mutableStateOf<Pair<Int, Long>?>(null) }
    var sessionFinished by remember { mutableStateOf(false) }
    val motionMode = LocalMotionMode.current
    val reducedMotion = LocalReducedMotion.current
    val focusRequester = remember { FocusRequester() }

    fun keepSession() {
        if (!sessionFinished) CookingSessions.save(context, CookingSession(recipeId, currentStepIndex, servingScale, checkedIngredientsCsv, timerState))
    }
    LaunchedEffect(recipeId, currentStepIndex, servingScale, checkedIngredientsCsv, timerState) { keepSession() }
    val closeCooking: () -> Unit = { keepSession(); onClose() }

    val progress = (currentStepIndex + 1).toFloat() / totalSteps.toFloat()

    fun navigateToStep(targetIndex: Int) {
        val clamped = targetIndex.coerceIn(0, totalSteps - 1)
        coroutineScope.launch {
            if (motionMode == MotionMode.Standard && !reducedMotion) {
                pagerState.animateScrollToPage(clamped)
            } else {
                pagerState.scrollToPage(clamped)
            }
        }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Dialog(
        onDismissRequest = closeCooking,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .focusRequester(focusRequester)
                .focusable()
                .onPreviewKeyEvent { event ->
                    if (event.type != KeyEventType.KeyUp) return@onPreviewKeyEvent false
                    when (event.key) {
                        Key.VolumeUp -> {
                            if (currentStepIndex >= totalSteps - 1) return@onPreviewKeyEvent false
                            navigateToStep(currentStepIndex + 1)
                            true
                        }
                        Key.VolumeDown -> {
                            if (currentStepIndex <= 0) return@onPreviewKeyEvent false
                            navigateToStep(currentStepIndex - 1)
                            true
                        }
                        else -> false
                    }
                },
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .safeDrawingPadding()
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
                                stringResource(R.string.cooking_mode_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        IconButton(onClick = closeCooking) {
                            Icon(Icons.Default.Close, contentDescription = stringResource(R.string.cooking_exit), tint = MaterialTheme.colorScheme.onSurface)
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
                            stringResource(R.string.cooking_step_of, currentStepIndex + 1, totalSteps),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            if (servingScale != 1f) {
                                Text(
                                    "${servingScale.toInt()}x scale",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.VolumeUp,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    "Volume keys",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
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
                                    stringResource(R.string.cooking_screen_awake),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
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

                // Middle: Swipeable Step Instruction Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(vertical = 14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = MaterialTheme.shapes.medium,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(22.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier.weight(1f).fillMaxWidth()
                        ) { stepIndex ->
                            val stepText = if (steps.isNotEmpty()) steps[stepIndex] else stringResource(R.string.cooking_no_steps)
                            val stepDurations = remember(stepText) { extractStepDurations(stepText) }
                            val stepIngredients = remember(stepText, scaledIngredients) { matchIngredientsForStep(stepText, scaledIngredients) }
                            val stepDescription = stringResource(
                                R.string.cooking_step_of,
                                stepIndex + 1,
                                totalSteps
                            )

                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        stringResource(R.string.cooking_current_step).uppercase(),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp
                                    )
                                    if (totalSteps > 1) {
                                        Text(
                                            "Swipe or use volume keys",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Text(
                                    text = stepText,
                                    style = LocalRecipeTypeScale.current.instruction,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.semantics {
                                        liveRegion = LiveRegionMode.Polite
                                        stateDescription = stepDescription
                                    }
                                )

                                // One-tap smart timer chips extracted from the current step!
                                if (stepDurations.isNotEmpty()) {
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        stepDurations.forEach { mention ->
                                            AssistChip(
                                                onClick = {
                                                    presetTimerRequest = mention.totalSeconds to System.nanoTime()
                                                },
                                                label = {
                                                    Text(
                                                        "Start ${mention.label} timer",
                                                        fontWeight = FontWeight.SemiBold
                                                    )
                                                },
                                                leadingIcon = {
                                                    Icon(
                                                        Icons.Default.Timer,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(16.dp),
                                                        tint = MaterialTheme.colorScheme.primary
                                                    )
                                                },
                                                colors = AssistChipDefaults.assistChipColors(
                                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                                    labelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                                ),
                                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                                                shape = RoundedCornerShape(AppRadii.control)
                                            )
                                        }
                                    }
                                }

                                // Contextual Ingredients for this specific step
                                if (stepIngredients.isNotEmpty()) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                                        shape = RoundedCornerShape(AppRadii.surface),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(12.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                if (servingScale != 1f) "IN THIS STEP (${servingScale.toInt()}X SCALE)" else "IN THIS STEP",
                                                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.9.sp),
                                                color = MaterialTheme.colorScheme.secondary,
                                                fontWeight = FontWeight.Bold
                                            )
                                            stepIngredients.forEach { matched ->
                                                val (qty, desc) = remember(matched) { ServingScaler.splitIngredientParts(matched) }
                                                Text(
                                                    text = buildAnnotatedString {
                                                        append("• ")
                                                        if (qty != null) {
                                                            withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)) {
                                                                append(qty)
                                                                append(" ")
                                                            }
                                                        }
                                                        append(desc)
                                                    },
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Toggle full ingredients sheet
                        Column(modifier = Modifier.padding(top = 8.dp)) {
                            FilledTonalButton(
                                onClick = { showIngredientsSummary = !showIngredientsSummary },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    if (showIngredientsSummary) stringResource(R.string.cooking_hide_ingredients)
                                    else stringResource(R.string.cooking_peek_ingredients, scaledIngredients.size)
                                )
                            }

                            AnimatedVisibility(
                                visible = showIngredientsSummary,
                                enter = if (motionMode == MotionMode.Standard && !reducedMotion) fadeIn(tween(AppMotion.content)) else EnterTransition.None,
                                exit = if (motionMode == MotionMode.Standard && !reducedMotion) fadeOut(tween(AppMotion.state)) else ExitTransition.None
                            ) {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(172.dp)
                                        .padding(top = 8.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(10.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                "SERVINGS",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                fontWeight = FontWeight.Bold
                                            )
                                            ServingScalerSelector(
                                                currentScale = servingScale,
                                                onScaleSelected = { servingScale = it }
                                            )
                                        }
                                        LazyColumn(
                                            modifier = Modifier.fillMaxSize(),
                                            verticalArrangement = Arrangement.spacedBy(2.dp)
                                        ) {
                                            itemsIndexed(scaledIngredients, contentType = { _, _ -> "ingredient" }) { index, ing ->
                                                val key = index.toString()
                                                val isChecked = key in checkedSet
                                                val (qty, desc) = remember(ing) { ServingScaler.splitIngredientParts(ing) }
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .clickable { toggleIngredient(key) }
                                                        .padding(vertical = 2.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Checkbox(
                                                        checked = isChecked,
                                                        onCheckedChange = { toggleIngredient(key) },
                                                        colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
                                                    )
                                                    Text(
                                                        text = buildAnnotatedString {
                                                            if (qty != null) {
                                                                withStyle(
                                                                    SpanStyle(
                                                                        fontWeight = FontWeight.Bold,
                                                                        color = if (isChecked) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary
                                                                    )
                                                                ) {
                                                                    append(qty)
                                                                    append(" ")
                                                                }
                                                            }
                                                            append(desc)
                                                        },
                                                        style = MaterialTheme.typography.bodySmall,
                                                        textDecoration = if (isChecked) TextDecoration.LineThrough else TextDecoration.None,
                                                        color = if (isChecked) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                                                    )
                                                }
                                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Bottom Section: Built-in Kitchen Timer & Navigation Controls
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    KitchenTimer(timer = timerState, onTimerChange = { timerState = it }, presetRequest = presetTimerRequest)

                    // Navigation Actions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { if (currentStepIndex > 0) navigateToStep(currentStepIndex - 1) },
                            enabled = currentStepIndex > 0,
                            modifier = Modifier
                                .weight(1f)
                                .height(54.dp),
                            shape = RoundedCornerShape(AppRadii.control)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                            Spacer(Modifier.size(6.dp))
                            Text(stringResource(R.string.cooking_previous))
                        }

                        if (currentStepIndex < totalSteps - 1) {
                            Button(
                                onClick = { navigateToStep(currentStepIndex + 1) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(54.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                shape = RoundedCornerShape(AppRadii.control)
                            ) {
                                Text(stringResource(R.string.cooking_next_step))
                                Spacer(Modifier.size(6.dp))
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                            }
                        } else {
                            Button(
                                onClick = {
                                    sessionFinished = true
                                    CookingSessions.clear(context, recipeId)
                                    onFinishAndMarkCooked()
                                    onClose()
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(54.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                shape = RoundedCornerShape(AppRadii.control)
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null)
                                Spacer(Modifier.size(6.dp))
                                Text(stringResource(R.string.cooking_finish_mark_cooked))
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Only the timer display ticks; its durable state changes on user actions and completion. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun KitchenTimer(
    timer: CookingTimerState,
    onTimerChange: (CookingTimerState) -> Unit,
    presetRequest: Pair<Int, Long>? = null
) {
    val context = LocalContext.current
    val locale = LocalConfiguration.current.locales[0]
    val haptics = LocalHapticFeedback.current
    val boot = remember { CookingSessions.bootCount(context) }
    var remaining by remember { mutableIntStateOf(timer.remainingSeconds(SystemClock.elapsedRealtime())) }
    var finished by rememberSaveable { mutableStateOf(false) }
    var soundRequest by remember { mutableIntStateOf(0) }

    LaunchedEffect(presetRequest?.second) {
        presetRequest?.let { (seconds, _) ->
            finished = false
            onTimerChange(CookingTimerState().addSeconds(seconds, SystemClock.elapsedRealtime(), boot))
        }
    }
    LaunchedEffect(timer) {
        remaining = timer.remainingSeconds(SystemClock.elapsedRealtime())
        while (timer.running && remaining > 0) {
            delay(250L)
            remaining = timer.remainingSeconds(SystemClock.elapsedRealtime())
        }
        if (timer.running && remaining == 0) {
            finished = true
            soundRequest++
            onTimerChange(CookingTimerState(bootCount = boot))
        }
    }
    LaunchedEffect(soundRequest) {
        if (soundRequest > 0) {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            val tone = runCatching { ToneGenerator(AudioManager.STREAM_ALARM, 85) }.getOrNull()
            try { tone?.startTone(ToneGenerator.TONE_PROP_BEEP2, 600); delay(700L) } finally { tone?.release() }
        }
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = if (finished) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.surface),
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(String.format(locale, "%02d:%02d", remaining / 60, remaining % 60), style = MaterialTheme.typography.headlineSmall)
            if (finished) Text("Timer done", style = MaterialTheme.typography.titleMedium)
            Text("Timer alerts sound while cooking mode is open.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = { finished = false; onTimerChange(timer.addSeconds(60, SystemClock.elapsedRealtime(), boot)) }) { Text("+1 min") }
                TextButton(onClick = { finished = false; onTimerChange(timer.addSeconds(300, SystemClock.elapsedRealtime(), boot)) }) { Text("+5 min") }
                if (remaining > 0) {
                    TextButton(onClick = {
                        onTimerChange(if (timer.running) timer.paused(SystemClock.elapsedRealtime()) else timer.resumed(SystemClock.elapsedRealtime(), boot))
                    }) { Text(if (timer.running) "Pause" else "Resume") }
                }
                if (remaining > 0 || finished) TextButton(onClick = { finished = false; onTimerChange(CookingTimerState(bootCount = boot)) }) { Text("Reset") }
            }
        }
    }
}

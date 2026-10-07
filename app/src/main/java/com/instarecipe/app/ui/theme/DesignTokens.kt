package com.instarecipe.app.ui.theme

import android.animation.ValueAnimator
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Immutable
object AppSpacing {
    val xxs = 4.dp
    val xs = 8.dp
    val sm = 12.dp
    val md = 16.dp
    val lg = 20.dp
    val xl = 24.dp
    val xxl = 32.dp
    val section = 48.dp
}

@Immutable
object AppRadii {
    val hairline = 2.dp
    val surface = 4.dp
    val control = 12.dp
}

@Immutable
object AppElevation {
    val resting = 0.dp
    val raised = 2.dp
}

enum class MotionMode(
    val label: String,
    val description: String
) {
    Standard("Standard", "Gentle page and recipe transitions."),
    Reduced("Reduced", "Less movement, with essential feedback only."),
    Off("Off", "Turn off all app animations.")
}

@Immutable
object AppMotion {
    const val state = 160
    const val content = 200
    const val screen = 240
    const val editorial = 360
    val standardEasing = androidx.compose.animation.core.CubicBezierEasing(0.2f, 0f, 0f, 1f)
}

/** Recipe-specific roles keep long-form reading surfaces consistent. */
@Immutable
data class RecipeTypeScale(
    val body: TextStyle,
    val ingredient: TextStyle,
    val instruction: TextStyle,
    val quantity: TextStyle,
    val metadata: TextStyle,
    val sectionTitle: TextStyle
)

val LocalRecipeTypeScale = staticCompositionLocalOf {
    RecipeTypeScale(
        body = TextStyle(fontSize = 17.sp, lineHeight = 28.sp),
        ingredient = TextStyle(fontSize = 16.sp, lineHeight = 25.sp),
        instruction = TextStyle(fontSize = 21.sp, lineHeight = 32.sp),
        quantity = TextStyle(fontSize = 16.sp, lineHeight = 24.sp, fontWeight = FontWeight.SemiBold),
        metadata = TextStyle(fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.Medium),
        sectionTitle = TextStyle(fontSize = 22.sp, lineHeight = 27.sp, fontWeight = FontWeight.SemiBold)
    )
}

val LocalMotionMode = staticCompositionLocalOf { MotionMode.Standard }
val LocalReducedMotion = staticCompositionLocalOf { false }

/** The OS setting supplies the initial default; an explicit app choice can still be Off. */
fun systemReducedMotionEnabled(): Boolean = !ValueAnimator.areAnimatorsEnabled()

fun defaultMotionMode(): MotionMode =
    if (systemReducedMotionEnabled()) MotionMode.Reduced else MotionMode.Standard

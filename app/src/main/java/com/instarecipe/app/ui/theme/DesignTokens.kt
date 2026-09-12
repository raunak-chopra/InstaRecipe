package com.instarecipe.app.ui.theme

import android.animation.ValueAnimator
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalInspectionMode
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
}

@Immutable
object AppRadii {
    val compact = 8.dp
    val control = 12.dp
    val surface = 16.dp
}

@Immutable
object AppElevation {
    val resting = 0.dp
    val raised = 2.dp
}

@Immutable
object AppMotion {
    const val state = 110
    const val content = 190
    const val screen = 250
    val standardEasing = CubicBezierEasing(0.2f, 0f, 0f, 1f)
}

/** Recipe-specific roles that avoid one-off text styling in reading surfaces. */
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
        body = TextStyle(fontSize = 17.sp, lineHeight = 26.sp),
        ingredient = TextStyle(fontSize = 16.sp, lineHeight = 25.sp),
        instruction = TextStyle(fontSize = 22.sp, lineHeight = 32.sp),
        quantity = TextStyle(fontSize = 16.sp, lineHeight = 24.sp, fontWeight = FontWeight.SemiBold),
        metadata = TextStyle(fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.Medium),
        sectionTitle = TextStyle(fontSize = 20.sp, lineHeight = 26.sp, fontWeight = FontWeight.SemiBold)
    )
}

val LocalReducedMotion = staticCompositionLocalOf { false }

@Composable
fun systemReducedMotionEnabled(): Boolean =
    !LocalInspectionMode.current && !ValueAnimator.areAnimatorsEnabled()

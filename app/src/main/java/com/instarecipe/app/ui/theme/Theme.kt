package com.instarecipe.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.instarecipe.app.R

enum class ThemeMode { System, Light, Dark }

private val LightColorScheme = lightColorScheme(
    primary = AccentTomatoDeep,
    onPrimary = SurfaceRaised,
    primaryContainer = TomatoContainer,
    onPrimaryContainer = TextPrimary,
    secondary = AccentLeaf,
    onSecondary = SurfaceRaised,
    secondaryContainer = LeafContainer,
    onSecondaryContainer = TextPrimary,
    tertiary = AccentButter,
    onTertiary = TextPrimary,
    tertiaryContainer = ButterContainer,
    onTertiaryContainer = TextPrimary,
    background = SurfaceCanvas,
    onBackground = TextPrimary,
    surface = SurfaceRaised,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceVariantWarm,
    onSurfaceVariant = TextSecondary,
    outline = TextSecondary,
    outlineVariant = BorderSoft,
    error = FeedbackError,
    onError = SurfaceRaised,
    errorContainer = ErrorContainer,
    onErrorContainer = TextPrimary,
    surfaceTint = AccentTomatoDeep
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkTomatoPrimary,
    onPrimary = SurfaceInk,
    primaryContainer = TomatoTintedSurface,
    onPrimaryContainer = DarkTomatoOnContainer,
    secondary = DarkLeafPrimary,
    onSecondary = SurfaceInk,
    secondaryContainer = LeafTintedSurface,
    onSecondaryContainer = DarkLeafOnContainer,
    tertiary = AccentButter,
    onTertiary = SurfaceInk,
    tertiaryContainer = ButterTintedSurface,
    onTertiaryContainer = DarkButterOnContainer,
    background = SurfaceInk,
    onBackground = TextOnInk,
    surface = SurfaceInkRaised,
    onSurface = TextOnInk,
    surfaceVariant = SurfaceVariantInk,
    onSurfaceVariant = TextSecondaryOnInk,
    outline = TextSecondaryOnInk,
    outlineVariant = DarkOutline,
    error = DarkError,
    onError = DarkErrorOn,
    errorContainer = DarkErrorContainer,
    onErrorContainer = DarkErrorOnContainer,
    surfaceTint = DarkTomatoPrimary
)

/** Shapes stay quiet by default; controls opt into the tactile 12dp radius explicitly. */
val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(2.dp),
    small = RoundedCornerShape(2.dp),
    medium = RoundedCornerShape(4.dp),
    large = RoundedCornerShape(4.dp),
    extraLarge = RoundedCornerShape(4.dp)
)

private val ElvaraSans = FontFamily(
    Font(R.font.elvara_sans_regular, FontWeight.Normal),
    Font(R.font.elvara_sans_medium, FontWeight.Medium),
    Font(R.font.elvara_sans_semibold, FontWeight.SemiBold),
    Font(R.font.elvara_sans_bold, FontWeight.Bold)
)

// The plan's display font is license-gated. The platform serif keeps the editorial character
// without shipping an unverified catalog font; replace this family only after rights approval.
private val EditorialSerif = FontFamily.Serif

val AppTypography = Typography(
    displayLarge = TextStyle(fontFamily = EditorialSerif, fontWeight = FontWeight.Normal, fontSize = 56.sp, lineHeight = 57.sp, letterSpacing = (-1.1).sp),
    displayMedium = TextStyle(fontFamily = EditorialSerif, fontWeight = FontWeight.Normal, fontSize = 48.sp, lineHeight = 50.sp, letterSpacing = (-0.8).sp),
    displaySmall = TextStyle(fontFamily = EditorialSerif, fontWeight = FontWeight.Normal, fontSize = 40.sp, lineHeight = 43.sp, letterSpacing = (-0.6).sp),
    headlineLarge = TextStyle(fontFamily = EditorialSerif, fontWeight = FontWeight.Normal, fontSize = 36.sp, lineHeight = 39.sp, letterSpacing = (-0.4).sp),
    headlineMedium = TextStyle(fontFamily = EditorialSerif, fontWeight = FontWeight.Normal, fontSize = 30.sp, lineHeight = 34.sp),
    headlineSmall = TextStyle(fontFamily = ElvaraSans, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 27.sp),
    titleLarge = TextStyle(fontFamily = ElvaraSans, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 26.sp),
    titleMedium = TextStyle(fontFamily = ElvaraSans, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp),
    titleSmall = TextStyle(fontFamily = ElvaraSans, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp),
    bodyLarge = TextStyle(fontFamily = ElvaraSans, fontWeight = FontWeight.Normal, fontSize = 17.sp, lineHeight = 28.sp),
    bodyMedium = TextStyle(fontFamily = ElvaraSans, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 21.sp),
    bodySmall = TextStyle(fontFamily = ElvaraSans, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 18.sp),
    labelLarge = TextStyle(fontFamily = ElvaraSans, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp),
    labelMedium = TextStyle(fontFamily = ElvaraSans, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 17.sp),
    labelSmall = TextStyle(fontFamily = ElvaraSans, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 15.sp)
)

@Composable
fun InstaRecipeTheme(
    themeMode: ThemeMode = ThemeMode.Light,
    motionMode: MotionMode = defaultMotionMode(),
    content: @Composable () -> Unit
) {
    val useDarkTheme = when (themeMode) {
        ThemeMode.System -> isSystemInDarkTheme()
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !useDarkTheme
                isAppearanceLightNavigationBars = !useDarkTheme
            }
        }
    }
    val recipeTypeScale = RecipeTypeScale(
        body = AppTypography.bodyLarge,
        ingredient = AppTypography.bodyLarge.copy(fontSize = 16.sp, lineHeight = 25.sp),
        instruction = AppTypography.bodyLarge.copy(fontSize = 21.sp, lineHeight = 32.sp),
        quantity = AppTypography.bodyLarge.copy(fontSize = 16.sp, lineHeight = 24.sp, fontWeight = FontWeight.SemiBold),
        metadata = AppTypography.labelMedium.copy(fontSize = 13.sp, lineHeight = 18.sp),
        sectionTitle = AppTypography.titleLarge.copy(fontSize = 22.sp, lineHeight = 27.sp)
    )
    CompositionLocalProvider(
        LocalRecipeTypeScale provides recipeTypeScale,
        LocalMotionMode provides motionMode,
        LocalReducedMotion provides (motionMode != MotionMode.Standard)
    ) {
        MaterialTheme(
            colorScheme = if (useDarkTheme) DarkColorScheme else LightColorScheme,
            shapes = AppShapes,
            typography = AppTypography,
            content = content
        )
    }
}

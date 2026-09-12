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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.instarecipe.app.R

enum class ThemeMode { System, Light, Dark }

private val LightColorScheme = lightColorScheme(
    primary = Basil600, onPrimary = Color.White,
    primaryContainer = Basil100, onPrimaryContainer = Basil900,
    secondary = Tomato500, onSecondary = Color.White,
    secondaryContainer = Tomato100, onSecondaryContainer = Tomato900,
    tertiary = Honey500, onTertiary = Ink900,
    tertiaryContainer = Honey200, onTertiaryContainer = Ink900,
    background = Cream50, onBackground = Ink900,
    surface = Color.White, onSurface = Ink900,
    surfaceVariant = Cream100, onSurfaceVariant = Ink700,
    outline = Ink500, outlineVariant = Cream200,
    error = Color(0xFFBA1A1A), onError = Color.White,
    errorContainer = Color(0xFFFFDAD6), onErrorContainer = Color(0xFF410002),
    surfaceTint = Basil600
)

private val DarkColorScheme = darkColorScheme(
    primary = Basil300, onPrimary = Color(0xFF003823),
    primaryContainer = Basil900, onPrimaryContainer = Color(0xFFA9F2CA),
    secondary = Tomato300, onSecondary = Color(0xFF521204),
    secondaryContainer = Tomato900, onSecondaryContainer = Color(0xFFFFDBD1),
    tertiary = Honey200, onTertiary = Color(0xFF422C00),
    tertiaryContainer = Color(0xFF5F4100), onTertiaryContainer = Color(0xFFFFDEA1),
    background = Night950, onBackground = NightText,
    surface = Night900, onSurface = NightText,
    surfaceVariant = Night800, onSurfaceVariant = NightMuted,
    outline = Color(0xFF89958E), outlineVariant = Night700,
    error = Color(0xFFFFB4AB), onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A), onErrorContainer = Color(0xFFFFDAD6),
    surfaceTint = Basil300
)

val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(20.dp)
)

private val ElvaraSans = FontFamily(
    Font(R.font.elvara_sans_regular, FontWeight.Normal),
    Font(R.font.elvara_sans_medium, FontWeight.Medium),
    Font(R.font.elvara_sans_semibold, FontWeight.SemiBold),
    Font(R.font.elvara_sans_bold, FontWeight.Bold)
)

val AppTypography = Typography(
    displaySmall = TextStyle(fontFamily = ElvaraSans, fontWeight = FontWeight.Bold, fontSize = 36.sp, lineHeight = 42.sp),
    headlineLarge = TextStyle(fontFamily = ElvaraSans, fontWeight = FontWeight.Bold, fontSize = 30.sp, lineHeight = 36.sp),
    headlineMedium = TextStyle(fontFamily = ElvaraSans, fontWeight = FontWeight.Bold, fontSize = 24.sp, lineHeight = 30.sp),
    headlineSmall = TextStyle(fontFamily = ElvaraSans, fontWeight = FontWeight.SemiBold, fontSize = 21.sp, lineHeight = 27.sp),
    titleLarge = TextStyle(fontFamily = ElvaraSans, fontWeight = FontWeight.SemiBold, fontSize = 19.sp, lineHeight = 25.sp),
    titleMedium = TextStyle(fontFamily = ElvaraSans, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp),
    titleSmall = TextStyle(fontFamily = ElvaraSans, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp),
    bodyLarge = TextStyle(fontFamily = ElvaraSans, fontWeight = FontWeight.Normal, fontSize = 17.sp, lineHeight = 26.sp),
    bodyMedium = TextStyle(fontFamily = ElvaraSans, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 21.sp),
    bodySmall = TextStyle(fontFamily = ElvaraSans, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 18.sp),
    labelLarge = TextStyle(fontFamily = ElvaraSans, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp),
    labelMedium = TextStyle(fontFamily = ElvaraSans, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp),
    labelSmall = TextStyle(fontFamily = ElvaraSans, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 15.sp)
)

@Composable
fun InstaRecipeTheme(themeMode: ThemeMode = ThemeMode.System, content: @Composable () -> Unit) {
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
        instruction = AppTypography.headlineSmall.copy(fontSize = 22.sp, lineHeight = 32.sp, fontWeight = FontWeight.Medium),
        quantity = AppTypography.bodyLarge.copy(fontSize = 16.sp, lineHeight = 24.sp, fontWeight = FontWeight.SemiBold),
        metadata = AppTypography.labelMedium.copy(fontSize = 13.sp, lineHeight = 18.sp),
        sectionTitle = AppTypography.titleLarge.copy(fontSize = 20.sp, lineHeight = 26.sp)
    )
    CompositionLocalProvider(
        LocalRecipeTypeScale provides recipeTypeScale,
        LocalReducedMotion provides systemReducedMotionEnabled()
    ) {
        MaterialTheme(
            colorScheme = if (useDarkTheme) DarkColorScheme else LightColorScheme,
            shapes = AppShapes,
            typography = AppTypography,
            content = content
        )
    }
}

package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Immutable
data class CycleSemanticPalette(
    val isDark: Boolean,
    val background: Color,
    val cardSurface: Color,
    val cardSurfaceAlt: Color,
    val border: Color,
    val dashedBorder: Color,
    val shadow: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val accentGold: Color,
    val accentBronze: Color,
    val accentGreen: Color,
    val accentGreenBg: Color,
    val chipBg: Color,
    val alertBg: Color,
    val alertBorder: Color,
    val errorBg: Color,
    val errorFg: Color,
    val brandPillBg: Color,
    val brandPillFg: Color
)

val LightCyclePalette = CycleSemanticPalette(
    isDark = false,
    background = PaperCream,
    cardSurface = WarmSurface,
    cardSurfaceAlt = WarmSurfaceAlt,
    border = CharcoalInk,
    dashedBorder = WarmMutedBorder,
    shadow = WarmShadow,
    textPrimary = CharcoalInk,
    textSecondary = InkSecondary,
    textMuted = InkMuted,
    accentGold = GoldAccent,
    accentBronze = AmberBronze,
    accentGreen = ActiveGreen,
    accentGreenBg = ActiveGreenBg,
    chipBg = WarmPillBg,
    alertBg = AmberSoftBg,
    alertBorder = AmberBronze,
    errorBg = ErrorRedBg,
    errorFg = ErrorRed,
    brandPillBg = CharcoalInk,
    brandPillFg = PaperCream
)

val DarkCyclePalette = CycleSemanticPalette(
    isDark = true,
    background = DarkBackground,
    cardSurface = DarkSurface,
    cardSurfaceAlt = DarkSurfaceAlt,
    border = DarkBorder,
    dashedBorder = Color(0xFF52525B),
    shadow = DarkShadow,
    textPrimary = DarkTextPrimary,
    textSecondary = DarkTextSecondary,
    textMuted = DarkTextMuted,
    accentGold = GoldAccent,
    accentBronze = GoldAccent,
    accentGreen = Color(0xFF4ADE80),
    accentGreenBg = Color(0xFF14532D),
    chipBg = DarkSurfaceAlt,
    alertBg = Color(0xFF271F0E),
    alertBorder = GoldAccent,
    errorBg = Color(0xFF3F1616),
    errorFg = Color(0xFFFCA5A5),
    brandPillBg = GoldAccent,
    brandPillFg = CharcoalInk
)

val LocalCyclePalette = staticCompositionLocalOf { LightCyclePalette }

private val CycleLightColorScheme = lightColorScheme(
    primary = CharcoalInk,
    onPrimary = PaperCream,
    primaryContainer = GoldAccent,
    onPrimaryContainer = CharcoalInk,
    secondary = AmberBronze,
    onSecondary = Color.White,
    secondaryContainer = WarmPillBg,
    onSecondaryContainer = CharcoalInk,
    tertiary = ActiveGreen,
    onTertiary = Color.White,
    background = PaperCream,
    onBackground = CharcoalInk,
    surface = WarmSurface,
    onSurface = CharcoalInk,
    surfaceVariant = WarmSurfaceAlt,
    onSurfaceVariant = InkSecondary,
    outline = CharcoalInk,
    outlineVariant = WarmShadow,
    error = ErrorRed,
    onError = Color.White,
    errorContainer = ErrorRedBg,
    onErrorContainer = ErrorRed
)

private val CycleDarkColorScheme = darkColorScheme(
    primary = GoldAccent,
    onPrimary = CharcoalInk,
    primaryContainer = DarkSurfaceAlt,
    onPrimaryContainer = GoldAccent,
    secondary = GoldAccent,
    onSecondary = CharcoalInk,
    secondaryContainer = DarkSurfaceAlt,
    onSecondaryContainer = DarkTextPrimary,
    tertiary = Color(0xFF4ADE80),
    onTertiary = CharcoalInk,
    background = DarkBackground,
    onBackground = DarkTextPrimary,
    surface = DarkSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkSurfaceAlt,
    onSurfaceVariant = DarkTextSecondary,
    outline = DarkBorder,
    outlineVariant = DarkBorder,
    error = ErrorRedBright,
    onError = Color.White,
    errorContainer = Color(0xFF3F1616),
    onErrorContainer = Color(0xFFFCA5A5)
)

val CycleShapes = Shapes(
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(22.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(36.dp)
)

@Composable
fun CycleTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val palette = if (darkTheme) DarkCyclePalette else LightCyclePalette
    val colorScheme = if (darkTheme) CycleDarkColorScheme else CycleLightColorScheme

    CompositionLocalProvider(LocalCyclePalette provides palette) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = CycleTypography,
            shapes = CycleShapes,
            content = content
        )
    }
}

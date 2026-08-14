package com.sktech.wastetrack.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = EmeraldLight,
    onPrimary = CarbonBlack,
    primaryContainer = IndustrialSurfaceDark(),
    onPrimaryContainer = IndustrialGreenGlow,
    secondary = SafetyOrangeLight,
    onSecondary = CarbonBlack,
    secondaryContainer = GraphiteLight,
    onSecondaryContainer = SafetyOrangeContainer,
    tertiary = TealLight,
    onTertiary = CarbonBlack,
    background = CarbonBlack,
    onBackground = OffWhite,
    surface = Graphite,
    onSurface = OffWhite,
    surfaceVariant = GraphiteLight,
    onSurfaceVariant = TextMuted,
    error = AlertRed,
    onError = OffWhite,
    outline = SteelGray
)

private fun IndustrialSurfaceDark() = Color(0xFF064E3B)

private val LightColorScheme = lightColorScheme(
    primary = EmeraldPrimary,
    onPrimary = Color.White,
    primaryContainer = EmeraldContainer,
    onPrimaryContainer = EmeraldSurface,
    secondary = SafetyOrange,
    onSecondary = Color.White,
    secondaryContainer = SafetyOrangeContainer,
    onSecondaryContainer = Color(0xFF7C2D12),
    tertiary = Teal,
    onTertiary = Color.White,
    background = LightCanvas,
    onBackground = TextPrimary,
    surface = LightCard,
    onSurface = TextPrimary,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    error = AlertRed,
    onError = Color.White,
    outline = LightCardBorder
)

@Composable
fun WastetrackTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = colorScheme.background.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}
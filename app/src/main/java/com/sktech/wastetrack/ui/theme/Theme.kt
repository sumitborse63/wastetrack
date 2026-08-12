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
    primary = IndustrialGreenLight,
    onPrimary = Color.White,
    primaryContainer = IndustrialGreen,
    onPrimaryContainer = Color.White,
    secondary = SafetyOrange,
    onSecondary = Color.White,
    secondaryContainer = SafetyOrangeLight,
    onSecondaryContainer = Color.White,
    tertiary = Teal,
    onTertiary = Color.White,
    background = CarbonBlack,
    onBackground = LightGray,
    surface = Graphite,
    onSurface = LightGray,
    surfaceVariant = GraphiteLight,
    onSurfaceVariant = LightGray,
    error = AlertRed,
    onError = Color.White,
    outline = SteelGray
)

private val LightColorScheme = lightColorScheme(
    primary = IndustrialGreen,
    onPrimary = Color.White,
    primaryContainer = IndustrialGreenSurface,
    onPrimaryContainer = Color.White,
    secondary = SafetyOrange,
    onSecondary = Color.White,
    secondaryContainer = SafetyOrangeLight,
    onSecondaryContainer = Color.White,
    tertiary = TealDark,
    onTertiary = Color.White,
    background = LightBackground,
    onBackground = CarbonBlack,
    surface = LightSurface,
    onSurface = CarbonBlack,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = SteelGray,
    error = AlertRed,
    onError = Color.White,
    outline = SteelGray
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
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}
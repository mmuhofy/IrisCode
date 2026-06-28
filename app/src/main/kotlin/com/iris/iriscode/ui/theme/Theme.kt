package com.iris.iriscode.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val IrisColorScheme = darkColorScheme(
    primary = IrisPrimary,
    onPrimary = IrisText,
    primaryContainer = IrisPrimaryVariant,
    onPrimaryContainer = IrisText,
    secondary = IrisAccent,
    onSecondary = IrisText,
    secondaryContainer = IrisAccentVariant,
    onSecondaryContainer = IrisText,
    background = IrisBackground,
    onBackground = IrisText,
    surface = IrisSurface,
    onSurface = IrisText,
    surfaceVariant = IrisSurfaceVariant,
    onSurfaceVariant = IrisTextSubtle,
    surfaceContainerLowest = IrisBackground,
    surfaceContainerLow = IrisSurface,
    surfaceContainer = IrisSurfaceContainer,
    surfaceContainerHigh = IrisSurfaceVariant,
    surfaceContainerHighest = IrisSurfaceContainer,
    error = IrisError,
    onError = IrisText,
    outline = IrisOutline,
    outlineVariant = IrisOutline
)

@Composable
fun IrisCodeTheme(content: @Composable () -> Unit) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = IrisBackground.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = IrisColorScheme,
        typography = IrisTypography,
        content = content
    )
}

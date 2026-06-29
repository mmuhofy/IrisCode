package com.iris.iriscode.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

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
    MaterialTheme(
        colorScheme = IrisColorScheme,
        typography = IrisTypography,
        content = content
    )
}

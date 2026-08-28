package com.example.trackmytrip.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = TrackPrimary,
    onPrimary = TrackOnPrimary,
    primaryContainer = TrackPrimaryContainer,
    onPrimaryContainer = TrackOnPrimaryContainer,
    secondary = TrackSecondary,
    secondaryContainer = TrackSecondaryContainer,
    tertiary = TrackTertiary,
    tertiaryContainer = TrackTertiaryContainer,
    background = TrackSurface,
    surface = TrackSurface,
    surfaceContainer = TrackSurfaceContainer,
    surfaceContainerHigh = TrackSurfaceContainerHigh,
    onSurface = TrackOnSurface,
    onSurfaceVariant = TrackOnSurfaceVariant,
    outline = TrackOutline,
    outlineVariant = TrackOutlineVariant
)

private val DarkColorScheme = darkColorScheme(
    primary = TrackPrimaryDark,
    onPrimary = TrackOnPrimaryDark,
    primaryContainer = TrackPrimaryContainerDark,
    onPrimaryContainer = TrackOnPrimaryContainerDark,
    secondary = TrackSecondaryDark,
    secondaryContainer = TrackSecondaryContainerDark,
    tertiary = TrackTertiaryDark,
    tertiaryContainer = TrackTertiaryContainerDark,
    background = TrackSurfaceDark,
    surface = TrackSurfaceDark,
    surfaceContainer = TrackSurfaceContainerDark,
    surfaceContainerHigh = TrackSurfaceContainerHighDark,
    onSurface = TrackOnSurfaceDark,
    onSurfaceVariant = TrackOnSurfaceVariantDark,
    outline = TrackOutlineDark,
    outlineVariant = TrackOutlineVariantDark
)

@Composable
fun TrackMyTripTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = Typography,
        content = content
    )
}

package com.gamearena.booster.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryPurple,
    secondary = PrimaryAccent,
    background = BackgroundDark,
    surface = SurfaceDark,
    onPrimary = BackgroundLight,
    onSecondary = BackgroundLight,
    onBackground = BackgroundLight,
    onSurface = BackgroundLight,
    surfaceVariant = SurfaceLight
)

@Composable
fun GameArenaTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = AppTypography,
        content = content
    )
}

package com.rvetiveau.rvscorecount.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    background = BackgroundBase,
    surface = SurfaceRaised,
    surfaceVariant = SurfaceContainer,
    primary = Accent,
    onPrimary = OnAccent,
    error = Error,
    onBackground = InkPrimary,
    onSurface = InkPrimary,
    onSurfaceVariant = InkPrimary,
)

@Composable
fun RvScoreCounterTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}

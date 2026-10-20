package com.rvetiveau.rvscorecount.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

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
    outline = InkMuted,
)

private val LightColorScheme = lightColorScheme(
    background = LightBackground,
    surface = LightSurface,
    surfaceVariant = LightSurfaceContainer,
    primary = LightAccent,
    onPrimary = LightOnAccent,
    error = LightError,
    onBackground = LightInk,
    onSurface = LightInk,
    onSurfaceVariant = LightInk,
    outline = LightInkMuted,
)

private val LocalRvColors = staticCompositionLocalOf { DarkRvColors }

/** The app's own colours for the current theme, beside [MaterialTheme.colorScheme]. */
object RvTheme {
    val colors: RvColors
        @Composable
        @ReadOnlyComposable
        get() = LocalRvColors.current
}

@Composable
fun RvScoreCounterTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalRvColors provides if (darkTheme) DarkRvColors else LightRvColors) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
            typography = Typography,
            content = content
        )
    }
}

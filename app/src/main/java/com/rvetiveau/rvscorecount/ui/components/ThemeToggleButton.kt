package com.rvetiveau.rvscorecount.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.rvetiveau.rvscorecount.R

/** The theme shown and how to flip it, provided once at the app's root. */
data class ThemeSwitch(val isDark: Boolean, val onToggle: () -> Unit)

/** Null where no root provides it (previews, screen tests): the switch is then not drawn. */
val LocalThemeSwitch = staticCompositionLocalOf<ThemeSwitch?> { null }

/**
 * Light/dark switch for the top-right corner of every screen, tinted like the screen's other actions.
 * It shows the theme a tap leads to: a sun in the dark theme, a moon in the light one, turning and
 * fading into each other when tapped.
 */
@Composable
fun ThemeToggleButton(modifier: Modifier = Modifier) {
    val switch = LocalThemeSwitch.current ?: return
    val rotation by animateFloatAsState(
        // The moon stays upright; the sun, symmetric, takes the half turn.
        targetValue = if (switch.isDark) 180f else 0f,
        animationSpec = tween(durationMillis = 450),
        label = "theme_toggle_rotation"
    )
    IconButton(onClick = switch.onToggle, modifier = modifier.testTag("theme_toggle")) {
        AnimatedContent(
            targetState = switch.isDark,
            transitionSpec = {
                (fadeIn(tween(300)) + scaleIn(tween(300), initialScale = 0.5f)) togetherWith
                    (fadeOut(tween(150)) + scaleOut(tween(150), targetScale = 0.5f))
            },
            label = "theme_toggle_icon"
        ) { dark ->
            Icon(
                painter = painterResource(if (dark) R.drawable.ic_light_mode else R.drawable.ic_dark_mode),
                contentDescription = stringResource(if (dark) R.string.theme_switch_to_light else R.string.theme_switch_to_dark),
                tint = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier
                    .size(24.dp)
                    .rotate(rotation)
            )
        }
    }
}

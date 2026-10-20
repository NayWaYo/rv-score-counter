package com.rvetiveau.rvscorecount.ui.components

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.rvetiveau.rvscorecount.ui.theme.RvScoreCounterTheme
import org.junit.Rule
import org.junit.Test

/** Instrumented tests for the light/dark switch shown in every [ScreenHeader]. */
class ThemeToggleButtonTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun theSwitchFlipsTheThemeAndSaysWhatItWillDo() {
        var dark by mutableStateOf(true)
        composeTestRule.setContent {
            RvScoreCounterTheme(darkTheme = dark) {
                CompositionLocalProvider(LocalThemeSwitch provides ThemeSwitch(dark) { dark = !dark }) {
                    ScreenHeader(title = "Résultats finaux", titleTag = "title")
                }
            }
        }

        composeTestRule.onNodeWithContentDescription("Passer en mode clair").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithContentDescription("Passer en mode sombre").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithContentDescription("Passer en mode clair").assertExists()
    }

    @Test
    fun noSwitchIsDrawnWithoutARootProvidingIt() {
        composeTestRule.setContent {
            RvScoreCounterTheme {
                ScreenHeader(title = "Résultats finaux", titleTag = "title")
            }
        }

        composeTestRule.onNodeWithTag("theme_toggle").assertDoesNotExist()
    }
}

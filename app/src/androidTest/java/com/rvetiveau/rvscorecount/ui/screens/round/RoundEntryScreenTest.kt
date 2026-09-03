package com.rvetiveau.rvscorecount.ui.screens.round

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.rvetiveau.rvscorecount.domain.model.CardSuit
import com.rvetiveau.rvscorecount.ui.theme.RvScoreCounterTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Instrumented tests for the pure [RoundEntryScreen] (AC 2, 4-7, 10): layout and wiring only —
 * die-roll behavior itself is covered by [com.rvetiveau.rvscorecount.ui.components.DieRollerTest]. */
class RoundEntryScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun setContent(state: RoundEntryUiState, onRoll: () -> Unit = {}) {
        composeTestRule.setContent {
            RvScoreCounterTheme {
                RoundEntryScreen(state = state, onRoll = onRoll)
            }
        }
    }

    @Test
    fun showsTheTitleAndSuitLabelBeforeAnyRoll() {
        setContent(RoundEntryUiState(suit = null))

        composeTestRule.onNodeWithTag("round_entry_title").assertIsDisplayed()
        composeTestRule.onNodeWithTag("round_entry_suit_label").assertIsDisplayed()
        composeTestRule.onNodeWithTag("round_entry_die_roller").assertIsDisplayed()
    }

    @Test
    fun forwardsTheRollIntentFromTheDieRoller() {
        var rolled = false
        setContent(RoundEntryUiState(suit = null), onRoll = { rolled = true })

        composeTestRule.onNodeWithTag("die_roll_button").performClick()

        assertTrue(rolled)
    }

    @Test
    fun keepsShowingTheResultAfterARoll() {
        setContent(RoundEntryUiState(suit = CardSuit.DIAMOND))

        composeTestRule.onNodeWithTag("die_result").assertIsDisplayed()
    }
}

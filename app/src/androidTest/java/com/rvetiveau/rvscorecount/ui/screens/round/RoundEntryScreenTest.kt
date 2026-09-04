package com.rvetiveau.rvscorecount.ui.screens.round

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.runtime.mutableStateOf
import com.rvetiveau.rvscorecount.domain.model.CardSuit
import com.rvetiveau.rvscorecount.ui.theme.RvScoreCounterTheme
import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Instrumented tests for the pure [RoundEntryScreen] (AC 1-12): layout, wiring, loading/error
 * states and the die-roll lock/unlock cycle. Per-field keyboard/focus behavior is covered by
 * `ScoreFieldTest`; the die-roll itself by [com.rvetiveau.rvscorecount.ui.components.DieRollerTest]. */
class RoundEntryScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val aliceId = UUID.randomUUID()
    private val bobId = UUID.randomUUID()

    private fun aLoadedState(
        suit: CardSuit? = null,
        scores: Map<UUID, String> = mapOf(aliceId to "", bobId to "")
    ) = RoundEntryUiState(
        suit = suit,
        isLoading = false,
        loadError = false,
        content = RoundEntryContent(
            participants = listOf(
                RoundEntryParticipant(aliceId, "Alice"),
                RoundEntryParticipant(bobId, "Bob")
            ),
            scores = scores
        )
    )

    private fun setContent(
        state: RoundEntryUiState,
        onRoll: () -> Unit = {},
        onScoreChanged: (UUID, String) -> Unit = { _, _ -> },
        onRetry: () -> Unit = {}
    ) {
        composeTestRule.setContent {
            RvScoreCounterTheme {
                RoundEntryScreen(
                    state = state,
                    onRoll = onRoll,
                    onScoreChanged = onScoreChanged,
                    onRetry = onRetry
                )
            }
        }
    }

    @Test
    fun showsALoadingIndicatorBeforeContentIsReady() {
        setContent(RoundEntryUiState(isLoading = true))

        composeTestRule.onNodeWithTag("round_entry_loading").assertIsDisplayed()
        composeTestRule.onNodeWithTag("round_entry_die_roller").assertDoesNotExist()
        composeTestRule.onNodeWithTag("score_field_$aliceId").assertDoesNotExist()
    }

    @Test
    fun showsAnErrorWithRetryAndNoFieldsWhenLoadingFails() {
        var retried = false
        setContent(RoundEntryUiState(isLoading = false, loadError = true), onRetry = { retried = true })

        composeTestRule.onNodeWithTag("round_entry_load_error").assertIsDisplayed()
        composeTestRule.onNodeWithTag("round_entry_die_roller").assertDoesNotExist()
        composeTestRule.onNodeWithTag("score_field_$aliceId").assertDoesNotExist()
        composeTestRule.onNodeWithTag("round_entry_retry_button").performClick()
        assertTrue(retried)
    }

    @Test
    fun showsTheTitleSuitLabelAndDieRollerOnceContentIsReady() {
        setContent(aLoadedState())

        composeTestRule.onNodeWithTag("round_entry_title").assertIsDisplayed()
        composeTestRule.onNodeWithTag("round_entry_suit_label").assertIsDisplayed()
        composeTestRule.onNodeWithTag("round_entry_die_roller").assertIsDisplayed()
    }

    @Test
    fun showsOneFieldPerParticipantAndTheSumIndicator() {
        setContent(aLoadedState())

        composeTestRule.onNodeWithTag("score_field_$aliceId").assertIsDisplayed()
        composeTestRule.onNodeWithTag("score_field_$bobId").assertIsDisplayed()
        composeTestRule.onNodeWithTag("round_entry_sum_indicator").assertIsDisplayed()
        composeTestRule.onNodeWithText("Somme : 0").assertIsDisplayed()
    }

    @Test
    fun showsTheCurrentSumFromAllEnteredScores() {
        setContent(aLoadedState(scores = mapOf(aliceId to "80", bobId to "45")))

        composeTestRule.onNodeWithText("Somme : 125").assertIsDisplayed()
    }

    @Test
    fun forwardsIndependentScoreChangesForEachParticipant() {
        val changes = mutableListOf<Pair<UUID, String>>()
        setContent(aLoadedState(), onScoreChanged = { id, value -> changes.add(id to value) })

        composeTestRule.onNodeWithTag("score_field_$bobId").performTextInput("80")
        composeTestRule.onNodeWithTag("score_field_$aliceId").performTextInput("25")

        assertEquals(listOf(bobId to "80", aliceId to "25"), changes.filter { it.second.isNotEmpty() })
    }

    @Test
    fun forwardsTheRollIntentFromTheDieRoller() {
        var rolled = false
        setContent(aLoadedState(), onRoll = { rolled = true })

        composeTestRule.onNodeWithTag("die_roll_button").performClick()

        assertTrue(rolled)
    }

    @Test
    fun keepsShowingTheResultAfterARoll() {
        setContent(aLoadedState(suit = CardSuit.DIAMOND))

        composeTestRule.onNodeWithTag("die_result").assertIsDisplayed()
    }

    @Test
    fun disablesRerollAsSoonAsAnyScoreIsEntered() {
        setContent(aLoadedState(suit = CardSuit.DIAMOND, scores = mapOf(aliceId to "10", bobId to "")))

        composeTestRule.onNodeWithTag("die_reroll_button").assertIsNotEnabled()
    }

    @Test
    fun keepsRerollEnabledWhileAllScoresAreEmpty() {
        setContent(aLoadedState(suit = CardSuit.DIAMOND))

        composeTestRule.onNodeWithTag("die_reroll_button").assertIsEnabled()
    }

    @Test
    fun focusesTheFirstEmptyFieldAndMovesFocusWithImeActions() {
        setContent(aLoadedState())

        composeTestRule.onNodeWithTag("score_field_$aliceId").assertIsFocused().performImeAction()
        composeTestRule.onNodeWithTag("score_field_$bobId").assertIsFocused().performImeAction()
        composeTestRule.onNodeWithTag("score_field_$bobId").assertIsNotFocused()
    }

    @Test
    fun clearingAFieldAfterLoadDoesNotRestartAutomaticFocus() {
        val state = mutableStateOf(
            aLoadedState(scores = mapOf(aliceId to "10", bobId to "20"))
        )
        composeTestRule.setContent {
            RvScoreCounterTheme {
                RoundEntryScreen(
                    state = state.value,
                    onRoll = {},
                    onScoreChanged = { _, _ -> },
                    onRetry = {}
                )
            }
        }
        composeTestRule.onNodeWithTag("score_field_$aliceId").performClick().assertIsFocused()

        composeTestRule.runOnIdle {
            state.value = aLoadedState(scores = mapOf(aliceId to "10", bobId to ""))
        }

        composeTestRule.onNodeWithTag("score_field_$aliceId").assertIsFocused()
        composeTestRule.onNodeWithTag("score_field_$bobId").assertIsNotFocused()
    }
}

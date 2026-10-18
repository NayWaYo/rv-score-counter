package com.rvetiveau.rvscorecount.ui.screens.results

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.rvetiveau.rvscorecount.R
import com.rvetiveau.rvscorecount.ui.components.StandingRow
import com.rvetiveau.rvscorecount.ui.theme.RvScoreCounterTheme
import java.util.UUID
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Instrumented tests for the pure [ResultsScreen] (Story 5.1). */
class ResultsScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val bobId = UUID.randomUUID()
    private val aliceId = UUID.randomUUID()

    private val loaded = ResultsUiState.Loaded(
        listOf(
            StandingRow(bobId, rank = 1, name = "Bob", cumulativeScore = 120, isDealer = false),
            StandingRow(aliceId, rank = 2, name = "Alice", cumulativeScore = 380, isDealer = false)
        )
    )

    private fun setContent(state: ResultsUiState, onBackToHome: () -> Unit = {}, onRetry: () -> Unit = {}) {
        composeTestRule.setContent {
            RvScoreCounterTheme {
                ResultsScreen(state = state, onBackToHome = onBackToHome, onRetry = onRetry)
            }
        }
    }

    @Test
    fun highlightsTheWinnerAndListsTheFullFinalClassement() {
        setContent(loaded)

        composeTestRule.onNodeWithTag("results_title").assertTextEquals("Résultats finaux")
        composeTestRule.onNodeWithTag("results_winner")
            .assertTextContains("Vainqueur").assertTextContains("Bob").assertTextContains("120 points")
        composeTestRule.onNodeWithTag("results_standing_row_$aliceId")
            .assertTextContains("2.").assertTextContains("Alice").assertTextContains("380")
    }

    @Test
    fun replayAndBackToHomeEmitTheirIntents() {
        var replayed = false
        var backHome = false
        composeTestRule.setContent {
            RvScoreCounterTheme {
                ResultsScreen(state = loaded, onReplay = { replayed = true }, onBackToHome = { backHome = true })
            }
        }

        composeTestRule.onNodeWithTag("results_replay_button").assertTextEquals("Rejouer").assertIsEnabled().performClick()
        composeTestRule.onNodeWithTag("results_back_home_button")
            .assertTextEquals("Retour à l'accueil").assertIsEnabled().performClick()

        assertTrue(replayed)
        assertTrue(backHome)
    }

    @Test
    fun disablesReplayWhileCreatingAndShowsAReplayError() {
        setContent(loaded.copy(isReplaying = true, replayErrorRes = R.string.results_replay_error))

        composeTestRule.onNodeWithTag("results_replay_button").assertIsNotEnabled()
        composeTestRule.onNodeWithTag("results_replay_error").assertTextEquals("Impossible de relancer la partie. Réessayez.")
    }

    @Test
    fun namesEveryTiedWinner() {
        setContent(
            ResultsUiState.Loaded(
                listOf(
                    StandingRow(bobId, rank = 1, name = "Bob", cumulativeScore = 120, isDealer = false),
                    StandingRow(aliceId, rank = 1, name = "Alice", cumulativeScore = 120, isDealer = false)
                )
            )
        )

        composeTestRule.onNodeWithTag("results_winner").assertTextContains("Vainqueurs").assertTextContains("Bob, Alice")
    }

    @Test
    fun showsTheErrorStateWithRetry() {
        var retried = false
        setContent(ResultsUiState.LoadError, onRetry = { retried = true })

        composeTestRule.onNodeWithTag("results_load_error").assertTextEquals("Impossible de charger les résultats.")
        composeTestRule.onNodeWithText("Réessayer").performClick()

        assertTrue(retried)
    }
}

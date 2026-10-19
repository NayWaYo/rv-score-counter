package com.rvetiveau.rvscorecount.ui.screens.results

import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import com.rvetiveau.rvscorecount.R
import com.rvetiveau.rvscorecount.domain.model.CardSuit
import com.rvetiveau.rvscorecount.ui.components.RoundRecordEntryUi
import com.rvetiveau.rvscorecount.ui.components.RoundRecordUi
import com.rvetiveau.rvscorecount.ui.components.RoundTable
import com.rvetiveau.rvscorecount.ui.components.RoundTablePlayer
import com.rvetiveau.rvscorecount.ui.components.RoundTableRow
import com.rvetiveau.rvscorecount.ui.components.SevenChampionUi
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

    @Test
    fun showsTheStatisticCardsAndTheScoreSheet() {
        setContent(
            loaded.copy(
                sevenChampion = SevenChampionUi(listOf("Bob"), 2),
                roundRecord = RoundRecordUi(listOf(RoundRecordEntryUi("Alice", 1), RoundRecordEntryUi("Alice", 3)), 200),
                roundTable = RoundTable(
                    players = listOf(RoundTablePlayer(aliceId, "Alice"), RoundTablePlayer(bobId, "Bob")),
                    rows = listOf(
                        RoundTableRow(1, CardSuit.HEART, mapOf(aliceId to 200, bobId to 50), sevenHolderId = bobId),
                        RoundTableRow(2, CardSuit.SPADE, mapOf(aliceId to 180, bobId to 70), sevenHolderId = bobId)
                    )
                )
            )
        )

        composeTestRule.onNodeWithTag("results_standings").performScrollToNode(hasTestTag("results_stats"))
        composeTestRule.onNodeWithTag("stat_card_seven_champion").assertContentDescriptionEquals("Roi du 40 : Bob (2 fois)")
        composeTestRule.onNodeWithTag("stat_card_round_record")
            .assertContentDescriptionEquals("Record en une manche : Alice (manche 1), Alice (manche 3) — 200 points")
        composeTestRule.onNodeWithTag("results_standings").performScrollToNode(hasTestTag("results_round_table"))
        composeTestRule.onNodeWithTag("round_table_row_2")
            .assertContentDescriptionEquals("Manche 2, Pique : Alice 180 · Bob 70, total 250, Le 40 : Bob")
        composeTestRule.onNodeWithTag("round_table_total_$aliceId", useUnmergedTree = true).assertTextEquals("380")
        composeTestRule.onNodeWithTag("results_standings").performScrollToNode(hasTestTag("results_score_chart"))
        composeTestRule.onNodeWithTag("score_chart_plot")
            .assertContentDescriptionEquals("Évolution des scores sur 2 manches. Scores cumulés : Alice 380, Bob 120")
    }

    @Test
    fun hasNoScoreSheetWithoutRounds() {
        setContent(loaded)

        composeTestRule.onNodeWithTag("results_round_table").assertDoesNotExist()
        composeTestRule.onNodeWithTag("results_score_chart").assertDoesNotExist()
        composeTestRule.onNodeWithTag("results_stats").assertDoesNotExist()
    }
}

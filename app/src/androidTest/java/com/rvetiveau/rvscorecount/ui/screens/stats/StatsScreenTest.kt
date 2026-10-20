package com.rvetiveau.rvscorecount.ui.screens.stats

import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import com.rvetiveau.rvscorecount.domain.model.CardSuit
import com.rvetiveau.rvscorecount.domain.model.PlayerStats
import com.rvetiveau.rvscorecount.domain.model.RoundPoints
import com.rvetiveau.rvscorecount.ui.components.RoundRecordEntryUi
import com.rvetiveau.rvscorecount.ui.components.RoundRecordUi
import com.rvetiveau.rvscorecount.ui.components.RoundTable
import com.rvetiveau.rvscorecount.ui.components.RoundTablePlayer
import com.rvetiveau.rvscorecount.ui.components.RoundTableRow
import com.rvetiveau.rvscorecount.ui.components.SevenChampionUi
import com.rvetiveau.rvscorecount.ui.components.StandingRow
import com.rvetiveau.rvscorecount.ui.theme.RvScoreCounterTheme
import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Instrumented tests for the pure [StatsScreen]: the single place for a Partie's statistics. */
class StatsScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val aliceId = UUID.randomUUID()
    private val bobId = UUID.randomUUID()

    private val loaded = StatsUiState.Loaded(
        standings = listOf(
            StandingRow(aliceId, rank = 1, name = "Alice", cumulativeScore = 200, isDealer = false),
            StandingRow(bobId, rank = 2, name = "Bob", cumulativeScore = 300, isDealer = false)
        ),
        roundTable = RoundTable(
            players = listOf(RoundTablePlayer(aliceId, "Alice"), RoundTablePlayer(bobId, "Bob")),
            rows = listOf(
                RoundTableRow(1, CardSuit.HEART, mapOf(aliceId to 200, bobId to 50), sevenHolderId = bobId),
                RoundTableRow(2, CardSuit.SPADE, mapOf(aliceId to 0, bobId to 250), sevenHolderId = bobId)
            )
        ),
        sevenChampion = SevenChampionUi(listOf("Bob"), 2),
        roundRecord = RoundRecordUi(listOf(RoundRecordEntryUi("Bob", 2)), 250)
    )

    private fun setContent(state: StatsUiState, onRoundSelected: (Int) -> Unit = {}, onRetry: () -> Unit = {}) {
        composeTestRule.setContent {
            RvScoreCounterTheme {
                StatsScreen(state = state, onRoundSelected = onRoundSelected, onRetry = onRetry)
            }
        }
    }

    private fun scrollTo(tag: String) {
        composeTestRule.onNodeWithTag("stats_content").performScrollToNode(hasTestTag(tag))
    }

    @Test
    fun showsTheClassementTheStatCardsTheChartAndTheRounds() {
        setContent(loaded)

        composeTestRule.onNodeWithTag("stats_title").assertTextEquals("Statistiques")
        composeTestRule.onNodeWithTag("stats_standing_row_$aliceId").assertIsDisplayed()
        scrollTo("stats_cards")
        composeTestRule.onNodeWithTag("stat_card_seven_champion").assertContentDescriptionEquals("Roi du 40 : Bob (2 fois)")
        composeTestRule.onNodeWithTag("stat_card_round_record")
            .assertContentDescriptionEquals("Record en une manche : Bob (manche 2) — 250 points")
        scrollTo("stats_score_chart")
        composeTestRule.onNodeWithTag("score_chart_plot")
            .assertContentDescriptionEquals("Évolution des scores sur 2 manches. Scores cumulés : Alice 200, Bob 300")
        scrollTo("stats_round_table")
        composeTestRule.onNodeWithTag("round_table_row_1")
            .assertContentDescriptionEquals("Manche 1, Cœur : Alice 200 · Bob 50, total 250, Le 40 : Bob")
        val first = composeTestRule.onNodeWithTag("round_table_row_1").fetchSemanticsNode().boundsInRoot.top
        val second = composeTestRule.onNodeWithTag("round_table_row_2").fetchSemanticsNode().boundsInRoot.top
        assertTrue(first < second)
        composeTestRule.onNodeWithTag("round_table_total_$bobId", useUnmergedTree = true).assertTextEquals("300")
    }

    @Test
    fun aStandingRowUnfoldsThePlayersStatistics() {
        val stats = PlayerStats(
            roundsPlayed = 2,
            averagePoints = 150.0,
            zeroRounds = 0,
            sevensTaken = 2,
            best = RoundPoints(points = 50, roundNumber = 1),
            worst = RoundPoints(points = 250, roundNumber = 2)
        )
        setContent(loaded.copy(standings = loaded.standings.map { if (it.participantId == bobId) it.copy(stats = stats) else it }))

        composeTestRule.onNodeWithTag("stats_standing_row_$bobId").performClick()

        composeTestRule.onNodeWithText("Moyenne").assertIsDisplayed()
    }

    @Test
    fun theRoundsOfAFinishedPartyAreReadOnly() {
        setContent(loaded)

        scrollTo("stats_round_table")
        composeTestRule.onNodeWithTag("round_table_row_1").assertHasNoClickAction()
        composeTestRule.onNodeWithTag("stats_correction_hint").assertDoesNotExist()
    }

    @Test
    fun touchingARoundOfAPartyInProgressAsksForItsCorrection() {
        var selected: Int? = null
        setContent(loaded.copy(canCorrectRounds = true), onRoundSelected = { selected = it })

        scrollTo("stats_correction_hint")
        composeTestRule.onNodeWithTag("stats_correction_hint").assertTextEquals("Touchez une manche pour la corriger.")
        scrollTo("stats_round_table")
        composeTestRule.onNodeWithTag("round_table_row_2").assertHasClickAction().performClick()

        assertEquals(2, selected)
    }

    @Test
    fun aPartyWithoutRoundsSaysSo() {
        setContent(loaded.copy(roundTable = RoundTable(), sevenChampion = null, roundRecord = null))

        scrollTo("stats_no_round")
        composeTestRule.onNodeWithTag("stats_no_round").assertTextEquals("Aucune manche jouée")
        composeTestRule.onNodeWithTag("stats_cards").assertDoesNotExist()
        composeTestRule.onNodeWithTag("stats_score_chart").assertDoesNotExist()
        composeTestRule.onNodeWithTag("stats_round_table").assertDoesNotExist()
    }

    @Test
    fun showsTheErrorStateWithRetry() {
        var retried = false
        setContent(StatsUiState.LoadError, onRetry = { retried = true })

        composeTestRule.onNodeWithTag("stats_load_error").assertTextEquals("Impossible de charger les statistiques.")
        composeTestRule.onNodeWithText("Réessayer").performClick()

        assertTrue(retried)
    }
}

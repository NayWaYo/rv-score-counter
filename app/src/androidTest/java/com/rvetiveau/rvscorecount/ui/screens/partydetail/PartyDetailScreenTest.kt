package com.rvetiveau.rvscorecount.ui.screens.partydetail

import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.hasTestTag
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

/** Instrumented tests for the pure [PartyDetailScreen] (Story 6.2). */
class PartyDetailScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val aliceId = UUID.randomUUID()
    private val bobId = UUID.randomUUID()

    private val loaded = PartyDetailUiState.Loaded(
        gameId = "papayoo",
        createdAt = 0L,
        standings = listOf(
            StandingRow(aliceId, rank = 1, name = "Alice", cumulativeScore = 200, isDealer = false),
            StandingRow(bobId, rank = 2, name = "Bob", cumulativeScore = 300, isDealer = false)
        ),
        roundTable = RoundTable(
            players = listOf(RoundTablePlayer(aliceId, "Alice"), RoundTablePlayer(bobId, "Bob")),
            rows = listOf(
                RoundTableRow(1, CardSuit.HEART, mapOf(aliceId to 200, bobId to 50)),
                RoundTableRow(2, CardSuit.SPADE, mapOf(aliceId to 0, bobId to 250))
            )
        )
    )

    private fun setContent(state: PartyDetailUiState, onReplay: () -> Unit = {}) {
        composeTestRule.setContent {
            RvScoreCounterTheme {
                PartyDetailScreen(state = state, onReplay = onReplay)
            }
        }
    }

    @Test
    fun showsTheGameTheWinnerAndTheFinalClassement() {
        setContent(loaded)

        composeTestRule.onNodeWithTag("party_detail_subtitle").assertTextContains("Papayoo", substring = true)
        composeTestRule.onNodeWithTag("party_detail_winner").assertTextContains("Vainqueur").assertTextContains("Alice")
        composeTestRule.onNodeWithTag("party_detail_standing_row_$bobId").assertTextContains("2.").assertTextContains("300")
    }

    @Test
    fun showsTheRoundsAsAChronologicalScoreSheetWithTotals() {
        setContent(loaded)

        composeTestRule.onNodeWithTag("party_detail_content").performScrollToNode(hasTestTag("party_detail_round_table"))
        composeTestRule.onNodeWithTag("round_table_row_1")
            .assertContentDescriptionEquals("Manche 1, Cœur : Alice 200 · Bob 50, total 250")
            .assertHasNoClickAction()
        composeTestRule.onNodeWithTag("round_table_row_2")
            .assertContentDescriptionEquals("Manche 2, Pique : Alice 0 · Bob 250, total 250")
        val first = composeTestRule.onNodeWithTag("round_table_row_1").fetchSemanticsNode().boundsInRoot.top
        val second = composeTestRule.onNodeWithTag("round_table_row_2").fetchSemanticsNode().boundsInRoot.top
        assertTrue(first < second)
        composeTestRule.onNodeWithTag("round_table_total_$aliceId", useUnmergedTree = true).assertTextEquals("200")
        composeTestRule.onNodeWithTag("round_table_total_$bobId", useUnmergedTree = true).assertTextEquals("300")
    }

    @Test
    fun aPartyWithoutRoundsSaysSoAndHasNoWinner() {
        setContent(loaded.copy(roundTable = RoundTable()))

        composeTestRule.onNodeWithTag("party_detail_winner").assertDoesNotExist()
        composeTestRule.onNodeWithTag("party_detail_no_round").assertTextEquals("Aucune manche jouée")
    }

    @Test
    fun replayEmitsItsIntent() {
        var replayed = false
        setContent(loaded, onReplay = { replayed = true })

        composeTestRule.onNodeWithTag("party_detail_replay_button").assertTextEquals("Rejouer avec ces joueurs").performClick()

        assertTrue(replayed)
    }

    @Test
    fun showsTheReplayErrorAndDisablesReplayWhileCreating() {
        setContent(loaded.copy(isReplaying = true, replayErrorRes = R.string.results_replay_error))

        composeTestRule.onNodeWithTag("party_detail_replay_button").assertIsNotEnabled()
        composeTestRule.onNodeWithTag("party_detail_replay_error").assertTextEquals("Impossible de relancer la partie. Réessayez.")
    }

    @Test
    fun showsTheUnavailableState() {
        setContent(PartyDetailUiState.Unavailable)

        composeTestRule.onNodeWithTag("party_detail_unavailable").assertTextEquals("Détail indisponible pour cette partie.")
    }

    @Test
    fun showsTheStatisticCardsAndWhoTookThe40InEachRound() {
        setContent(
            loaded.copy(
                roundTable = loaded.roundTable.copy(rows = loaded.roundTable.rows.map { it.copy(sevenHolderId = bobId) }),
                sevenChampion = SevenChampionUi(listOf("Bob"), 2),
                roundRecord = RoundRecordUi(listOf(RoundRecordEntryUi("Bob", 2)), 250)
            )
        )

        composeTestRule.onNodeWithTag("party_detail_content").performScrollToNode(hasTestTag("party_detail_stats"))
        composeTestRule.onNodeWithTag("stat_card_seven_champion").assertContentDescriptionEquals("Roi du 40 : Bob (2 fois)")
        composeTestRule.onNodeWithTag("stat_card_round_record")
            .assertContentDescriptionEquals("Record en une manche : Bob (manche 2) — 250 points")
        composeTestRule.onNodeWithTag("party_detail_content").performScrollToNode(hasTestTag("party_detail_round_table"))
        composeTestRule.onNodeWithTag("round_table_row_1")
            .assertContentDescriptionEquals("Manche 1, Cœur : Alice 200 · Bob 50, total 250, Le 40 : Bob")
    }
}

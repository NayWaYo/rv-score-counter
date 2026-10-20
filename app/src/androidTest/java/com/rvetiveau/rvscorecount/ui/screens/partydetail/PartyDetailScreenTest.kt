package com.rvetiveau.rvscorecount.ui.screens.partydetail

import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.rvetiveau.rvscorecount.R
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
        roundCount = 2
    )

    private fun setContent(state: PartyDetailUiState, onReplay: () -> Unit = {}, onShowStats: () -> Unit = {}) {
        composeTestRule.setContent {
            RvScoreCounterTheme {
                PartyDetailScreen(state = state, onReplay = onReplay, onShowStats = onShowStats)
            }
        }
    }

    @Test
    fun showsTheGameTheWinnerAndTheFinalClassement() {
        setContent(loaded)

        composeTestRule.onNodeWithTag("party_detail_subtitle").assertTextContains("Papayoo", substring = true)
        composeTestRule.onNodeWithTag("party_detail_standing_row_$aliceId").assert(hasContentDescription("Vainqueur"))
        composeTestRule.onNodeWithTag("party_detail_standing_row_$bobId").assertTextContains("2.").assertTextContains("300")
    }

    @Test
    fun theStatsButtonShowsTheRoundCountAndEmitsItsIntent() {
        var opened = false
        setContent(loaded, onShowStats = { opened = true })

        composeTestRule.onNodeWithTag("party_detail_stats_button")
            .assertTextEquals("Statistiques (2)")
            .performClick()

        assertTrue(opened)
    }

    @Test
    fun aPartyWithoutRoundsSaysSoAndHasNoWinnerNorStats() {
        setContent(loaded.copy(roundCount = 0))

        composeTestRule.onNodeWithTag("party_detail_standing_row_$aliceId").assert(hasContentDescription("Vainqueur").not())
        composeTestRule.onNodeWithTag("party_detail_stats_button").assertDoesNotExist()
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
}

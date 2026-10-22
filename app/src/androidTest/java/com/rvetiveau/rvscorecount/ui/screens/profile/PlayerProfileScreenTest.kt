package com.rvetiveau.rvscorecount.ui.screens.profile

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.rvetiveau.rvscorecount.domain.model.BeloteRecords
import com.rvetiveau.rvscorecount.domain.model.PapayooRecords
import com.rvetiveau.rvscorecount.domain.model.PlayerRecords
import com.rvetiveau.rvscorecount.domain.model.PlayerSummary
import com.rvetiveau.rvscorecount.domain.model.TeammateRecord
import com.rvetiveau.rvscorecount.domain.model.WinRecord
import com.rvetiveau.rvscorecount.domain.model.WorstRound
import com.rvetiveau.rvscorecount.ui.theme.RvScoreCounterTheme
import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** What the pure [PlayerProfileScreen] shows (Story 10.3); the figures are in PlayerRecordsTest. */
class PlayerProfileScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val alice = PlayerSummary(UUID.randomUUID(), "Alice", finishedParties = 6, lastPlayedAt = 1_760_000_000_000)

    private val papayoo = PapayooRecords(
        wins = WinRecord(wins = 2, parties = 5),
        roundsPlayed = 40,
        sevensTaken = 6,
        sevensRecorded = 30,
        zeroRounds = 10,
        worstRound = WorstRound(160, UUID.randomUUID(), 1_760_000_000_000),
        fullRounds = 0,
        fullRoundPoints = 250
    )

    private fun setContent(state: PlayerProfileUiState, actions: ProfileActions = ProfileActions()) {
        composeTestRule.setContent { RvScoreCounterTheme { PlayerProfileScreen(state = state, actions = actions) } }
    }

    @Test
    fun thePapayooSectionLeadsWithTheWinRateThenTheChosenFigures() {
        setContent(PlayerProfileUiState.Loaded(PlayerRecords(alice, papayoo, belote = null)))

        composeTestRule.onNodeWithTag("profile_title").assertTextEquals("Alice")
        composeTestRule.onNodeWithTag("profile_papayoo_win_rate").assertTextEquals("40 %")
        composeTestRule.onNodeWithTag("profile_papayoo_wins").assertTextEquals("2 victoires sur 5 parties")
        composeTestRule.onNodeWithTag("profile_papayoo_sevens_value").assertTextEquals("6 · 20 % des manches")
        composeTestRule.onNodeWithTag("profile_papayoo_zeros_value").assertTextEquals("25 % (10)")
        composeTestRule.onNodeWithTag("profile_papayoo_full_value").assertTextEquals("0")
        composeTestRule.onNodeWithTag("profile_belote").assertDoesNotExist()
    }

    @Test
    fun theBeloteSectionShowsTakesAndBothTeammates() {
        val bob = TeammateRecord(UUID.randomUUID(), "Bob", WinRecord(3, 4))
        val chloe = TeammateRecord(UUID.randomUUID(), "Chloé", WinRecord(0, 1))
        val belote = BeloteRecords(WinRecord(3, 5), takes = 12, made = 9, bestTeammate = bob, worstTeammate = chloe)
        setContent(PlayerProfileUiState.Loaded(PlayerRecords(alice, papayoo = null, belote = belote)))

        composeTestRule.onNodeWithTag("profile_belote_win_rate").assertTextEquals("60 %")
        composeTestRule.onNodeWithTag("profile_belote_takes_value").assertTextEquals("2,4 par partie · 75 % de contrats remplis")
        composeTestRule.onNodeWithTag("profile_belote_best_teammate_value").performScrollTo()
            .assertTextEquals("Bob · 75 % de victoires (4 parties ensemble)")
        composeTestRule.onNodeWithTag("profile_belote_worst_teammate_value").performScrollTo()
            .assertTextEquals("Chloé · 0 % de victoires (1 partie ensemble)")
        composeTestRule.onNodeWithTag("profile_papayoo").assertDoesNotExist()
    }

    // --- Story 10.4 ---

    private val noParty = PlayerSummary(UUID.randomUUID(), "Alcie", finishedParties = 0, lastPlayedAt = null)

    @Test
    fun mergeIsOfferedWhenOthersExistAndDeleteOnlyWithoutAnyParty() {
        val calls = mutableListOf<String>()
        setContent(
            PlayerProfileUiState.Loaded(PlayerRecords(noParty, papayoo = null, belote = null), otherPlayers = listOf(alice)),
            ProfileActions(onMergeRequested = { calls += "merge" }, onDeleteRequested = { calls += "delete" })
        )

        composeTestRule.onNodeWithTag("profile_merge_button").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("profile_delete_button").performScrollTo().performClick()

        assertEquals(listOf("merge", "delete"), calls)
    }

    @Test
    fun aPlayerWithPartiesAndNoOtherPlayerHasNeitherAction() {
        setContent(PlayerProfileUiState.Loaded(PlayerRecords(alice, papayoo, belote = null)))

        composeTestRule.onNodeWithTag("profile_merge_button").assertDoesNotExist()
        composeTestRule.onNodeWithTag("profile_delete_button").assertDoesNotExist()
    }

    @Test
    fun theMergeConfirmationNamesBothPlayersAndRelaysTheChoice() {
        var confirmed = false
        setContent(
            PlayerProfileUiState.Loaded(
                PlayerRecords(alice, papayoo, belote = null),
                otherPlayers = listOf(noParty),
                dialog = ProfileDialog.ConfirmMerge(noParty, failed = true)
            ),
            ProfileActions(onMergeConfirmed = { confirmed = true })
        )

        composeTestRule.onNodeWithText("Fusionner Alcie dans Alice ?").assertIsDisplayed()
        composeTestRule.onNodeWithTag("profile_action_failed").assertIsDisplayed()
        composeTestRule.onNodeWithTag("profile_confirm").performClick()

        assertTrue(confirmed)
    }

    @Test
    fun aRefusedMergeNamesTheGameOfTheSharedParty() {
        setContent(
            PlayerProfileUiState.Loaded(
                PlayerRecords(alice, papayoo, belote = null),
                otherPlayers = listOf(noParty),
                dialog = ProfileDialog.MergeRefused(noParty, "papayoo", 1_760_000_000_000)
            )
        )

        val text = composeTestRule.onNodeWithTag("profile_merge_refused").fetchSemanticsNode()
            .config[SemanticsProperties.Text].single().text
        assertTrue(text, text.startsWith("Alice et Alcie ont joué ensemble la partie de Papayoo du "))
    }

    @Test
    fun aPlayerWithoutFinishedPartySaysSo() {
        setContent(PlayerProfileUiState.Loaded(PlayerRecords(alice.copy(finishedParties = 0), papayoo = null, belote = null)))

        composeTestRule.onNodeWithTag("profile_no_finished_party").assertIsDisplayed()
    }
}

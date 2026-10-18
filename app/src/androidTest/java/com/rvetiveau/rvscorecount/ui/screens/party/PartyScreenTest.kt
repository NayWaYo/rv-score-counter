package com.rvetiveau.rvscorecount.ui.screens.party

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import com.rvetiveau.rvscorecount.ui.theme.RvScoreCounterTheme
import java.util.UUID
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Instrumented tests for the pure [PartyScreen] (AC 5, 10): state rendering only —
 * Room loading and Donneur derivation are covered by [PartyViewModelTest]. */
class PartyScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun setContent(state: PartyUiState, onRetry: () -> Unit = {}, onStartRound: () -> Unit = {}) {
        composeTestRule.setContent {
            RvScoreCounterTheme {
                PartyScreen(state = state, onRetry = onRetry, onStartRound = onStartRound)
            }
        }
    }

    @Test
    fun showsTheDealerTitleAndEmptyStateWhenLoaded() {
        setContent(PartyUiState.Loaded(dealerName = "Alice"))

        composeTestRule.onNodeWithTag("party_dealer_title").assertTextEquals("Manche 1 — Donneur : Alice")
        composeTestRule.onNodeWithTag("party_empty_state").assertTextEquals("Lancez la première manche pour commencer.")
    }

    // --- Story 4.1 ---

    private val aliceId = UUID.randomUUID()
    private val bobId = UUID.randomUUID()

    private fun aLoadedStateWithStandings(roundNumber: Int = 3) = PartyUiState.Loaded(
        dealerName = "Bob",
        roundNumber = roundNumber,
        standings = listOf(
            StandingRow(bobId, rank = 1, name = "Bob", cumulativeScore = 40, isDealer = true),
            StandingRow(aliceId, rank = 2, name = "Alice", cumulativeScore = 210, isDealer = false)
        )
    )

    @Test
    fun showsEachStandingRowWithRankNameAndScoreInOrder() {
        setContent(aLoadedStateWithStandings())

        composeTestRule.onNodeWithTag("party_standing_row_$bobId")
            .assertTextContains("1.").assertTextContains("Bob").assertTextContains("40")
        composeTestRule.onNodeWithTag("party_standing_row_$aliceId")
            .assertTextContains("2.").assertTextContains("Alice").assertTextContains("210")
        val bobTop = composeTestRule.onNodeWithTag("party_standing_row_$bobId").fetchSemanticsNode().boundsInRoot.top
        val aliceTop = composeTestRule.onNodeWithTag("party_standing_row_$aliceId").fetchSemanticsNode().boundsInRoot.top
        assertTrue(bobTop < aliceTop)
    }

    @Test
    fun showsTheDealerBadgeOnlyOnTheDealerRowAndAnnouncesIt() {
        setContent(aLoadedStateWithStandings())

        composeTestRule.onAllNodesWithTag("party_dealer_badge", useUnmergedTree = true).assertCountEquals(1)
        composeTestRule.onNodeWithTag("party_standing_row_$bobId").assertTextContains("Donneur")
    }

    @Test
    fun showsTheRoundSubtitleAndHidesTheEmptyStateAfterTheFirstRound() {
        setContent(aLoadedStateWithStandings(roundNumber = 3))

        composeTestRule.onNodeWithTag("party_dealer_title").assertTextEquals("Manche 3 — Donneur : Bob")
        composeTestRule.onNodeWithTag("party_empty_state").assertDoesNotExist()
    }

    @Test
    fun showsBothBottomActionsWithEndOfPartyDisabledUntilClosureExists() {
        setContent(aLoadedStateWithStandings())

        composeTestRule.onNodeWithTag("party_start_round_button").assertIsEnabled()
        composeTestRule.onNodeWithTag("party_end_button").assertTextEquals("Fin de partie").assertIsNotEnabled()
    }

    @Test
    fun eightStandingRowsFitWithoutScrolling() {
        val rows = List(8) { StandingRow(UUID.randomUUID(), it + 1, "Joueur $it", it * 10, it == 0) }
        setContent(PartyUiState.Loaded(dealerName = "Joueur 0", roundNumber = 2, standings = rows))

        rows.forEach { composeTestRule.onNodeWithTag("party_standing_row_${it.participantId}").assertIsDisplayed() }
    }

    @Test
    fun startRoundButtonTriggersItsCallbackWhenLoaded() {
        var started = false
        setContent(PartyUiState.Loaded(dealerName = "Alice"), onStartRound = { started = true })

        composeTestRule.onNodeWithTag("party_start_round_button").performClick()

        assertTrue(started)
    }

    @Test
    fun startRoundButtonIsHiddenWhenThePartyIsFinished() {
        setContent(PartyUiState.Loaded(dealerName = "Alice", canStartRound = false))

        composeTestRule.onNodeWithTag("party_start_round_button").assertDoesNotExist()
    }

    @Test
    fun showsALoadingIndicatorWhileLoading() {
        setContent(PartyUiState.Loading)

        composeTestRule.onNodeWithTag("party_loading").assertExists()
    }

    @Test
    fun showsAnExplicitMessageWhenThePartyIsNotFound() {
        setContent(PartyUiState.PartyNotFound)

        composeTestRule.onNodeWithTag("party_not_found").assertExists()
    }

    @Test
    fun showsAnExplicitMessageWhenParticipantsAreInconsistent() {
        setContent(PartyUiState.InconsistentParticipants)

        composeTestRule.onNodeWithTag("party_inconsistent").assertExists()
    }

    @Test
    fun showsALoadErrorAndRetries() {
        var retried = false
        setContent(PartyUiState.LoadError, onRetry = { retried = true })

        composeTestRule.onNodeWithTag("party_load_error").assertExists()
        composeTestRule.onNodeWithText("Réessayer").performClick()

        assertTrue(retried)
    }

    @Test
    fun dealerNameIsNotTruncatedAtALargeFontScale() {
        val longName = "Bartholomew-Alexandre-Christophe"
        composeTestRule.setContent {
            val largeFontDensity = Density(density = LocalDensity.current.density, fontScale = 2f)
            CompositionLocalProvider(LocalDensity provides largeFontDensity) {
                RvScoreCounterTheme {
                    PartyScreen(state = PartyUiState.Loaded(dealerName = longName))
                }
            }
        }

        val textLayoutResults = mutableListOf<TextLayoutResult>()
        val getTextLayoutResult = composeTestRule
            .onNodeWithTag("party_dealer_title", useUnmergedTree = true)
            .fetchSemanticsNode()
            .config[SemanticsActions.GetTextLayoutResult]

        assertTrue(getTextLayoutResult.action?.invoke(textLayoutResults) == true)
        assertTrue(textLayoutResults.none { it.hasVisualOverflow })
    }
}

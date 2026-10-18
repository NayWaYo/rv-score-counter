package com.rvetiveau.rvscorecount.ui.screens.party

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasNoClickAction
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
import com.rvetiveau.rvscorecount.R
import com.rvetiveau.rvscorecount.domain.model.CardSuit
import com.rvetiveau.rvscorecount.ui.components.RoundHistoryItem
import com.rvetiveau.rvscorecount.ui.components.RoundHistoryScore
import com.rvetiveau.rvscorecount.ui.components.StandingRow
import com.rvetiveau.rvscorecount.ui.theme.RvScoreCounterTheme
import java.util.UUID
import org.junit.Assert.assertEquals
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

        composeTestRule.onAllNodesWithTag("dealer_badge", useUnmergedTree = true).assertCountEquals(1)
        composeTestRule.onNodeWithTag("party_standing_row_$bobId").assertTextContains("Donneur")
    }

    @Test
    fun showsTheRoundSubtitleAndHidesTheEmptyStateAfterTheFirstRound() {
        setContent(aLoadedStateWithStandings(roundNumber = 3))

        composeTestRule.onNodeWithTag("party_dealer_title").assertTextEquals("Manche 3 — Donneur : Bob")
        composeTestRule.onNodeWithTag("party_empty_state").assertDoesNotExist()
    }

    @Test
    fun showsBothBottomActionsAndEndOfPartyEmitsItsIntent() {
        var endClicked = false
        composeTestRule.setContent {
            RvScoreCounterTheme {
                PartyScreen(state = aLoadedStateWithStandings(), onEndPartyClick = { endClicked = true })
            }
        }

        composeTestRule.onNodeWithTag("party_start_round_button").assertIsEnabled()
        composeTestRule.onNodeWithTag("party_end_button").assertTextEquals("Fin de partie").performClick()

        assertTrue(endClicked)
    }

    // --- Story 5.1 ---

    @Test
    fun theEndConfirmationDialogAsksAndEmitsConfirmOrCancel() {
        var confirmed = false
        var dismissed = false
        composeTestRule.setContent {
            RvScoreCounterTheme {
                PartyScreen(
                    state = aLoadedStateWithStandings().copy(closure = PartyClosureUiState(isConfirmationOpen = true)),
                    onEndPartyConfirmed = { confirmed = true },
                    onEndPartyDismissed = { dismissed = true }
                )
            }
        }

        composeTestRule.onNodeWithText("Fin de partie ?").assertIsDisplayed()
        composeTestRule.onNodeWithText("Voulez-vous vraiment mettre fin à cette partie ?").assertIsDisplayed()
        composeTestRule.onNodeWithTag("party_end_confirm_button").assertTextEquals("Confirmer").performClick()
        composeTestRule.onNodeWithTag("party_end_cancel_button").performClick()

        assertTrue(confirmed)
        assertTrue(dismissed)
    }

    @Test
    fun showsTheClosureErrorUnderTheActions() {
        setContent(aLoadedStateWithStandings().copy(closure = PartyClosureUiState(errorRes = R.string.party_end_error)))

        composeTestRule.onNodeWithTag("party_end_error").assertTextEquals("Impossible de terminer la partie. Réessayez.")
    }

    @Test
    fun eightStandingRowsFitWithoutScrolling() {
        val rows = List(8) { StandingRow(UUID.randomUUID(), it + 1, "Joueur $it", it * 10, it == 0) }
        setContent(PartyUiState.Loaded(dealerName = "Joueur 0", roundNumber = 2, standings = rows))

        rows.forEach { composeTestRule.onNodeWithTag("party_standing_row_${it.participantId}").assertIsDisplayed() }
    }

    // --- Story 4.3 ---

    private val heartRound = RoundHistoryItem(
        roundNumber = 2,
        suit = CardSuit.HEART,
        scores = listOf(RoundHistoryScore("Bob", 40), RoundHistoryScore("Alice", 210)),
        total = 250
    )

    @Test
    fun hidesTheRoundsLinkBeforeTheFirstRound() {
        setContent(PartyUiState.Loaded(dealerName = "Alice"))

        composeTestRule.onNodeWithTag("party_rounds_link").assertDoesNotExist()
    }

    @Test
    fun theRoundsLinkShowsTheCountAndEmitsItsIntent() {
        var opened = false
        composeTestRule.setContent {
            RvScoreCounterTheme {
                PartyScreen(
                    state = aLoadedStateWithStandings().copy(roundHistory = listOf(heartRound)),
                    onShowRoundHistory = { opened = true }
                )
            }
        }

        composeTestRule.onNodeWithTag("party_rounds_link").assertTextEquals("Voir les manches (1)").performClick()

        assertTrue(opened)
    }

    @Test
    fun theOpenRoundHistoryReadsEachRowAsOneReadOnlyBlock() {
        setContent(aLoadedStateWithStandings().copy(roundHistory = listOf(heartRound), isRoundHistoryOpen = true))

        composeTestRule.onNodeWithTag("party_round_history_2")
            .assertContentDescriptionEquals("Manche 2, Cœur : Bob 40 · Alice 210, total 250")
            .assertHasNoClickAction()
    }

    // --- Story 4.2 ---

    @Test
    fun showsTheAddButtonBelowTheMaximumAndEmitsItsIntent() {
        var clicked = false
        composeTestRule.setContent {
            RvScoreCounterTheme {
                PartyScreen(
                    state = aLoadedStateWithStandings().copy(maxPlayers = 8),
                    onAddParticipantClick = { clicked = true }
                )
            }
        }

        composeTestRule.onNodeWithTag("party_add_participant_button").performClick()

        assertTrue(clicked)
    }

    @Test
    fun replacesTheAddButtonWithTheMaximumMessageAtCapacity() {
        val rows = List(8) { StandingRow(UUID.randomUUID(), it + 1, "Joueur $it", it, it == 0) }
        setContent(PartyUiState.Loaded(dealerName = "Joueur 0", standings = rows, maxPlayers = 8))

        composeTestRule.onNodeWithTag("party_add_participant_button").assertDoesNotExist()
        composeTestRule.onNodeWithTag("party_max_participants_reached").assertTextEquals("Maximum de 8 joueurs atteint")
    }

    @Test
    fun showsTheNameFieldWithItsErrorDuringTheNameStep() {
        setContent(
            aLoadedStateWithStandings().copy(
                maxPlayers = 8,
                addition = ParticipantAdditionUiState(nameInput = "", errorRes = R.string.party_add_error_empty_name)
            )
        )

        composeTestRule.onNodeWithTag("party_add_name_field").assertIsDisplayed()
        composeTestRule.onNodeWithTag("party_add_name_error").assertTextEquals("Veuillez entrer un prénom")
        composeTestRule.onNodeWithTag("party_add_participant_button").assertDoesNotExist()
    }

    @Test
    fun theSeatPickerListsOptionsFlagsTheDealerSeatAndEmitsTheChosenPosition() {
        var selected: Int? = null
        composeTestRule.setContent {
            RvScoreCounterTheme {
                PartyScreen(
                    state = aLoadedStateWithStandings().copy(
                        maxPlayers = 8,
                        addition = ParticipantAdditionUiState(
                            nameInput = "Zoé",
                            insertionOptions = listOf(
                                InsertionOption(position = 1, afterName = "Alice", becomesDealer = true),
                                InsertionOption(position = 2, afterName = "Bob", becomesDealer = false)
                            )
                        )
                    ),
                    onInsertionPositionSelected = { selected = it }
                )
            }
        }

        composeTestRule.onNodeWithTag("party_add_position_title").assertTextEquals("Où s'assoit Zoé ?")
        composeTestRule.onNodeWithTag("party_add_position_1").assertTextContains("Après Alice").assertTextContains("Sera le prochain donneur")
        composeTestRule.onNodeWithTag("party_add_position_2").performClick()

        assertEquals(2, selected)
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

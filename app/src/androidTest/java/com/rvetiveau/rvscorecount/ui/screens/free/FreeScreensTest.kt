package com.rvetiveau.rvscorecount.ui.screens.free

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.rvetiveau.rvscorecount.domain.model.EndRule
import com.rvetiveau.rvscorecount.domain.model.WinDirection
import com.rvetiveau.rvscorecount.ui.components.RoundTable
import com.rvetiveau.rvscorecount.ui.components.RoundTablePlayer
import com.rvetiveau.rvscorecount.ui.components.RoundTableRow
import com.rvetiveau.rvscorecount.ui.components.StandingRow
import com.rvetiveau.rvscorecount.ui.screens.party.PartyClosureUiState
import com.rvetiveau.rvscorecount.ui.screens.setup.ParticipantDraft
import com.rvetiveau.rvscorecount.ui.theme.RvScoreCounterTheme
import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** The creation, the Partie and the saisie of the Manches of the Jeu libre (Stories 11.2, 11.3), stateless. */
class FreeScreensTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val ready = FreeSetupUiState(
        gameNameInput = "Skyjo",
        knownGameNames = listOf("Skyjo", "Uno"),
        participants = listOf(ParticipantDraft(name = "Alice"), ParticipantDraft(name = "Bob")),
        knownPlayerNames = listOf("Alice", "Bob", "Chloé")
    )

    private fun setSetup(state: FreeSetupUiState, actions: FreeSetupActions = FreeSetupActions()) {
        composeTestRule.setContent {
            RvScoreCounterTheme { FreeSetupScreen(state = state, actions = actions, modifier = Modifier.fillMaxSize().testTag("root")) }
        }
    }

    @Test
    fun theRulesAreChosenBySegmentsAndStartRelaysWhenReady() {
        val calls = mutableListOf<Any>()
        setSetup(
            ready.copy(endMode = FreeEndMode.TARGET_SCORE),
            FreeSetupActions(
                onWinDirectionChanged = { calls += it },
                onEndModeChanged = { calls += it },
                onStartParty = { calls += "start" }
            )
        )

        composeTestRule.onNodeWithTag("free_setup_win_LOWEST_WINS").assertIsSelected()
        composeTestRule.onNodeWithTag("free_setup_end_value").assertTextEquals("100")
        composeTestRule.onNodeWithTag("free_setup_win_HIGHEST_WINS").performClick()
        composeTestRule.onNodeWithTag("free_setup_end_mode_ROUND_COUNT").performClick()
        composeTestRule.onNodeWithTag("free_setup_start").assertIsEnabled().performClick()

        assertEquals(listOf(WinDirection.HIGHEST_WINS, FreeEndMode.ROUND_COUNT, "start"), calls)
    }

    @Test
    fun startStaysOffWithoutAGameName() {
        setSetup(ready.copy(gameNameInput = " "))

        composeTestRule.onNodeWithTag("free_setup_start").assertIsNotEnabled()
        composeTestRule.onNodeWithTag("free_setup_end_value").assertDoesNotExist()
    }

    @Test
    fun knownGamesArePickedByChip() {
        val picked = mutableListOf<String>()
        setSetup(ready.copy(gameNameInput = "u"), FreeSetupActions(onGameNameSuggestionSelected = { picked += it }))

        composeTestRule.onNodeWithTag("free_setup_game_suggestion_Uno").performClick()

        assertEquals(listOf("Uno"), picked)
    }

    private val aliceId = UUID.randomUUID()
    private val bobId = UUID.randomUUID()

    private val inProgress = FreePartyUiState.Loaded(
        gameName = "Skyjo",
        winDirection = WinDirection.LOWEST_WINS,
        endRule = EndRule.TargetScore(100),
        isFinished = false,
        standings = listOf(
            StandingRow(bobId, 1, "Bob", -5, isDealer = false),
            StandingRow(aliceId, 2, "Alice", 40, isDealer = true)
        ),
        winnerNames = emptyList(),
        progress = FreeProgress.Target(100, 60, listOf("Alice")),
        roundTable = RoundTable(
            players = listOf(RoundTablePlayer(aliceId, "Alice"), RoundTablePlayer(bobId, "Bob")),
            rows = listOf(RoundTableRow(1, suit = null, points = mapOf(aliceId to 40, bobId to -5)))
        ),
        nextRoundNumber = 2
    )

    private fun setParty(state: FreePartyUiState, actions: FreePartyActions = FreePartyActions()) {
        composeTestRule.setContent {
            RvScoreCounterTheme { FreePartyScreen(state = state, actions = actions, modifier = Modifier.fillMaxSize().testTag("root")) }
        }
    }

    @Test
    fun thePartyShowsItsGameItsRulesItsProgressItsClassementAndItsManches() {
        val calls = mutableListOf<Any>()
        setParty(inProgress, FreePartyActions(onPlayRound = { calls += "play" }, onRoundSelected = { calls += it }))

        composeTestRule.onNodeWithTag("free_party_title").assertTextEquals("Skyjo")
        composeTestRule.onNodeWithTag("free_party_rules").assertTextEquals("Le plus petit total gagne · Fin à 100 points")
        composeTestRule.onNodeWithTag("free_party_progress").assertTextEquals("Alice : encore 60 points avant 100")
        composeTestRule.onNodeWithTag("dealer_badge", useUnmergedTree = true).assertExists()
        composeTestRule.onNodeWithTag("round_table_total_$bobId", useUnmergedTree = true).assertTextEquals("-5")
        composeTestRule.onNodeWithTag("round_table_row_1").performClick()
        composeTestRule.onNodeWithTag("free_party_play_round").assertTextEquals("Manche 2").performClick()

        assertEquals(listOf<Any>(1, "play"), calls)
    }

    @Test
    fun finDePartieAsksForAConfirmation() {
        val calls = mutableListOf<String>()
        setParty(
            inProgress.copy(closure = PartyClosureUiState(isConfirmationOpen = true)),
            FreePartyActions(onEndPartyConfirmed = { calls += "confirm" })
        )

        composeTestRule.onNodeWithText("Fin de partie ?").assertExists()
        composeTestRule.onNodeWithTag("party_end_confirm_button").performClick()

        assertEquals(listOf("confirm"), calls)
    }

    @Test
    fun aFinishedPartyShowsItsWinnersReadOnly() {
        val calls = mutableListOf<Any>()
        setParty(
            inProgress.copy(
                isFinished = true,
                standings = inProgress.standings.map { it.copy(isDealer = false) },
                winnerNames = listOf("Bob"),
                progress = null
            ),
            FreePartyActions(onRoundSelected = { calls += it }, onBackHome = { calls += "home" })
        )

        composeTestRule.onNodeWithTag("free_party_winners").assertTextEquals("Vainqueur : Bob")
        composeTestRule.onNodeWithTag("winner_trophy", useUnmergedTree = true).assertExists()
        composeTestRule.onNodeWithTag("free_party_play_round").assertDoesNotExist()
        composeTestRule.onNodeWithTag("free_party_end").assertDoesNotExist()
        composeTestRule.onNodeWithTag("round_table_row_1").performClick()
        composeTestRule.onNodeWithTag("free_party_back_home").performClick()

        assertEquals(listOf<Any>("home"), calls)
    }

    @Test
    fun theSaisieTakesNegativeScoresThroughTheSignButton() {
        val calls = mutableListOf<Any>()
        val fields = listOf(FreeScoreInput(aliceId, "Alice", "-3"), FreeScoreInput(bobId, "Bob"))
        composeTestRule.setContent {
            RvScoreCounterTheme {
                FreeRoundScreen(
                    state = FreeRoundUiState.Loaded(roundNumber = 2, isCorrection = false, fields = fields),
                    onScoreChanged = { id, text -> calls += id to text },
                    onToggleSign = { calls += it },
                    modifier = Modifier.fillMaxSize().testTag("root")
                )
            }
        }

        composeTestRule.onNodeWithTag("free_round_title").assertTextEquals("Manche 2")
        composeTestRule.onNodeWithTag("score_field_$aliceId").assertTextEquals("-3")
        composeTestRule.onNodeWithTag("free_round_validate").assertIsNotEnabled()
        composeTestRule.onNodeWithTag("score_field_sign_$bobId").performClick()
        composeTestRule.onNodeWithTag("score_field_$bobId").performTextInput("-7")

        assertEquals(listOf<Any>(bobId, bobId to "-7"), calls)
    }

    @Test
    fun aFilledCorrectionCanBeSaved() {
        var saved = false
        val fields = listOf(FreeScoreInput(aliceId, "Alice", "4"), FreeScoreInput(bobId, "Bob", "0"))
        composeTestRule.setContent {
            RvScoreCounterTheme {
                FreeRoundScreen(
                    state = FreeRoundUiState.Loaded(roundNumber = 1, isCorrection = true, fields = fields),
                    onValidate = { saved = true }
                )
            }
        }

        composeTestRule.onNodeWithTag("free_round_title").assertTextEquals("Corriger la manche 1")
        composeTestRule.onNodeWithTag("free_round_validate").assertIsEnabled().performClick()

        assertTrue(saved)
    }
}

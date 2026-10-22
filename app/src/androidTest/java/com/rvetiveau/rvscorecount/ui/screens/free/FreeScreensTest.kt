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
import androidx.compose.ui.test.performClick
import com.rvetiveau.rvscorecount.domain.model.EndRule
import com.rvetiveau.rvscorecount.domain.model.Participant
import com.rvetiveau.rvscorecount.domain.model.WinDirection
import com.rvetiveau.rvscorecount.ui.screens.setup.ParticipantDraft
import com.rvetiveau.rvscorecount.ui.theme.RvScoreCounterTheme
import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/** The creation and the first party screen of the Jeu libre (Story 11.2), stateless. */
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

    @Test
    fun thePartyShowsItsGameItsRulesAndItsTableWithTheDealer() {
        val partyId = UUID.randomUUID()
        val alice = Participant(UUID.randomUUID(), partyId, "Alice", 0)
        val bob = Participant(UUID.randomUUID(), partyId, "Bob", 1)
        composeTestRule.setContent {
            RvScoreCounterTheme {
                FreePartyScreen(
                    FreePartyUiState.Loaded("Skyjo", WinDirection.LOWEST_WINS, EndRule.TargetScore(100), listOf(alice, bob), dealerId = alice.id),
                    modifier = Modifier.fillMaxSize().testTag("root")
                )
            }
        }

        composeTestRule.onNodeWithTag("free_party_title").assertTextEquals("Skyjo")
        composeTestRule.onNodeWithTag("free_party_rules").assertTextEquals("Le plus petit total gagne · Fin à 100 points")
        composeTestRule.onNodeWithTag("free_party_dealer").assertTextEquals("Donneur M1")
    }
}

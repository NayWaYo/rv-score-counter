package com.rvetiveau.rvscorecount.ui.screens.party

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import com.rvetiveau.rvscorecount.ui.theme.RvScoreCounterTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Instrumented tests for the pure [PartyScreen] (AC 5, 10): state rendering only —
 * Room loading and Donneur derivation are covered by [PartyViewModelTest]. */
class PartyScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun setContent(state: PartyUiState, onRetry: () -> Unit = {}) {
        composeTestRule.setContent {
            RvScoreCounterTheme {
                PartyScreen(state = state, onRetry = onRetry)
            }
        }
    }

    @Test
    fun showsTheDealerTitleAndEmptyStateWhenLoaded() {
        setContent(PartyUiState.Loaded(dealerName = "Alice"))

        composeTestRule.onNodeWithTag("party_dealer_title").assertTextEquals("Manche 1 — Donneur : Alice")
        composeTestRule.onNodeWithTag("party_empty_state").assertTextEquals("Lancez la première manche pour commencer.")
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

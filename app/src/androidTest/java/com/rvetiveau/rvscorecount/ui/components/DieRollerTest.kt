package com.rvetiveau.rvscorecount.ui.components

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import com.rvetiveau.rvscorecount.domain.model.CardSuit
import com.rvetiveau.rvscorecount.ui.theme.RvScoreCounterTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Instrumented tests for the pure [DieRoller] (AC 2-4, 6, 10) and its roll animation: no [kotlin.random.Random] or
 * ViewModel involved — state rendering and callbacks only. */
class DieRollerTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun setContent(suit: CardSuit?, onRoll: () -> Unit = {}, rerollEnabled: Boolean = true) {
        composeTestRule.setContent {
            RvScoreCounterTheme {
                DieRoller(suit = suit, onRoll = onRoll, rerollEnabled = rerollEnabled)
            }
        }
    }

    @Test
    fun showsTheRollButtonBeforeAnyResult() {
        setContent(suit = null)

        composeTestRule.onNodeWithTag("die_roll_button").assertIsDisplayed()
        composeTestRule.onNodeWithText("Lancer").assertIsDisplayed()
    }

    @Test
    fun rollButtonTriggersItsCallback() {
        var rolled = false
        setContent(suit = null, onRoll = { rolled = true })

        composeTestRule.onNodeWithTag("die_roll_button").performClick()

        assertTrue(rolled)
    }

    @Test
    fun showsThePictogramAndFrenchNameForSpade() {
        setContent(suit = CardSuit.SPADE)

        composeTestRule.onNodeWithText("♠", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("Pique", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun showsThePictogramAndFrenchNameForHeart() {
        setContent(suit = CardSuit.HEART)

        composeTestRule.onNodeWithText("♥", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("Cœur", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun showsThePictogramAndFrenchNameForClub() {
        setContent(suit = CardSuit.CLUB)

        composeTestRule.onNodeWithText("♣", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("Trèfle", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun showsThePictogramAndFrenchNameForDiamond() {
        setContent(suit = CardSuit.DIAMOND)

        composeTestRule.onNodeWithText("♦", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("Carreau", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun announcesTheResultForAccessibility() {
        setContent(suit = CardSuit.HEART)

        composeTestRule.onNodeWithTag("die_result").assertContentDescriptionEquals("Couleur du 7 : Cœur")
        composeTestRule.onNodeWithText("Cœur").assertDoesNotExist()
    }

    @Test
    fun rerollButtonTriggersItsCallback() {
        var rolled = false
        setContent(suit = CardSuit.SPADE, onRoll = { rolled = true })

        composeTestRule.onNodeWithTag("die_reroll_button").performClick()

        assertTrue(rolled)
    }

    @Test
    fun rerollButtonIsDisabledWhenRerollEnabledIsFalse() {
        setContent(suit = CardSuit.SPADE, rerollEnabled = false)

        composeTestRule.onNodeWithTag("die_reroll_button").assertIsNotEnabled()
    }

    @Test
    fun suitNameIsNotTruncatedAtALargeFontScale() {
        composeTestRule.setContent {
            val largeFontDensity = Density(density = LocalDensity.current.density, fontScale = 2f)
            CompositionLocalProvider(LocalDensity provides largeFontDensity) {
                RvScoreCounterTheme {
                    DieRoller(suit = CardSuit.CLUB, onRoll = {})
                }
            }
        }

        val textLayoutResults = mutableListOf<TextLayoutResult>()
        val getTextLayoutResult = composeTestRule
            .onNodeWithTag("die_result_name", useUnmergedTree = true)
            .fetchSemanticsNode()
            .config[SemanticsActions.GetTextLayoutResult]

        assertTrue(getTextLayoutResult.action?.invoke(textLayoutResults) == true)
        assertTrue(textLayoutResults.none { it.hasVisualOverflow })
    }

    @Test
    fun playsARollAnimationBeforeRevealingTheDrawnSuit() {
        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.setContent {
            var suit by remember { mutableStateOf<CardSuit?>(null) }
            RvScoreCounterTheme {
                DieRoller(suit = suit, onRoll = { suit = CardSuit.HEART })
            }
        }

        composeTestRule.onNodeWithTag("die_roll_button").performClick()
        composeTestRule.mainClock.advanceTimeByFrame()

        composeTestRule.onNodeWithTag("die_rolling")
            .assertContentDescriptionEquals("Lancer du dé en cours")
        composeTestRule.onNodeWithTag("die_result").assertDoesNotExist()
        composeTestRule.onNodeWithTag("die_reroll_button").assertIsNotEnabled()

        composeTestRule.mainClock.advanceTimeBy(DIE_ROLL_ANIMATION_MILLIS + 100L)

        composeTestRule.onNodeWithTag("die_rolling").assertDoesNotExist()
        composeTestRule.onNodeWithTag("die_result").assertContentDescriptionEquals("Couleur du 7 : Cœur")
        composeTestRule.onNodeWithTag("die_reroll_button").assertIsEnabled()
    }

    @Test
    fun replaysTheAnimationOnReroll() {
        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.setContent {
            var suit by remember { mutableStateOf(CardSuit.SPADE) }
            RvScoreCounterTheme {
                DieRoller(suit = suit, onRoll = { suit = CardSuit.CLUB })
            }
        }
        composeTestRule.mainClock.advanceTimeByFrame()
        composeTestRule.onNodeWithTag("die_result").assertContentDescriptionEquals("Couleur du 7 : Pique")

        composeTestRule.onNodeWithTag("die_reroll_button").performClick()
        composeTestRule.mainClock.advanceTimeByFrame()

        composeTestRule.onNodeWithTag("die_rolling").assertExists()

        composeTestRule.mainClock.advanceTimeBy(DIE_ROLL_ANIMATION_MILLIS + 100L)

        composeTestRule.onNodeWithTag("die_result").assertContentDescriptionEquals("Couleur du 7 : Trèfle")
    }
}

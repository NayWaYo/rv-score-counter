package com.rvetiveau.rvscorecount.ui.components

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.click
import androidx.compose.ui.test.centerRight
import com.rvetiveau.rvscorecount.domain.model.CardSuit
import com.rvetiveau.rvscorecount.ui.theme.RvScoreCounterTheme
import java.util.UUID
import org.junit.Rule
import org.junit.Test

/** Instrumented tests for [ScoreChart]: summary for TalkBack, legend, and the touched Manche. */
class ScoreChartTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val alice = RoundTablePlayer(UUID.randomUUID(), "Alice")
    private val bob = RoundTablePlayer(UUID.randomUUID(), "Bob")

    private val table = RoundTable(
        players = listOf(alice, bob),
        rows = listOf(
            RoundTableRow(1, CardSuit.HEART, mapOf(alice.participantId to 200, bob.participantId to 50)),
            RoundTableRow(2, CardSuit.SPADE, mapOf(alice.participantId to 0, bob.participantId to 250))
        )
    )

    private fun setContent() {
        composeTestRule.setContent {
            RvScoreCounterTheme {
                ScoreChart(table)
            }
        }
    }

    @Test
    fun summarisesTheFinalScoresForTalkBackAndShowsTheLegend() {
        setContent()

        composeTestRule.onNodeWithTag("score_chart_plot")
            .assertContentDescriptionEquals("Évolution des scores sur 2 manches. Scores cumulés : Alice 200, Bob 300")
        composeTestRule.onNodeWithText("Alice 200").assertIsDisplayed()
        composeTestRule.onNodeWithText("Bob 300").assertIsDisplayed()
    }

    @Test
    fun touchingTheChartSelectsTheNearestRound() {
        setContent()

        composeTestRule.onNodeWithTag("score_chart_plot").assert(
            SemanticsMatcher.keyNotDefined(SemanticsProperties.StateDescription)
        )
        composeTestRule.onNodeWithTag("score_chart_plot").performTouchInput { click(centerRight) }

        composeTestRule.onNodeWithTag("score_chart_plot").assert(
            SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Manche 2 : Alice 200, Bob 300")
        )
    }
}

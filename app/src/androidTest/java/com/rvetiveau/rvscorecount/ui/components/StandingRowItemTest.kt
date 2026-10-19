package com.rvetiveau.rvscorecount.ui.components

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.Modifier
import com.rvetiveau.rvscorecount.domain.model.PlayerStats
import com.rvetiveau.rvscorecount.domain.model.RoundPoints
import com.rvetiveau.rvscorecount.ui.theme.RvScoreCounterTheme
import java.util.UUID
import org.junit.Rule
import org.junit.Test

/** Instrumented tests for [StandingRowItem]: the per-Participant statistics unfold on a tap. */
class StandingRowItemTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val id = UUID.randomUUID()
    private val stats = PlayerStats(
        roundsPlayed = 2,
        averagePoints = 62.5,
        zeroRounds = 1,
        sevensTaken = 1,
        best = RoundPoints(0, 2),
        worst = RoundPoints(125, 1)
    )

    private fun setContent(row: StandingRow) {
        composeTestRule.setContent {
            RvScoreCounterTheme {
                StandingRowItem(row, modifier = Modifier.testTag("row"))
            }
        }
    }

    @Test
    fun aTapUnfoldsAndFoldsTheParticipantStatistics() {
        setContent(StandingRow(id, rank = 1, name = "Alice", cumulativeScore = 125, isDealer = false, stats = stats))

        composeTestRule.onNodeWithTag("standing_stats_$id").assertDoesNotExist()
        composeTestRule.onNodeWithTag("row")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Statistiques masquées"))
            .performClick()

        composeTestRule.onNodeWithTag("standing_stats_$id", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithTag("row")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Statistiques affichées"))
            .assertTextContains("Moyenne")
            // The decimal separator follows the device locale (62,5 on a French phone).
            .assertTextContains("62", substring = true)
            .assertTextContains("Manches à 0")
            .assertTextContains("40 pris")
            .assertTextContains("0 pts · manche 2")
            .assertTextContains("125 pts · manche 1")
            .performClick()

        composeTestRule.onNodeWithTag("standing_stats_$id").assertDoesNotExist()
    }

    @Test
    fun hidesTheSevensWhenTheyAreNotTracked() {
        setContent(StandingRow(id, rank = 1, name = "Alice", cumulativeScore = 125, isDealer = false, stats = stats.copy(sevensTaken = null)))

        composeTestRule.onNodeWithTag("row").performClick()

        composeTestRule.onNodeWithTag("row").assert(
            SemanticsMatcher("has no 40 pris") { node ->
                node.config.getOrElse(SemanticsProperties.Text) { emptyList() }.none { it.text == "40 pris" }
            }
        )
    }

    @Test
    fun aRowWithoutStatisticsIsNotClickable() {
        setContent(StandingRow(id, rank = 2, name = "Zoé", cumulativeScore = 0, isDealer = false))

        composeTestRule.onNodeWithTag("row").assertHasNoClickAction().assertTextContains("Zoé")
    }

    @Test
    fun aRowWithStatisticsIsClickable() {
        setContent(StandingRow(id, rank = 1, name = "Alice", cumulativeScore = 125, isDealer = false, stats = stats))

        composeTestRule.onNodeWithTag("row").assertHasClickAction()
    }
}

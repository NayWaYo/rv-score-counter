package com.rvetiveau.rvscorecount.ui.screens.round

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.unit.height
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import com.rvetiveau.rvscorecount.domain.model.CardSuit
import com.rvetiveau.rvscorecount.ui.components.StandingRow
import com.rvetiveau.rvscorecount.ui.theme.RvScoreCounterTheme
import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Instrumented tests for the pure [RoundEntryScreen] (AC 1-12): layout, wiring, loading/error
 * states and the die-roll lock/unlock cycle. Per-field keyboard/focus behavior is covered by
 * `ScoreFieldTest`; the die-roll itself by [com.rvetiveau.rvscorecount.ui.components.DieRollerTest]. */
class RoundEntryScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val aliceId = UUID.randomUUID()
    private val bobId = UUID.randomUUID()

    private fun aLoadedState(
        suit: CardSuit? = null,
        scores: Map<UUID, String> = mapOf(aliceId to "", bobId to ""),
        suggestion: ScoreSuggestion? = null,
        panel: RoundEntryPanel = RoundEntryPanel.SCORES
    ) = RoundEntryUiState(
        suit = suit,
        isLoading = false,
        loadError = false,
        panel = panel,
        content = RoundEntryContent(
            participants = listOf(
                RoundEntryParticipant(aliceId, "Alice"),
                RoundEntryParticipant(bobId, "Bob")
            ),
            scores = scores,
            totalPointsPerRound = 250,
            suggestion = suggestion
        )
    )

    private fun setContent(
        state: RoundEntryUiState,
        onRoll: () -> Unit = {},
        onScoreChanged: (UUID, String) -> Unit = { _, _ -> },
        onRetry: () -> Unit = {},
        onValidate: () -> Unit = {}
    ) {
        composeTestRule.setContent {
            RvScoreCounterTheme {
                RoundEntryScreen(
                    state = state,
                    onRoll = onRoll,
                    onScoreChanged = onScoreChanged,
                    onRetry = onRetry,
                    onValidate = onValidate
                )
            }
        }
    }

    @Test
    fun showsALoadingIndicatorBeforeContentIsReady() {
        setContent(RoundEntryUiState(isLoading = true))

        composeTestRule.onNodeWithTag("round_entry_loading").assertIsDisplayed()
        composeTestRule.onNodeWithTag("round_entry_die_roller").assertDoesNotExist()
        composeTestRule.onNodeWithTag("score_field_$aliceId").assertDoesNotExist()
    }

    @Test
    fun showsAnErrorWithRetryAndNoFieldsWhenLoadingFails() {
        var retried = false
        setContent(RoundEntryUiState(isLoading = false, loadError = true), onRetry = { retried = true })

        composeTestRule.onNodeWithTag("round_entry_load_error").assertIsDisplayed()
        composeTestRule.onNodeWithTag("round_entry_die_roller").assertDoesNotExist()
        composeTestRule.onNodeWithTag("score_field_$aliceId").assertDoesNotExist()
        composeTestRule.onNodeWithTag("round_entry_retry_button").performClick()
        assertTrue(retried)
    }

    @Test
    fun showsTheTitleSuitLabelAndDieRollerOnceContentIsReady() {
        setContent(aLoadedState())

        composeTestRule.onNodeWithTag("round_entry_title").assertIsDisplayed()
        composeTestRule.onNodeWithTag("round_entry_suit_label").assertIsDisplayed()
        composeTestRule.onNodeWithTag("round_entry_die_roller").assertIsDisplayed()
    }

    @Test
    fun showsOneFieldPerParticipantAndTheSumIndicator() {
        setContent(aLoadedState())

        composeTestRule.onNodeWithTag("score_field_$aliceId").assertIsDisplayed()
        composeTestRule.onNodeWithTag("score_field_$bobId").assertIsDisplayed()
        composeTestRule.onNodeWithTag("round_entry_sum_indicator").assertIsDisplayed()
        composeTestRule.onNodeWithText("Somme : 0").assertIsDisplayed()
    }

    @Test
    fun showsTheCurrentSumFromAllEnteredScores() {
        setContent(aLoadedState(scores = mapOf(aliceId to "80", bobId to "45")))

        composeTestRule.onNodeWithText("Somme : 125").assertIsDisplayed()
    }

    @Test
    fun showsTheSuggestionOnlyOnItsParticipantAndCountsItInTheSum() {
        setContent(
            aLoadedState(
                scores = mapOf(aliceId to "100", bobId to ""),
                suggestion = ScoreSuggestion(bobId, 150)
            )
        )

        composeTestRule.onNodeWithTag("score_field_suggestion_$bobId", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithTag("score_field_suggestion_$aliceId", useUnmergedTree = true).assertDoesNotExist()
        composeTestRule.onNodeWithText("Somme : 250", substring = true).assertIsDisplayed()
    }

    @Test
    fun usingTheSuggestionEntersItForItsParticipant() {
        val changes = mutableListOf<Pair<UUID, String>>()
        setContent(
            aLoadedState(scores = mapOf(aliceId to "100", bobId to ""), suggestion = ScoreSuggestion(bobId, 150)),
            onScoreChanged = { id, value -> changes += id to value }
        )

        composeTestRule.onNodeWithTag("score_field_use_suggestion_$bobId").performClick()

        assertEquals(listOf(bobId to "150"), changes)
    }

    @Test
    fun showsTheErrorMessageAndMarksEveryFieldWhenTheTotalIsExceeded() {
        setContent(aLoadedState(scores = mapOf(aliceId to "200", bobId to "100")))

        composeTestRule.onNodeWithText("Total : 300 — corrigez pour atteindre 250.").assertIsDisplayed()
        listOf(aliceId, bobId).forEach { participantId ->
            composeTestRule.onNodeWithTag("score_field_$participantId")
                .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Error))
        }
    }

    @Test
    fun enablesValidationForARolledSuitAndExactTotalAndEmitsOnValidate() {
        var validateCount = 0
        setContent(
            aLoadedState(suit = CardSuit.HEART, scores = mapOf(aliceId to "100", bobId to "150")),
            onValidate = { validateCount++ }
        )

        composeTestRule.onNodeWithTag("round_entry_validate_button")
            .assertIsEnabled()
            .performClick()

        assertEquals(1, validateCount)
    }

    @Test
    fun disablesValidationWhileTheSuitIsNotRolled() {
        setContent(aLoadedState(scores = mapOf(aliceId to "100", bobId to "150")))

        composeTestRule.onNodeWithTag("round_entry_validate_button").assertIsNotEnabled()
    }

    @Test
    fun disablesValidationWhileSavingAndShowsASaveError() {
        setContent(
            aLoadedState(suit = CardSuit.HEART, scores = mapOf(aliceId to "100", bobId to "150"))
                .copy(isSaving = true, saveError = true)
        )

        composeTestRule.onNodeWithTag("round_entry_validate_button").assertIsNotEnabled()
        composeTestRule.onNodeWithTag("round_entry_save_error").assertIsDisplayed()
    }

    @Test
    fun disablesValidationForAnIncompleteTotal() {
        setContent(aLoadedState(scores = mapOf(aliceId to "100", bobId to "")))

        composeTestRule.onNodeWithTag("round_entry_validate_button").assertIsNotEnabled()
    }

    @Test
    fun forwardsIndependentScoreChangesForEachParticipant() {
        val changes = mutableListOf<Pair<UUID, String>>()
        setContent(aLoadedState(), onScoreChanged = { id, value -> changes.add(id to value) })

        composeTestRule.onNodeWithTag("score_field_$bobId").performTextInput("80")
        composeTestRule.onNodeWithTag("score_field_$aliceId").performTextInput("25")

        assertEquals(listOf(bobId to "80", aliceId to "25"), changes.filter { it.second.isNotEmpty() })
    }

    @Test
    fun forwardsTheRollIntentFromTheDieRoller() {
        var rolled = false
        setContent(aLoadedState(), onRoll = { rolled = true })

        composeTestRule.onNodeWithTag("die_roll_button").performClick()

        assertTrue(rolled)
    }

    @Test
    fun keepsShowingTheResultAfterARoll() {
        setContent(aLoadedState(suit = CardSuit.DIAMOND))

        composeTestRule.onNodeWithTag("die_result").assertIsDisplayed()
    }

    @Test
    fun disablesRerollAsSoonAsAnyScoreIsEntered() {
        setContent(aLoadedState(suit = CardSuit.DIAMOND, scores = mapOf(aliceId to "10", bobId to "")))

        composeTestRule.onNodeWithTag("die_reroll_button").assertIsNotEnabled()
    }

    @Test
    fun keepsRerollEnabledWhileAllScoresAreEmpty() {
        setContent(aLoadedState(suit = CardSuit.DIAMOND))

        composeTestRule.onNodeWithTag("die_reroll_button").assertIsEnabled()
    }

    @Test
    fun focusesTheFirstEmptyFieldAndMovesFocusWithImeActions() {
        setContent(aLoadedState())

        composeTestRule.onNodeWithTag("score_field_$aliceId").assertIsFocused().performImeAction()
        composeTestRule.onNodeWithTag("score_field_$bobId").assertIsFocused().performImeAction()
        composeTestRule.onNodeWithTag("score_field_$bobId").assertIsNotFocused()
    }

    @Test
    fun clearingAFieldAfterLoadDoesNotRestartAutomaticFocus() {
        val state = mutableStateOf(
            aLoadedState(scores = mapOf(aliceId to "10", bobId to "20"))
        )
        composeTestRule.setContent {
            RvScoreCounterTheme {
                RoundEntryScreen(
                    state = state.value,
                    onRoll = {},
                    onScoreChanged = { _, _ -> },
                    onRetry = {}
                )
            }
        }
        composeTestRule.onNodeWithTag("score_field_$aliceId").performClick().assertIsFocused()

        composeTestRule.runOnIdle {
            state.value = aLoadedState(scores = mapOf(aliceId to "10", bobId to ""))
        }

        composeTestRule.onNodeWithTag("score_field_$aliceId").assertIsFocused()
        composeTestRule.onNodeWithTag("score_field_$bobId").assertIsNotFocused()
    }

    // --- Qui a eu le 40 ---

    @Test
    fun asksWhoTookTheSevenOfTheRolledSuitAndEmitsTheChoice() {
        var chosen: UUID? = null
        composeTestRule.setContent {
            RvScoreCounterTheme {
                RoundEntryScreen(
                    state = aLoadedState(suit = CardSuit.HEART).let { it.copy(content = it.content!!.copy(sevenPoints = 40)) },
                    onRoll = {},
                    onScoreChanged = { _, _ -> },
                    onRetry = {},
                    onSevenHolderSelected = { chosen = it }
                )
            }
        }

        composeTestRule.onNodeWithTag("round_entry_seven_question").assertTextEquals("Qui a eu le 7 de Cœur (40 points) ?")
        composeTestRule.onNodeWithTag("round_entry_seven_holder_$bobId").performScrollTo().performClick()

        assertEquals(bobId, chosen)
    }

    @Test
    fun flagsAHolderWithFewerPointsThanTheSevenAndBlocksValidation() {
        setContent(
            aLoadedState(suit = CardSuit.HEART, scores = mapOf(aliceId to "230", bobId to "20"))
                .let { it.copy(content = it.content!!.copy(sevenPoints = 40, sevenHolderId = bobId)) }
        )

        composeTestRule.onNodeWithTag("round_entry_seven_error")
            .assertTextEquals("Bob a moins de 40 points sur cette manche : vérifiez le score ou le joueur.")
        composeTestRule.onNodeWithTag("round_entry_validate_button").assertIsNotEnabled()
    }

    // --- Panneaux sous le dé ---

    private val aSummary = RoundEntrySummary(
        standings = listOf(
            StandingRow(aliceId, rank = 1, name = "Alice", cumulativeScore = 140, isDealer = false),
            StandingRow(bobId, rank = 2, name = "Bob", cumulativeScore = 360, isDealer = true)
        )
    )

    @Test
    fun showsOnlyTheDieAndThePanelButtonsUntilAPanelIsChosen() {
        setContent(aLoadedState(panel = RoundEntryPanel.NONE))

        composeTestRule.onNodeWithTag("round_entry_die_roller").assertIsDisplayed()
        composeTestRule.onNodeWithTag("round_entry_standings_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("round_entry_scores_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("score_field_$aliceId").assertDoesNotExist()
        composeTestRule.onNodeWithTag("round_entry_validate_button").assertDoesNotExist()
        composeTestRule.onNodeWithTag("round_entry_standings_panel").assertDoesNotExist()
    }

    @Test
    fun panelButtonsEmitTheirPanelAndMarkTheShownOneAsSelected() {
        val chosen = mutableListOf<RoundEntryPanel>()
        composeTestRule.setContent {
            RvScoreCounterTheme {
                RoundEntryScreen(
                    state = aLoadedState(panel = RoundEntryPanel.SCORES),
                    onRoll = {},
                    onScoreChanged = { _, _ -> },
                    onRetry = {},
                    onPanelSelected = { chosen += it }
                )
            }
        }

        composeTestRule.onNodeWithTag("round_entry_scores_button").assertIsSelected()
        composeTestRule.onNodeWithTag("round_entry_standings_button").assertIsNotSelected().performClick()
        composeTestRule.onNodeWithTag("round_entry_scores_button").performClick()

        assertEquals(listOf(RoundEntryPanel.STANDINGS, RoundEntryPanel.SCORES), chosen)
    }

    @Test
    fun theStandingsPanelShowsOnlyTheStandings() {
        val state = aLoadedState(panel = RoundEntryPanel.STANDINGS)
        setContent(state.copy(content = state.content!!.copy(summary = aSummary)))

        composeTestRule.onNodeWithTag("score_field_$aliceId").assertDoesNotExist()
        composeTestRule.onNodeWithTag("round_entry_standing_row_$aliceId").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("round_entry_standing_row_$bobId").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("stat_card_seven_champion").assertDoesNotExist()
        composeTestRule.onNodeWithTag("round_table_row_1").assertDoesNotExist()
        composeTestRule.onNodeWithTag("score_chart_plot").assertDoesNotExist()
    }

    @Test
    fun shrinksTheDieWhileTheScorePanelIsShown() {
        val state = mutableStateOf(aLoadedState(suit = CardSuit.SPADE, panel = RoundEntryPanel.NONE))
        composeTestRule.setContent {
            RvScoreCounterTheme {
                RoundEntryScreen(state = state.value, onRoll = {}, onScoreChanged = { _, _ -> }, onRetry = {})
            }
        }
        val largeHeight = composeTestRule.onNodeWithTag("round_entry_die_roller").getUnclippedBoundsInRoot().height

        state.value = aLoadedState(suit = CardSuit.SPADE, panel = RoundEntryPanel.SCORES)

        val compactHeight = composeTestRule.onNodeWithTag("round_entry_die_roller").getUnclippedBoundsInRoot().height
        assertTrue("compact $compactHeight vs large $largeHeight", compactHeight < largeHeight / 2)
        composeTestRule.onNodeWithTag("die_result").assertIsDisplayed()
        composeTestRule.onNodeWithTag("die_reroll_button").assertIsDisplayed()
    }

    // --- Correction d'une Manche validée ---

    @Test
    fun aCorrectionShowsItsRoundAndSuitInsteadOfTheDieAndSavesWithItsOwnButton() {
        var validated = false
        setContent(
            aLoadedState(suit = CardSuit.CLUB, scores = mapOf(aliceId to "200", bobId to "50"))
                .copy(correctedRoundNumber = 3),
            onValidate = { validated = true }
        )

        composeTestRule.onNodeWithTag("round_entry_title").assertTextEquals("Correction de la manche 3")
        composeTestRule.onNodeWithTag("round_correction_suit").assertTextEquals("Couleur du 7 : ♣ Trèfle")
        composeTestRule.onNodeWithTag("round_entry_die_roller").assertDoesNotExist()
        composeTestRule.onNodeWithTag("round_entry_standings_button").assertDoesNotExist()
        composeTestRule.onNodeWithTag("round_entry_validate_button")
            .assertTextEquals("Enregistrer la correction")
            .assertIsEnabled()
            .performClick()

        assertTrue(validated)
    }

    // --- « Qui a eu le 40 ? » d'abord, validation fixée en bas ---

    @Test
    fun theSevenQuestionComesBeforeTheScoreFields() {
        setContent(aLoadedState(suit = CardSuit.HEART).let { it.copy(content = it.content!!.copy(sevenPoints = 40)) })

        val question = composeTestRule.onNodeWithTag("round_entry_seven_question").getUnclippedBoundsInRoot().top
        val firstField = composeTestRule.onNodeWithTag("score_field_$aliceId").getUnclippedBoundsInRoot().top
        assertTrue(question < firstField)
    }

    @Test
    fun theValidationBarStaysVisibleWithEightPlayers() {
        val players = List(8) { RoundEntryParticipant(UUID.randomUUID(), "Joueur $it") }
        val state = aLoadedState(suit = CardSuit.SPADE)
        setContent(state.copy(content = state.content!!.copy(participants = players, scores = emptyMap())))

        composeTestRule.onNodeWithTag("round_entry_validate_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("round_entry_sum_indicator").assertIsDisplayed()
    }

    @Test
    fun theValidationBarIsHiddenUntilTheScorePanelIsShown() {
        setContent(aLoadedState(panel = RoundEntryPanel.NONE))

        composeTestRule.onNodeWithTag("round_entry_validation_bar").assertDoesNotExist()
    }
}

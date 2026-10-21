package com.rvetiveau.rvscorecount.ui.screens.history

import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import com.rvetiveau.rvscorecount.R
import com.rvetiveau.rvscorecount.domain.model.PartySummary
import com.rvetiveau.rvscorecount.ui.components.PartySummaryRow
import com.rvetiveau.rvscorecount.ui.theme.RvScoreCounterTheme
import androidx.compose.ui.unit.dp
import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Instrumented tests for the pure [HistoryScreen] (Story 6.1). */
class HistoryScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val played = PartySummary(UUID.randomUUID(), "papayoo", 0L, roundCount = 3, winners = listOf("Bob"), winningScore = 120)
    private val empty = PartySummary(UUID.randomUUID(), "papayoo", 0L, roundCount = 0, winners = emptyList(), winningScore = null)
    private val broken = PartySummary(UUID.randomUUID(), "papayoo", 0L, roundCount = 1, winners = emptyList(), winningScore = null, isConsistent = false)

    private fun setContent(
        state: HistoryUiState,
        onEntrySelected: (UUID) -> Unit = {},
        onDeleteRequested: (UUID) -> Unit = {},
        onDeleteConfirmed: () -> Unit = {},
        onDeleteDismissed: () -> Unit = {}
    ) {
        composeTestRule.setContent {
            RvScoreCounterTheme {
                HistoryScreen(
                    state = state,
                    onEntrySelected = onEntrySelected,
                    onDeleteRequested = onDeleteRequested,
                    onDeleteConfirmed = onDeleteConfirmed,
                    onDeleteDismissed = onDeleteDismissed
                )
            }
        }
    }

    @Test
    fun eachEntryShowsGameRoundCountAndWinnerOrItsAbsence() {
        setContent(HistoryUiState.Loaded(listOf(played, empty, broken)))

        composeTestRule.onNodeWithTag("history_entry_${played.partyId}")
            .assertTextContains("Papayoo", substring = true)
            .assertTextContains("3 manches", substring = true)
            .assertTextContains("Vainqueur : Bob · 120 points")
        composeTestRule.onNodeWithTag("history_entry_${empty.partyId}").assertTextContains("Aucune manche jouée")
        composeTestRule.onNodeWithTag("history_entry_${broken.partyId}").assertTextContains("Données incohérentes")
    }

    @Test
    fun tappingAnEntryEmitsItsId() {
        var selected: UUID? = null
        setContent(HistoryUiState.Loaded(listOf(played)), onEntrySelected = { selected = it })

        composeTestRule.onNodeWithText("Vainqueur : Bob · 120 points").performClick()

        assertEquals(played.partyId, selected)
    }

    @Test
    fun swipingLeftAsksForDeletionWithoutDeleting() {
        var requested: UUID? = null
        setContent(HistoryUiState.Loaded(listOf(played)), onDeleteRequested = { requested = it })

        composeTestRule.onNodeWithTag("history_entry_${played.partyId}").performTouchInput { swipeLeft() }
        composeTestRule.waitForIdle()

        assertEquals(played.partyId, requested)
        composeTestRule.onNodeWithTag("history_entry_${played.partyId}").assertTextContains("Vainqueur : Bob · 120 points")
    }

    @Test
    fun theVisibleTrashAsksForDeletionWithoutOpeningTheParty() {
        var requested: UUID? = null
        var opened: UUID? = null
        setContent(HistoryUiState.Loaded(listOf(played)), onDeleteRequested = { requested = it }, onEntrySelected = { opened = it })

        composeTestRule.onNodeWithTag("history_delete_${played.partyId}")
            .assertIsDisplayed()
            .assertHeightIsAtLeast(48.dp)
            .assertWidthIsAtLeast(48.dp)
            .assert(hasContentDescription("Supprimer la partie du", substring = true))
            .performClick()

        assertEquals(played.partyId, requested)
        assertNull(opened)
    }

    @Test
    fun aRowWithoutDeletionHasNoTrash() {
        composeTestRule.setContent {
            RvScoreCounterTheme { PartySummaryRow(entry = played, onSelected = {}) }
        }

        composeTestRule.onNodeWithTag("history_delete_${played.partyId}").assertDoesNotExist()
    }

    @Test
    fun theConfirmationDialogConfirmsOrCancels() {
        var confirmed = false
        var dismissed = false
        setContent(
            HistoryUiState.Loaded(listOf(played), pendingDeletionId = played.partyId),
            onDeleteConfirmed = { confirmed = true },
            onDeleteDismissed = { dismissed = true }
        )

        composeTestRule.onNodeWithText("Supprimer cette partie ?").assertExists()
        composeTestRule.onNodeWithTag("history_delete_confirm_button").assertTextEquals("Supprimer").performClick()
        composeTestRule.onNodeWithTag("history_delete_cancel_button").performClick()

        assertTrue(confirmed)
        assertTrue(dismissed)
    }

    @Test
    fun showsTheEmptyState() {
        setContent(HistoryUiState.Empty)

        composeTestRule.onNodeWithTag("history_empty").assertTextEquals("Aucune partie terminée pour l'instant.")
    }

    @Test
    fun showsTheDeleteErrorAboveTheList() {
        setContent(HistoryUiState.Loaded(listOf(played), deleteErrorRes = R.string.history_delete_error))

        composeTestRule.onNodeWithTag("history_delete_error").assertTextEquals("Impossible de supprimer la partie. Réessayez.")
    }
}

package com.rvetiveau.rvscorecount.ui.screens.players

import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.unit.dp
import com.rvetiveau.rvscorecount.R
import com.rvetiveau.rvscorecount.domain.model.PlayerSummary
import com.rvetiveau.rvscorecount.ui.theme.RvScoreCounterTheme
import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Rendering and callbacks of the pure [PlayersScreen] (Story 10.2); rules are in [PlayersViewModelTest]. */
class PlayersScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val alice = PlayerSummary(UUID.randomUUID(), "Alice", finishedParties = 3, lastPlayedAt = 1_760_000_000_000)
    private val bob = PlayerSummary(UUID.randomUUID(), "Bob", finishedParties = 0, lastPlayedAt = null)

    private fun setContent(
        state: PlayersUiState,
        onRenameRequested: (UUID) -> Unit = {},
        onRenameInputChanged: (String) -> Unit = {},
        onRenameConfirmed: () -> Unit = {}
    ) {
        composeTestRule.setContent {
            RvScoreCounterTheme {
                PlayersScreen(
                    state = state,
                    onRenameRequested = onRenameRequested,
                    onRenameInputChanged = onRenameInputChanged,
                    onRenameConfirmed = onRenameConfirmed
                )
            }
        }
    }

    @Test
    fun eachPlayerShowsTheirFinishedPartiesAndLatestPartie() {
        setContent(PlayersUiState.Loaded(listOf(alice, bob)))

        composeTestRule.onNodeWithTag("players_name_${alice.id}").assertTextEquals("Alice")
        val aliceDetails = composeTestRule.onNodeWithTag("players_details_${alice.id}")
        aliceDetails.assertIsDisplayed()
        assertTrue(
            aliceDetails.fetchSemanticsNode().config[androidx.compose.ui.semantics.SemanticsProperties.Text]
                .single().text.startsWith("3 parties terminées · dernière partie le ")
        )
        composeTestRule.onNodeWithTag("players_details_${bob.id}").assertTextEquals("Aucune partie")
    }

    @Test
    fun thePencilAsksToRenameItsPlayer() {
        val requested = mutableListOf<UUID>()
        setContent(PlayersUiState.Loaded(listOf(alice, bob)), onRenameRequested = { requested += it })

        composeTestRule.onNodeWithContentDescription("Renommer Bob")
            .assertHeightIsAtLeast(48.dp)
            .performClick()

        assertEquals(listOf(bob.id), requested)
    }

    @Test
    fun theRenameDialogShowsTheRefusalAndRelaysTheInput() {
        val inputs = mutableListOf<String>()
        var confirmed = false
        setContent(
            PlayersUiState.Loaded(
                listOf(alice, bob),
                PlayerRenaming(alice.id, "Alice", input = "bob", errorRes = R.string.players_rename_taken, takenBy = "Bob")
            ),
            onRenameInputChanged = { inputs += it },
            onRenameConfirmed = { confirmed = true }
        )

        composeTestRule.onNodeWithTag("players_rename_error").assertTextEquals("Bob existe déjà")
        composeTestRule.onNodeWithTag("players_rename_field").performTextReplacement("Alicia")
        composeTestRule.onNodeWithTag("players_rename_confirm").performClick()

        assertEquals(listOf("Alicia"), inputs)
        assertTrue(confirmed)
    }

    @Test
    fun anEmptyListSaysSo() {
        setContent(PlayersUiState.Empty)

        composeTestRule.onNodeWithTag("players_empty").assertIsDisplayed()
    }
}

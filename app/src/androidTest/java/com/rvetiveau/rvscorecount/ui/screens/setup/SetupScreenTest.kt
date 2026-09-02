package com.rvetiveau.rvscorecount.ui.screens.setup

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.rvetiveau.rvscorecount.ui.theme.RvScoreCounterTheme
import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Instrumented tests for the pure [SetupScreen] (AC 1-2, 5, 7): state rendering and callback
 * contracts only — business rules are covered by [SetupViewModelTest]. */
class SetupScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun draft(name: String) = ParticipantDraft(name = name)

    private fun stateWith(
        participants: List<ParticipantDraft> = emptyList(),
        minPlayers: Int = 3,
        maxPlayers: Int = 8,
        messageRes: Int? = null
    ) = SetupUiState(
        gameId = "papayoo",
        minPlayers = minPlayers,
        maxPlayers = maxPlayers,
        isInitialized = true,
        participants = participants,
        messageRes = messageRes
    )

    private fun setContent(
        state: SetupUiState,
        onAddParticipant: () -> Unit = {},
        onRemoveParticipant: (UUID) -> Unit = {},
        onMoveParticipant: (Int, Int) -> Unit = { _, _ -> }
    ) {
        composeTestRule.setContent {
            RvScoreCounterTheme {
                SetupScreen(
                    state = state,
                    onNameChanged = {},
                    onAddParticipant = onAddParticipant,
                    onRemoveParticipant = onRemoveParticipant,
                    onMoveParticipant = onMoveParticipant,
                    onStartParty = {}
                )
            }
        }
    }

    @Test
    fun showsTheInlineErrorMessageWhenTheStateCarriesOne() {
        setContent(stateWith(messageRes = com.rvetiveau.rvscorecount.R.string.setup_error_empty_name))

        composeTestRule.onNodeWithTag("setup_input_message").assertTextEquals("Veuillez entrer un prénom")
    }

    @Test
    fun showsTheMaxReachedMessageAutomaticallyWhenAtCapacity() {
        val eightParticipants = (1..8).map { draft("Joueur$it") }
        setContent(stateWith(participants = eightParticipants, minPlayers = 3, maxPlayers = 8))

        composeTestRule.onNodeWithTag("setup_input_message").assertTextEquals("Maximum 8 joueurs atteint")
    }

    @Test
    fun disablesTheNameFieldAndAddButtonWhenTheMaximumIsReached() {
        val eightParticipants = (1..8).map { draft("Joueur$it") }
        setContent(stateWith(participants = eightParticipants, minPlayers = 3, maxPlayers = 8))

        composeTestRule.onNodeWithTag("setup_name_field").assertIsNotEnabled()
        composeTestRule.onNodeWithTag("setup_add_button").assertIsNotEnabled()
    }

    @Test
    fun enablesTheNameFieldAndAddButtonBelowTheMaximum() {
        setContent(stateWith(participants = listOf(draft("Alice"))))

        composeTestRule.onNodeWithTag("setup_name_field").assertIsEnabled()
        composeTestRule.onNodeWithTag("setup_add_button").assertIsEnabled()
    }

    @Test
    fun disablesTheStartButtonBelowTheMinimum() {
        setContent(stateWith(participants = listOf(draft("Alice"), draft("Bob")), minPlayers = 3))

        composeTestRule.onNodeWithTag("setup_start_button").assertIsNotEnabled()
    }

    @Test
    fun enablesTheStartButtonAtTheMinimum() {
        setContent(stateWith(participants = listOf(draft("Alice"), draft("Bob"), draft("Charlie")), minPlayers = 3))

        composeTestRule.onNodeWithTag("setup_start_button").assertIsEnabled()
    }

    @Test
    fun showsTheParticipantCounterWithCurrentMinAndMax() {
        setContent(stateWith(participants = listOf(draft("Alice"), draft("Bob")), minPlayers = 3, maxPlayers = 8))

        composeTestRule.onNodeWithTag("setup_participant_count").assertTextEquals("2/3 à 8")
    }

    @Test
    fun showsTheDealerBadgeOnlyOnTheFirstParticipant() {
        setContent(stateWith(participants = listOf(draft("Alice"), draft("Bob"), draft("Charlie"))))

        composeTestRule.onNodeWithTag("setup_dealer_badge").assertContentDescriptionEquals("Donneur")
        composeTestRule.onNodeWithText("Donneur M1", useUnmergedTree = true).assertTextEquals("Donneur M1")
    }

    @Test
    fun clickingDeleteInvokesTheCallbackWithTheExactParticipantId() {
        val alice = draft("Alice")
        val bob = draft("Bob")
        var removedId: UUID? = null
        setContent(
            stateWith(participants = listOf(alice, bob)),
            onRemoveParticipant = { removedId = it }
        )

        composeTestRule.onNodeWithTag("setup_participant_delete_${bob.id}").performClick()

        assertEquals(bob.id, removedId)
    }

    @Test
    fun dragHandleExposesAnExplicitAccessibilityDescription() {
        val alice = draft("Alice")
        setContent(stateWith(participants = listOf(alice, draft("Bob"), draft("Charlie"))))

        composeTestRule.onNodeWithTag("setup_participant_handle_${alice.id}")
            .assertContentDescriptionEquals("Réorganiser Alice, joueur 1 sur 3")
    }

    @Test
    fun deleteActionExposesAnExplicitAccessibilityDescription() {
        val alice = draft("Alice")
        setContent(stateWith(participants = listOf(alice)))

        composeTestRule.onNodeWithTag("setup_participant_delete_${alice.id}")
            .assertContentDescriptionEquals("Supprimer Alice")
    }

    @Test
    fun interactiveControlsMeetTheFortyEightDpTouchTargetFloor() {
        val alice = draft("Alice")
        setContent(stateWith(participants = listOf(alice)))

        composeTestRule.onNodeWithTag("setup_add_button").assertHeightIsAtLeast(48.dp).assertWidthIsAtLeast(48.dp)
        composeTestRule.onNodeWithTag("setup_start_button").assertHeightIsAtLeast(48.dp)
        composeTestRule.onNodeWithTag("setup_participant_handle_${alice.id}")
            .assertHeightIsAtLeast(48.dp)
            .assertWidthIsAtLeast(48.dp)
        composeTestRule.onNodeWithTag("setup_participant_delete_${alice.id}")
            .assertHeightIsAtLeast(48.dp)
            .assertWidthIsAtLeast(48.dp)
    }

    @Test
    fun participantNameIsNotTruncatedAtALargeFontScale() {
        val longName = "Bartholomew-Alexandre-Christophe"
        val participant = draft(longName)
        composeTestRule.setContent {
            val largeFontDensity = Density(density = LocalDensity.current.density, fontScale = 2f)
            CompositionLocalProvider(LocalDensity provides largeFontDensity) {
                RvScoreCounterTheme {
                    SetupScreen(
                        state = stateWith(participants = listOf(participant)),
                        onNameChanged = {},
                        onAddParticipant = {},
                        onRemoveParticipant = {},
                        onMoveParticipant = { _, _ -> },
                        onStartParty = {}
                    )
                }
            }
        }

        val textLayoutResults = mutableListOf<androidx.compose.ui.text.TextLayoutResult>()
        val getTextLayoutResult = composeTestRule
            .onNodeWithTag("setup_participant_name_${participant.id}", useUnmergedTree = true)
            .fetchSemanticsNode()
            .config[SemanticsActions.GetTextLayoutResult]

        assertTrue(getTextLayoutResult.action?.invoke(textLayoutResults) == true)
        assertTrue(textLayoutResults.none { it.hasVisualOverflow })
    }

    @Test
    fun draggingTheHandleUpdatesTheRenderedOrderAcrossConsecutiveMoves() {
        val alice = draft("Alice")
        val bob = draft("Bob")
        val charlie = draft("Charlie")
        var participants by mutableStateOf(listOf(alice, bob, charlie))
        val moves = mutableListOf<Pair<Int, Int>>()
        composeTestRule.setContent {
            RvScoreCounterTheme {
                SetupScreen(
                    state = stateWith(participants = participants),
                    onNameChanged = {},
                    onAddParticipant = {},
                    onRemoveParticipant = {},
                    onMoveParticipant = { from, to ->
                        moves += from to to
                        participants = participants.toMutableList().apply { add(to, removeAt(from)) }
                    },
                    onStartParty = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("setup_participant_handle_${alice.id}").performTouchInput {
            down(center)
            advanceEventTime(600)
            moveBy(Offset(0f, 140f))
            advanceEventTime(16)
            up()
        }
        composeTestRule.waitForIdle()

        assertEquals(listOf(bob, alice, charlie), participants)
        composeTestRule.onNodeWithTag("setup_participant_handle_${alice.id}")
            .assertContentDescriptionEquals("Réorganiser Alice, joueur 2 sur 3")

        composeTestRule.onNodeWithTag("setup_participant_handle_${alice.id}").performTouchInput {
            down(center)
            advanceEventTime(600)
            moveBy(Offset(0f, 140f))
            advanceEventTime(16)
            up()
        }
        composeTestRule.waitForIdle()

        assertEquals(listOf(bob, charlie, alice), participants)
        composeTestRule.onNodeWithTag("setup_participant_handle_${alice.id}")
            .assertContentDescriptionEquals("Réorganiser Alice, joueur 3 sur 3")

        composeTestRule.onNodeWithTag("setup_participant_handle_${alice.id}").performTouchInput {
            down(center)
            advanceEventTime(600)
            moveBy(Offset(0f, -140f))
            advanceEventTime(16)
            up()
        }
        composeTestRule.waitForIdle()

        assertEquals(listOf(bob, alice, charlie), participants)

        composeTestRule.onNodeWithTag("setup_participant_handle_${alice.id}").performTouchInput {
            down(center)
            advanceEventTime(600)
            moveBy(Offset(0f, -140f))
            advanceEventTime(16)
            up()
        }
        composeTestRule.waitForIdle()

        assertEquals(listOf(alice, bob, charlie), participants)
        assertEquals(listOf(0 to 1, 1 to 2, 2 to 1, 1 to 0), moves)
        composeTestRule.onNodeWithTag("setup_participant_handle_${alice.id}")
            .assertContentDescriptionEquals("Réorganiser Alice, joueur 1 sur 3")
    }
}

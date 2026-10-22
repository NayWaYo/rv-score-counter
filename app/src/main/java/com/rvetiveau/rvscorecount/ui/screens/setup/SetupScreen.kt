package com.rvetiveau.rvscorecount.ui.screens.setup

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.rvetiveau.rvscorecount.R
import com.rvetiveau.rvscorecount.ui.components.HistoryButton
import com.rvetiveau.rvscorecount.ui.components.ScreenHeader
import java.util.UUID

/**
 * Stateless Création de Partie screen (AC 1-5, 7, 8): name entry, ordered Participant list with
 * drag-to-reorder and removal, and the (not-yet-wired) "Commencer" action. No Hilt, repository,
 * concrete GameEngine or NavController access is allowed here.
 */
@Composable
fun SetupScreen(
    state: SetupUiState,
    onNameChanged: (String) -> Unit,
    onAddParticipant: () -> Unit,
    onRemoveParticipant: (UUID) -> Unit,
    onMoveParticipant: (Int, Int) -> Unit,
    onStartParty: () -> Unit,
    modifier: Modifier = Modifier,
    onHistorySelected: () -> Unit = {},
    onSuggestionSelected: (String) -> Unit = {}
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp)
    ) {
        ScreenHeader(
            title = stringResource(R.string.setup_title),
            titleTag = "setup_title",
            modifier = Modifier.padding(vertical = 8.dp)
        ) {
            HistoryButton(onClick = onHistorySelected, modifier = Modifier.testTag("setup_history_action"))
        }

        if (state.creationErrorRes != null) {
            Text(
                text = stringResource(state.creationErrorRes),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier
                    .padding(top = 4.dp)
                    .testTag("setup_creation_error_message")
            )
        }

        val messageRes = state.messageRes ?: R.string.setup_max_players_reached.takeIf { state.isMaxReached }
        ParticipantDraftEditor(
            nameInput = state.nameInput,
            canAdd = state.canAdd,
            isNameError = state.messageRes == R.string.setup_error_empty_name,
            suggestions = state.nameSuggestions,
            message = messageRes?.let { res ->
                if (res == R.string.setup_max_players_reached) stringResource(res, state.maxPlayers) else stringResource(res)
            },
            countText = stringResource(R.string.setup_participant_count, state.participants.size, state.minPlayers, state.maxPlayers),
            participants = state.participants,
            onNameChanged = onNameChanged,
            onAddParticipant = onAddParticipant,
            onSuggestionSelected = onSuggestionSelected,
            onMoveParticipant = onMoveParticipant,
            onRemoveParticipant = onRemoveParticipant
        )

        Button(
            onClick = onStartParty,
            enabled = state.canStart,
            modifier = Modifier
                .padding(vertical = 16.dp)
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .testTag("setup_start_button")
        ) {
            Text(text = stringResource(R.string.setup_start_button))
        }
    }
}

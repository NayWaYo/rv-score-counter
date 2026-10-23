package com.rvetiveau.rvscorecount.ui.screens.tarot

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rvetiveau.rvscorecount.R
import com.rvetiveau.rvscorecount.ui.components.HistoryButton
import com.rvetiveau.rvscorecount.ui.components.ScreenHeader
import com.rvetiveau.rvscorecount.ui.navigation.NavigationEvent
import com.rvetiveau.rvscorecount.ui.screens.free.Segments
import com.rvetiveau.rvscorecount.ui.screens.setup.ParticipantDraftEditor
import java.util.UUID

/** What the creation of a Partie of Tarot reports to its ViewModel (Story 13.3). */
data class TarotSetupActions(
    val onNameChanged: (String) -> Unit = {},
    val onAddParticipant: () -> Unit = {},
    val onSuggestionSelected: (String) -> Unit = {},
    val onRemoveParticipant: (UUID) -> Unit = {},
    val onMoveParticipant: (Int, Int) -> Unit = { _, _ -> },
    val onRoundLimitChanged: (Boolean) -> Unit = {},
    val onRoundCountChanged: (String) -> Unit = {},
    val onStartParty: () -> Unit = {},
    val onHistorySelected: () -> Unit = {}
)

/** Compose/Hilt adapter for the creation of a Partie of Tarot (Story 13.3). */
@Composable
fun TarotSetupRoute(
    onNavigate: (NavigationEvent) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TarotSetupViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val currentOnNavigate by rememberUpdatedState(onNavigate)

    LaunchedEffect(viewModel) {
        viewModel.navigationEvents.collect { event -> currentOnNavigate(event) }
    }

    TarotSetupScreen(
        state = state,
        actions = TarotSetupActions(
            onNameChanged = viewModel::onNameChanged,
            onAddParticipant = viewModel::onAddParticipant,
            onSuggestionSelected = viewModel::onSuggestionSelected,
            onRemoveParticipant = viewModel::onRemoveParticipant,
            onMoveParticipant = viewModel::onMoveParticipant,
            onRoundLimitChanged = viewModel::onRoundLimitChanged,
            onRoundCountChanged = viewModel::onRoundCountChanged,
            onStartParty = viewModel::onStartParty,
            onHistorySelected = viewModel::onHistorySelected
        ),
        modifier = modifier
    )
}

/**
 * Stateless creation of a Partie of Tarot (Story 13.3): the Participants (the same editor as the
 * other Jeux, the list taking the room left), then how it ends.
 */
@Composable
fun TarotSetupScreen(
    state: TarotSetupUiState,
    modifier: Modifier = Modifier,
    actions: TarotSetupActions = TarotSetupActions()
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .imePadding()
            .padding(horizontal = 16.dp)
    ) {
        ScreenHeader(
            title = stringResource(R.string.tarot_setup_title),
            titleTag = "tarot_setup_title",
            modifier = Modifier.padding(vertical = 8.dp)
        ) {
            HistoryButton(onClick = actions.onHistorySelected, modifier = Modifier.testTag("tarot_setup_history_action"))
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
            onNameChanged = actions.onNameChanged,
            onAddParticipant = actions.onAddParticipant,
            onSuggestionSelected = actions.onSuggestionSelected,
            onMoveParticipant = actions.onMoveParticipant,
            onRemoveParticipant = actions.onRemoveParticipant
        )

        EndSection(state, actions)

        state.creationErrorRes?.let { errorRes ->
            Text(
                text = stringResource(errorRes),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.testTag("tarot_setup_error")
            )
        }
        Button(
            onClick = actions.onStartParty,
            enabled = state.canStart,
            modifier = Modifier
                .padding(vertical = 12.dp)
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .testTag("tarot_setup_start")
        ) {
            Text(text = stringResource(R.string.setup_start_button))
        }
    }
}

@Composable
private fun EndSection(state: TarotSetupUiState, actions: TarotSetupActions) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 8.dp)) {
        Text(text = stringResource(R.string.tarot_setup_end_title), style = MaterialTheme.typography.titleSmall)
        Segments(
            options = listOf(false, true),
            selected = state.hasRoundLimit,
            label = { stringResource(if (it) R.string.tarot_end_rounds else R.string.tarot_end_manual) },
            tag = { if (it) "tarot_setup_end_rounds" else "tarot_setup_end_manual" },
            onSelected = actions.onRoundLimitChanged
        )
        if (state.hasRoundLimit) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.tarot_end_rounds_label),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = state.roundCountInput,
                    onValueChange = actions.onRoundCountChanged,
                    singleLine = true,
                    isError = !state.isEndValueValid,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .width(112.dp)
                        .testTag("tarot_setup_round_count")
                )
            }
            if (!state.isEndValueValid) {
                Text(
                    text = stringResource(R.string.tarot_end_value_error),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.testTag("tarot_setup_round_count_error")
                )
            }
        }
    }
}

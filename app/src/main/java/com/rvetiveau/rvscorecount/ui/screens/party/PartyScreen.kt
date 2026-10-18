package com.rvetiveau.rvscorecount.ui.screens.party

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.rvetiveau.rvscorecount.R
import com.rvetiveau.rvscorecount.ui.components.RoundHistoryItem
import com.rvetiveau.rvscorecount.ui.components.RoundHistoryRow
import com.rvetiveau.rvscorecount.ui.components.StandingRowItem

/**
 * Stateless Partie en cours screen (Story 4.1): Classement sorted by Score Cumulé with the
 * Donneur badge, « Manche N — Donneur » subtitle, « Nouvelle manche » and « Fin de partie ».
 * Error and loading states never render a partial Classement.
 */
@Composable
fun PartyScreen(
    state: PartyUiState,
    modifier: Modifier = Modifier,
    onRetry: () -> Unit = {},
    onStartRound: () -> Unit = {},
    onAddParticipantClick: () -> Unit = {},
    onAdditionNameChanged: (String) -> Unit = {},
    onAdditionNameConfirmed: () -> Unit = {},
    onInsertionPositionSelected: (Int) -> Unit = {},
    onAdditionCancelled: () -> Unit = {},
    onShowRoundHistory: () -> Unit = {},
    onHideRoundHistory: () -> Unit = {},
    onEndPartyClick: () -> Unit = {},
    onEndPartyConfirmed: () -> Unit = {},
    onEndPartyDismissed: () -> Unit = {}
) {
    when (state) {
        is PartyUiState.Loading -> CenteredMessage(modifier) {
            CircularProgressIndicator(modifier = Modifier.testTag("party_loading"))
        }

        is PartyUiState.PartyNotFound -> CenteredMessage(modifier) {
            Text(
                text = stringResource(R.string.party_not_found),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.testTag("party_not_found")
            )
        }

        is PartyUiState.LoadError -> CenteredMessage(modifier) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = stringResource(R.string.party_load_error),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.testTag("party_load_error")
                )
                Button(onClick = onRetry, modifier = Modifier.padding(top = 16.dp)) {
                    Text(text = stringResource(R.string.root_error_retry))
                }
            }
        }

        is PartyUiState.InconsistentParticipants -> CenteredMessage(modifier) {
            Text(
                text = stringResource(R.string.party_inconsistent_participants),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.testTag("party_inconsistent")
            )
        }

        is PartyUiState.Loaded -> Column(
            modifier = modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text(
                text = stringResource(R.string.party_screen_title),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier
                    .padding(top = 16.dp)
                    .testTag("party_screen_title")
            )
            Text(
                text = stringResource(R.string.party_dealer_title, state.roundNumber, state.dealerName),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier
                    .padding(top = 4.dp, bottom = 16.dp)
                    .testTag("party_dealer_title")
            )
            val addition = state.addition
            when {
                addition != null -> AdditionNameRow(
                    addition = addition,
                    onNameChanged = onAdditionNameChanged,
                    onConfirm = onAdditionNameConfirmed,
                    onCancel = onAdditionCancelled
                )
                state.canAddParticipant -> TextButton(
                    onClick = onAddParticipantClick,
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("party_add_participant_button")
                ) {
                    Text(text = stringResource(R.string.party_add_participant_button))
                }
                state.isMaxPlayersReached -> Text(
                    text = stringResource(R.string.party_max_participants_reached, state.maxPlayers),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .padding(bottom = 8.dp)
                        .testTag("party_max_participants_reached")
                )
            }
            if (addition != null && addition.isChoosingPosition) {
                InsertionPositionSheet(
                    addition = addition,
                    onPositionSelected = onInsertionPositionSelected,
                    onDismiss = onAdditionCancelled
                )
            }
            if (state.roundNumber == 1) {
                Text(
                    text = stringResource(R.string.party_empty_state),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .padding(bottom = 16.dp)
                        .testTag("party_empty_state")
                )
            }
            if (state.roundHistory.isNotEmpty()) {
                TextButton(
                    onClick = onShowRoundHistory,
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("party_rounds_link")
                ) {
                    Text(text = stringResource(R.string.party_rounds_link, state.roundHistory.size))
                }
            }
            if (state.closure.isConfirmationOpen) {
                EndPartyDialog(
                    isClosing = state.closure.isClosing,
                    onConfirm = onEndPartyConfirmed,
                    onDismiss = onEndPartyDismissed
                )
            }
            if (state.isRoundHistoryOpen) {
                RoundHistorySheet(rounds = state.roundHistory, onDismiss = onHideRoundHistory)
            }
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .testTag("party_standings")
            ) {
                items(state.standings, key = { it.participantId }) { row ->
                    StandingRowItem(row, modifier = Modifier.testTag("party_standing_row_${row.participantId}"))
                }
            }
            if (state.canStartRound) {
                Button(
                    onClick = onStartRound,
                    modifier = Modifier
                        .padding(top = 16.dp)
                        .fillMaxWidth()
                        .heightIn(min = 56.dp)
                        .testTag("party_start_round_button")
                ) {
                    Text(text = stringResource(R.string.party_start_round_button))
                }
                state.closure.errorRes?.let { errorRes ->
                    Text(
                        text = stringResource(errorRes),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .testTag("party_end_error")
                    )
                }
                OutlinedButton(
                    onClick = onEndPartyClick,
                    enabled = !state.closure.isClosing,
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .fillMaxWidth()
                        .heightIn(min = 56.dp)
                        .testTag("party_end_button")
                ) {
                    Text(text = stringResource(R.string.party_end_button))
                }
            }
        }
    }
}

@Composable
private fun AdditionNameRow(
    addition: ParticipantAdditionUiState,
    onNameChanged: (String) -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    Column(modifier = Modifier.padding(bottom = 16.dp)) {
        OutlinedTextField(
            value = addition.nameInput,
            onValueChange = onNameChanged,
            label = { Text(text = stringResource(R.string.party_add_name_label)) },
            singleLine = true,
            isError = addition.errorRes != null && !addition.isChoosingPosition,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onConfirm() }),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("party_add_name_field")
        )
        if (addition.errorRes != null && !addition.isChoosingPosition) {
            Text(
                text = stringResource(addition.errorRes),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier
                    .padding(top = 4.dp)
                    .testTag("party_add_name_error")
            )
        }
        Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
            TextButton(onClick = onCancel, modifier = Modifier.testTag("party_add_cancel_button")) {
                Text(text = stringResource(R.string.party_add_cancel))
            }
            Button(onClick = onConfirm, modifier = Modifier.testTag("party_add_choose_position_button")) {
                Text(text = stringResource(R.string.party_add_choose_position))
            }
        }
    }
}

/** Seat picker (Story 4.2, AC 2-4, 7): one « Après {nom} » row per Participant in table order. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InsertionPositionSheet(
    addition: ParticipantAdditionUiState,
    onPositionSelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 32.dp)) {
            Text(
                text = stringResource(R.string.party_add_position_title, addition.nameInput.trim()),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier
                    .padding(bottom = 8.dp)
                    .testTag("party_add_position_title")
            )
            addition.insertionOptions.forEach { option ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp)
                        .clickable(enabled = !addition.isSaving) { onPositionSelected(option.position) }
                        .padding(vertical = 8.dp)
                        .testTag("party_add_position_${option.position}")
                ) {
                    Text(
                        text = stringResource(R.string.party_add_position_after, option.afterName),
                        style = MaterialTheme.typography.bodyLarge
                    )
                    if (option.becomesDealer) {
                        Text(
                            text = stringResource(R.string.party_add_position_becomes_dealer),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            if (addition.errorRes != null) {
                Text(
                    text = stringResource(addition.errorRes),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .testTag("party_add_position_error")
                )
            }
        }
    }
}

/** Confirmation of a destructive closure (Story 5.1): Confirmer in error color, Annuler. */
@Composable
private fun EndPartyDialog(isClosing: Boolean, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.party_end_dialog_title)) },
        text = { Text(text = stringResource(R.string.party_end_dialog_message)) },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = !isClosing,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.testTag("party_end_confirm_button")
            ) {
                Text(text = stringResource(R.string.party_end_dialog_confirm))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isClosing,
                modifier = Modifier.testTag("party_end_cancel_button")
            ) {
                Text(text = stringResource(R.string.party_add_cancel))
            }
        },
        modifier = Modifier.testTag("party_end_dialog")
    )
}

/** Read-only list of validated Manches, most recent first (Story 4.3). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RoundHistorySheet(rounds: List<RoundHistoryItem>, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Text(
            text = stringResource(R.string.party_rounds_title),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
        )
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .padding(start = 16.dp, end = 16.dp, bottom = 32.dp)
                .testTag("party_round_history")
        ) {
            items(rounds, key = { it.roundNumber }) { round ->
                RoundHistoryRow(
                    item = round,
                    modifier = Modifier.testTag("party_round_history_${round.roundNumber}")
                )
            }
        }
    }
}

@Composable
private fun CenteredMessage(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        content()
    }
}

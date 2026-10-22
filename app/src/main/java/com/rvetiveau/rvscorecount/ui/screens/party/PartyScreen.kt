package com.rvetiveau.rvscorecount.ui.screens.party

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.rvetiveau.rvscorecount.R
import com.rvetiveau.rvscorecount.ui.components.ScreenHeader
import com.rvetiveau.rvscorecount.domain.engine.DealingRules
import com.rvetiveau.rvscorecount.ui.components.StandingRowItem

/**
 * Stateless Partie en cours screen (Story 4.1): Classement sorted by Score Cumulé with the
 * Donneur badge, a dealing reminder for the table size, « Nouvelle manche » and « Fin de partie ».
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
    onShowStats: () -> Unit = {},
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
            ScreenHeader(
                title = stringResource(R.string.party_screen_title),
                titleTag = "party_screen_title",
                modifier = Modifier.padding(top = 8.dp)
            ) {
                if (state.playedRoundCount > 0) {
                    TextButton(
                        onClick = onShowStats,
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("party_stats_link")
                    ) {
                        Text(text = stringResource(R.string.stats_short_link, state.playedRoundCount))
                    }
                }
            }
            state.dealingRules?.let { rules ->
                DealingReminder(
                    playerCount = state.standings.size,
                    rules = rules,
                    modifier = Modifier.padding(top = 12.dp, bottom = 8.dp)
                )
            } ?: Spacer(modifier = Modifier.height(16.dp))
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
            }
            if (addition != null && addition.isChoosingPosition) {
                InsertionPositionSheet(
                    addition = addition,
                    onPositionSelected = onInsertionPositionSelected,
                    onDismiss = onAdditionCancelled
                )
            }
            if (state.closure.isConfirmationOpen) {
                EndPartyDialog(
                    isClosing = state.closure.isClosing,
                    onConfirm = onEndPartyConfirmed,
                    onDismiss = onEndPartyDismissed
                )
            }
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .testTag("party_standings")
            ) {
                items(state.standings, key = { it.participantId }) { row ->
                    StandingRowItem(
                        row = row,
                        compact = true,
                        modifier = Modifier.testTag("party_standing_row_${row.participantId}")
                    )
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
                            color = MaterialTheme.colorScheme.outline
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
internal fun EndPartyDialog(isClosing: Boolean, onConfirm: () -> Unit, onDismiss: () -> Unit) {
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

/** Rule reminder for the table size: cards per player and how to deal them, the pass, removed cards.
 * Outlined rather than filled, so it reads as a side note and not as one more standing row. */
@Composable
private fun DealingReminder(playerCount: Int, rules: DealingRules, modifier: Modifier = Modifier) {
    val lines = buildList {
        rules.removedSuitRank?.let { add(stringResource(R.string.party_rules_removed, it)) }
        add(
            if (rules.lastPacket == 0) {
                stringResource(R.string.party_rules_deal, rules.cardsPerPlayer, rules.fullPackets)
            } else {
                stringResource(R.string.party_rules_deal_with_rest, rules.cardsPerPlayer, rules.fullPackets, rules.lastPacket)
            }
        )
        add(stringResource(R.string.party_rules_pass, rules.cardsPassed))
    }
    Column(
        verticalArrangement = Arrangement.spacedBy(2.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .semantics(mergeDescendants = true) {}
            .testTag("party_dealing_reminder")
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.party_rules_emoji),
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier
                    .padding(end = 6.dp)
                    .clearAndSetSemantics {}
            )
            Text(
                text = stringResource(R.string.party_rules_title, playerCount),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.outline
            )
        }
        lines.forEach { line ->
            Text(
                text = "• $line",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CenteredMessage(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        content()
    }
}

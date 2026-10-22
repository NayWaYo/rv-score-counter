package com.rvetiveau.rvscorecount.ui.screens.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.rvetiveau.rvscorecount.R
import com.rvetiveau.rvscorecount.domain.model.PlayerSummary
import com.rvetiveau.rvscorecount.ui.components.gamePresentation
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.UUID

/** What the profile's merge and delete actions report to its ViewModel (Story 10.4). */
data class ProfileActions(
    val onMergeRequested: () -> Unit = {},
    val onMergeTargetSelected: (UUID) -> Unit = {},
    val onMergeConfirmed: () -> Unit = {},
    val onDeleteRequested: () -> Unit = {},
    val onDeleteConfirmed: () -> Unit = {},
    val onDialogDismissed: () -> Unit = {}
)

/** « Fusionner avec… », and « Supprimer ce joueur » for a Joueur without any Partie left. */
@Composable
internal fun ProfileButtons(state: PlayerProfileUiState.Loaded, actions: ProfileActions) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        if (state.otherPlayers.isNotEmpty()) {
            OutlinedButton(
                onClick = actions.onMergeRequested,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .testTag("profile_merge_button")
            ) {
                Text(text = stringResource(R.string.profile_merge_button))
            }
        }
        if (state.records.player.lastPlayedAt == null) {
            OutlinedButton(
                onClick = actions.onDeleteRequested,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .testTag("profile_delete_button")
            ) {
                Text(text = stringResource(R.string.profile_delete_button))
            }
        }
    }
}

@Composable
internal fun ProfileDialogHost(state: PlayerProfileUiState.Loaded, actions: ProfileActions) {
    val name = state.records.player.name
    when (val dialog = state.dialog) {
        null -> Unit
        ProfileDialog.ChooseMergeTarget -> ChooseMergeTargetDialog(name, state.otherPlayers, actions)
        is ProfileDialog.ConfirmMerge -> ConfirmDialog(
            title = stringResource(R.string.profile_merge_confirm_title, name, dialog.target.name),
            message = stringResource(R.string.profile_merge_confirm_message, name, dialog.target.name),
            confirmLabel = stringResource(R.string.profile_merge_confirm),
            isSaving = dialog.isSaving,
            failed = dialog.failed,
            isDestructive = false,
            onConfirm = actions.onMergeConfirmed,
            onDismiss = actions.onDialogDismissed
        )
        is ProfileDialog.MergeRefused -> {
            val formatter = remember { DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM) }
            val date = Instant.ofEpochMilli(dialog.playedAt).atZone(ZoneId.systemDefault()).format(formatter)
            AlertDialog(
                onDismissRequest = actions.onDialogDismissed,
                title = { Text(text = stringResource(R.string.profile_merge_refused_title)) },
                text = {
                    Text(
                        text = stringResource(
                            R.string.profile_merge_refused_message,
                            name,
                            dialog.target.name,
                            stringResource(gamePresentation(dialog.gameId).nameRes),
                            date
                        ),
                        modifier = Modifier.testTag("profile_merge_refused")
                    )
                },
                confirmButton = {
                    TextButton(onClick = actions.onDialogDismissed) { Text(text = stringResource(R.string.profile_merge_refused_ok)) }
                }
            )
        }
        is ProfileDialog.ConfirmDelete -> ConfirmDialog(
            title = stringResource(R.string.profile_delete_confirm_title, name),
            message = stringResource(R.string.profile_delete_confirm_message, name),
            confirmLabel = stringResource(R.string.profile_delete_confirm),
            isSaving = dialog.isSaving,
            failed = dialog.failed,
            isDestructive = true,
            onConfirm = actions.onDeleteConfirmed,
            onDismiss = actions.onDialogDismissed
        )
    }
}

@Composable
private fun ChooseMergeTargetDialog(name: String, players: List<PlayerSummary>, actions: ProfileActions) {
    AlertDialog(
        onDismissRequest = actions.onDialogDismissed,
        title = { Text(text = stringResource(R.string.profile_merge_choose_title, name)) },
        text = {
            LazyColumn(modifier = Modifier.testTag("profile_merge_targets")) {
                items(players, key = { it.id }) { player ->
                    Text(
                        text = player.name,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .clickable { actions.onMergeTargetSelected(player.id) }
                            .padding(vertical = 12.dp)
                            .testTag("profile_merge_target_${player.id}")
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = actions.onDialogDismissed) { Text(text = stringResource(R.string.profile_cancel)) }
        }
    )
}

@Composable
private fun ConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String,
    isSaving: Boolean,
    failed: Boolean,
    isDestructive: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = message)
                if (failed) {
                    Text(
                        text = stringResource(R.string.profile_action_failed),
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.testTag("profile_action_failed")
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = !isSaving,
                colors = if (isDestructive) {
                    ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                } else {
                    ButtonDefaults.buttonColors()
                },
                modifier = Modifier.testTag("profile_confirm")
            ) {
                Text(text = confirmLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSaving, modifier = Modifier.testTag("profile_dismiss")) {
                Text(text = stringResource(R.string.profile_cancel))
            }
        }
    )
}

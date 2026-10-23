package com.rvetiveau.rvscorecount.ui.screens.backup

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.rvetiveau.rvscorecount.R
import com.rvetiveau.rvscorecount.domain.model.BackupContent
import com.rvetiveau.rvscorecount.ui.components.ScreenHeader
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Stateless « Sauvegarde » (Epic 12): what the backup holds, « Exporter » (Story 12.1), « Restaurer »
 * with its confirmation (Story 12.2) and the outcome of the last operation. Both buttons are off while
 * one runs; « Exporter » also when there is nothing to save, « Restaurer » never otherwise (a new phone
 * has nothing yet).
 */
@Composable
fun BackupScreen(
    state: BackupUiState,
    modifier: Modifier = Modifier,
    onExportRequested: () -> Unit = {},
    onRestoreRequested: () -> Unit = {},
    onRestoreConfirmed: () -> Unit = {},
    onRestoreDismissed: () -> Unit = {}
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        ScreenHeader(
            title = stringResource(R.string.backup_title),
            titleTag = "backup_title",
            modifier = Modifier.padding(vertical = 8.dp)
        )
        when (state) {
            BackupUiState.Loading -> Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                CircularProgressIndicator(modifier = Modifier.testTag("backup_loading"))
            }

            BackupUiState.LoadError -> Message(stringResource(R.string.backup_load_error), "backup_load_error")

            is BackupUiState.Loaded -> {
                LoadedContent(state, onExportRequested, onRestoreRequested)
                state.pendingRestore?.let { pending ->
                    RestoreDialog(pending, onConfirm = onRestoreConfirmed, onDismiss = onRestoreDismissed)
                }
            }
        }
    }
}

@Composable
private fun LoadedContent(state: BackupUiState.Loaded, onExportRequested: () -> Unit, onRestoreRequested: () -> Unit) {
    val content = state.content
    val idle = state.operation == null
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.verticalScroll(rememberScrollState())
    ) {
        Message(stringResource(R.string.backup_intro), "backup_intro")
        Text(
            text = if (content.isEmpty) {
                stringResource(R.string.backup_empty)
            } else {
                stringResource(R.string.backup_content, playersText(content), partiesText(content))
            },
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(16.dp)
                .testTag("backup_content")
        )
        Button(
            onClick = onExportRequested,
            enabled = idle && !content.isEmpty,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .testTag("backup_export")
        ) {
            Text(text = stringResource(R.string.backup_export))
        }
        Message(stringResource(R.string.backup_restore_intro), "backup_restore_intro")
        OutlinedButton(
            onClick = onRestoreRequested,
            enabled = idle,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .testTag("backup_restore")
        ) {
            Text(text = stringResource(R.string.backup_restore))
        }
        if (!idle) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("backup_progress")
            )
        }
        state.message?.let { message ->
            Text(
                text = stringResource(message.textRes()),
                style = MaterialTheme.typography.bodyLarge,
                color = if (message.isSuccess) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                modifier = Modifier.testTag("backup_message")
            )
        }
    }
}

@Composable
private fun RestoreDialog(pending: PendingRestore, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    val dateFormatter = remember { DateTimeFormatter.ofPattern("dd/MM/yyyy") }
    val date = Instant.ofEpochMilli(pending.createdAt).atZone(ZoneId.systemDefault()).format(dateFormatter)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.backup_restore_title)) },
        text = {
            Text(
                text = stringResource(
                    R.string.backup_restore_question,
                    date,
                    playersText(pending.content),
                    partiesText(pending.content)
                ),
                modifier = Modifier.testTag("backup_restore_question")
            )
        },
        confirmButton = {
            Button(onClick = onConfirm, modifier = Modifier.testTag("backup_restore_confirm")) {
                Text(text = stringResource(R.string.backup_restore_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.testTag("backup_restore_cancel")) {
                Text(text = stringResource(R.string.backup_restore_cancel))
            }
        }
    )
}

@Composable
private fun playersText(content: BackupContent): String =
    pluralStringResource(R.plurals.backup_players, content.players, content.players)

@Composable
private fun partiesText(content: BackupContent): String =
    pluralStringResource(R.plurals.backup_parties, content.parties, content.parties)

@StringRes
private fun BackupMessage.textRes(): Int = when (this) {
    BackupMessage.EXPORTED -> R.string.backup_exported
    BackupMessage.EXPORT_FAILED -> R.string.backup_export_failed
    BackupMessage.RESTORED -> R.string.backup_restored
    BackupMessage.RESTORE_FAILED -> R.string.backup_restore_failed
    BackupMessage.NOT_A_BACKUP -> R.string.backup_not_a_backup
    BackupMessage.NEWER_FORMAT -> R.string.backup_newer_format
    BackupMessage.DAMAGED -> R.string.backup_damaged
    BackupMessage.UNREADABLE -> R.string.backup_unreadable
}

@Composable
private fun Message(text: String, tag: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.testTag(tag)
    )
}

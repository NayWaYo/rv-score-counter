package com.rvetiveau.rvscorecount.ui.screens.players

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.rvetiveau.rvscorecount.R
import com.rvetiveau.rvscorecount.domain.model.PlayerSummary
import com.rvetiveau.rvscorecount.ui.components.ScreenHeader
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.UUID

/**
 * Stateless « Joueurs » (Story 10.2): every Joueur with their finished Parties and latest Partie,
 * and a pencil that opens the rename dialog; tapping a row opens the profile (Story 10.3).
 */
@Composable
fun PlayersScreen(
    state: PlayersUiState,
    modifier: Modifier = Modifier,
    onPlayerSelected: (UUID) -> Unit = {},
    onRenameRequested: (UUID) -> Unit = {},
    onRenameInputChanged: (String) -> Unit = {},
    onRenameConfirmed: () -> Unit = {},
    onRenameDismissed: () -> Unit = {}
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        ScreenHeader(
            title = stringResource(R.string.players_title),
            titleTag = "players_title",
            modifier = Modifier.padding(vertical = 8.dp)
        )
        when (state) {
            PlayersUiState.Loading -> CenteredContent {
                CircularProgressIndicator(modifier = Modifier.testTag("players_loading"))
            }

            PlayersUiState.Empty -> CenteredContent { Message(R.string.players_empty, "players_empty") }

            PlayersUiState.LoadError -> CenteredContent { Message(R.string.players_load_error, "players_load_error") }

            is PlayersUiState.Loaded -> {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("players_list")
                ) {
                    items(state.players, key = { it.id }) { player ->
                        PlayerRow(
                            player = player,
                            onSelected = { onPlayerSelected(player.id) },
                            onRenameRequested = { onRenameRequested(player.id) }
                        )
                    }
                }
                state.renaming?.let { renaming ->
                    RenameDialog(
                        renaming = renaming,
                        onInputChanged = onRenameInputChanged,
                        onConfirm = onRenameConfirmed,
                        onDismiss = onRenameDismissed
                    )
                }
            }
        }
    }
}

@Composable
private fun PlayerRow(player: PlayerSummary, onSelected: () -> Unit, onRenameRequested: () -> Unit) {
    val dateFormatter = remember { DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM) }
    val details = player.lastPlayedAt?.let { lastPlayedAt ->
        val date = Instant.ofEpochMilli(lastPlayedAt).atZone(ZoneId.systemDefault()).format(dateFormatter)
        pluralStringResource(R.plurals.players_finished_parties, player.finishedParties, player.finishedParties) +
            " · " + stringResource(R.string.players_last_played, date)
    } ?: stringResource(R.string.players_no_party)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onSelected)
            .padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 8.dp)
            .testTag("players_row_${player.id}")
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = player.name,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.testTag("players_name_${player.id}")
            )
            Text(
                text = details,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.testTag("players_details_${player.id}")
            )
        }
        IconButton(
            onClick = onRenameRequested,
            modifier = Modifier
                .size(48.dp)
                .testTag("players_rename_${player.id}")
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_edit),
                contentDescription = stringResource(R.string.players_rename_content_description, player.name),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun RenameDialog(
    renaming: PlayerRenaming,
    onInputChanged: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.players_rename_title, renaming.currentName)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = renaming.input,
                    onValueChange = onInputChanged,
                    label = { Text(stringResource(R.string.players_rename_label)) },
                    singleLine = true,
                    isError = renaming.errorRes != null,
                    enabled = !renaming.isSaving,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("players_rename_field")
                )
                renaming.errorRes?.let { errorRes ->
                    Text(
                        text = renaming.takenBy?.let { stringResource(errorRes, it) } ?: stringResource(errorRes),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.testTag("players_rename_error")
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = !renaming.isSaving,
                modifier = Modifier.testTag("players_rename_confirm")
            ) {
                Text(text = stringResource(R.string.players_rename_confirm))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !renaming.isSaving,
                modifier = Modifier.testTag("players_rename_cancel")
            ) {
                Text(text = stringResource(R.string.players_rename_cancel))
            }
        }
    )
}

@Composable
private fun Message(textRes: Int, tag: String) {
    Text(
        text = stringResource(textRes),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.testTag(tag)
    )
}

@Composable
private fun CenteredContent(content: @Composable () -> Unit) {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) { content() }
}

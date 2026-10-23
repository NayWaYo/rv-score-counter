package com.rvetiveau.rvscorecount.ui.screens.backup

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.rvetiveau.rvscorecount.R
import com.rvetiveau.rvscorecount.ui.components.ScreenHeader

/**
 * Stateless « Sauvegarde » (Story 12.1): what the backup holds, « Exporter » and how the last
 * export went. « Exporter » is off while writing and when there is nothing to save.
 */
@Composable
fun BackupScreen(
    state: BackupUiState,
    modifier: Modifier = Modifier,
    onExportRequested: () -> Unit = {}
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
                Message(stringResource(R.string.backup_intro), "backup_intro")
                val content = state.content
                Text(
                    text = if (content.isEmpty) {
                        stringResource(R.string.backup_empty)
                    } else {
                        stringResource(
                            R.string.backup_content,
                            pluralStringResource(R.plurals.backup_players, content.players, content.players),
                            pluralStringResource(R.plurals.backup_parties, content.parties, content.parties)
                        )
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
                    enabled = !content.isEmpty && !state.isExporting,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("backup_export")
                ) {
                    Text(text = stringResource(R.string.backup_export))
                }
                when (state.exportResult) {
                    ExportResult.EXPORTED -> Text(
                        text = stringResource(R.string.backup_exported),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.testTag("backup_exported")
                    )
                    ExportResult.FAILED -> Text(
                        text = stringResource(R.string.backup_export_failed),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.testTag("backup_export_failed")
                    )
                    null -> Unit
                }
            }
        }
    }
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

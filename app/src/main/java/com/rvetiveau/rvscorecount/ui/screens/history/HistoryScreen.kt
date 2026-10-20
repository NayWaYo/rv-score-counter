package com.rvetiveau.rvscorecount.ui.screens.history

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.rvetiveau.rvscorecount.R
import com.rvetiveau.rvscorecount.ui.components.ScreenHeader
import com.rvetiveau.rvscorecount.domain.model.PartySummary
import com.rvetiveau.rvscorecount.ui.components.PartySummaryRow
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.UUID
import kotlinx.coroutines.launch

/**
 * Stateless Historique (Story 6.1): finished Parties most recent first, tap to open the detail,
 * swipe left (or the « Supprimer » accessibility action) to ask for a confirmed deletion.
 */
@Composable
fun HistoryScreen(
    state: HistoryUiState,
    modifier: Modifier = Modifier,
    onRetry: () -> Unit = {},
    onEntrySelected: (UUID) -> Unit = {},
    onDeleteRequested: (UUID) -> Unit = {},
    onDeleteConfirmed: () -> Unit = {},
    onDeleteDismissed: () -> Unit = {}
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        ScreenHeader(
            title = stringResource(R.string.history_title),
            titleTag = "history_title",
            modifier = Modifier.padding(vertical = 8.dp)
        )
        when (state) {
            is HistoryUiState.Loading -> CenteredContent {
                CircularProgressIndicator(modifier = Modifier.testTag("history_loading"))
            }

            is HistoryUiState.Empty -> CenteredContent {
                Text(
                    text = stringResource(R.string.history_empty),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.testTag("history_empty")
                )
            }

            is HistoryUiState.LoadError -> CenteredContent {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = stringResource(R.string.history_load_error),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.testTag("history_load_error")
                    )
                    Button(onClick = onRetry, modifier = Modifier.padding(top = 16.dp)) {
                        Text(text = stringResource(R.string.root_error_retry))
                    }
                }
            }

            is HistoryUiState.Loaded -> {
                state.deleteErrorRes?.let { errorRes ->
                    Text(
                        text = stringResource(errorRes),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier
                            .padding(bottom = 8.dp)
                            .testTag("history_delete_error")
                    )
                }
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("history_list")
                ) {
                    items(state.entries, key = { it.partyId }) { entry ->
                        SwipeToDeleteEntry(
                            entry = entry,
                            onSelected = { onEntrySelected(entry.partyId) },
                            onDeleteRequested = { onDeleteRequested(entry.partyId) }
                        )
                    }
                }
                if (state.pendingDeletionId != null) {
                    DeletePartyDialog(
                        isDeleting = state.isDeleting,
                        onConfirm = onDeleteConfirmed,
                        onDismiss = onDeleteDismissed
                    )
                }
            }
        }
    }
}

/** The swipe only asks for deletion: the row snaps back and the dialog decides. */
@Composable
private fun SwipeToDeleteEntry(entry: PartySummary, onSelected: () -> Unit, onDeleteRequested: () -> Unit) {
    val dismissState = rememberSwipeToDismissBoxState()
    val scope = rememberCoroutineScope()
    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        onDismiss = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) onDeleteRequested()
            scope.launch { dismissState.reset() }
        },
        backgroundContent = {
            Box(
                contentAlignment = Alignment.CenterEnd,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.error)
                    .padding(horizontal = 16.dp)
            ) {
                Text(
                    text = stringResource(R.string.history_delete_action),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onError
                )
            }
        }
    ) {
        PartySummaryRow(
            entry = entry,
            onSelected = onSelected,
            onDeleteRequested = onDeleteRequested,
            modifier = Modifier.testTag("history_entry_${entry.partyId}")
        )
    }
}

@Composable
private fun DeletePartyDialog(isDeleting: Boolean, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.history_delete_dialog_title)) },
        text = { Text(text = stringResource(R.string.history_delete_dialog_message)) },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = !isDeleting,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.testTag("history_delete_confirm_button")
            ) {
                Text(text = stringResource(R.string.history_delete_action))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isDeleting,
                modifier = Modifier.testTag("history_delete_cancel_button")
            ) {
                Text(text = stringResource(R.string.history_delete_cancel))
            }
        }
    )
}

@Composable
private fun CenteredContent(content: @Composable () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        content()
    }
}

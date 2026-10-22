package com.rvetiveau.rvscorecount.ui.screens.round

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.rvetiveau.rvscorecount.R
import java.util.UUID

/** What the score form and [TrumpScanDialog] report to the screen's ViewModel (Story 9.1). */
data class TrumpScanActions(
    val onScanRequested: (UUID) -> Unit = {},
    val onTrumpToggled: (Int) -> Unit = {},
    val onRetake: () -> Unit = {},
    val onDismiss: () -> Unit = {},
    val onConfirm: () -> Unit = {}
)

/**
 * Confirmation of the trumps read on a photo (Story 9.1): every trump of [trumps] as a chip to
 * check or uncheck, the ones read already checked. A trump already counted for another
 * Participant shows their name and, while checked, blocks « Valider ».
 */
@Composable
fun TrumpScanDialog(
    scan: TrumpScan,
    participantName: String,
    trumps: IntRange,
    takenByOthers: Map<Int, String>,
    sevenPoints: Int?,
    onTrumpToggled: (Int) -> Unit,
    onRetake: () -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val conflicts = scan.selected.filter { it in takenByOthers }.sorted()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.trump_scan_title, participantName)) },
        text = {
            if (scan.isReading) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.testTag("trump_scan_reading")
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(32.dp))
                    Text(text = stringResource(R.string.trump_scan_reading), style = MaterialTheme.typography.bodyLarge)
                }
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    val messageRes = when {
                        scan.readFailed -> R.string.trump_scan_failed
                        scan.nothingRecognized -> R.string.trump_scan_nothing
                        else -> R.string.trump_scan_hint
                    }
                    Text(
                        text = stringResource(messageRes),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.testTag("trump_scan_message")
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.testTag("trump_scan_trumps")
                    ) {
                        trumps.forEach { trump ->
                            TrumpChip(
                                trump = trump,
                                selected = trump in scan.selected,
                                takenBy = takenByOthers[trump],
                                onClick = { onTrumpToggled(trump) }
                            )
                        }
                    }
                    conflicts.forEach { trump ->
                        Text(
                            text = stringResource(R.string.trump_scan_conflict, trump, takenByOthers.getValue(trump)),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.testTag("trump_scan_conflict_$trump")
                        )
                    }
                    Text(
                        text = if (scan.includesSeven && sevenPoints != null) {
                            stringResource(R.string.trump_scan_score_with_seven, scan.score, scan.selected.sum(), sevenPoints)
                        } else {
                            stringResource(R.string.trump_scan_score, scan.score)
                        },
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.testTag("trump_scan_score")
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = !scan.isReading && conflicts.isEmpty(),
                modifier = Modifier.testTag("trump_scan_confirm")
            ) {
                Text(text = stringResource(R.string.trump_scan_confirm))
            }
        },
        dismissButton = {
            Row {
                TextButton(
                    onClick = onRetake,
                    enabled = !scan.isReading,
                    modifier = Modifier.testTag("trump_scan_retake")
                ) {
                    Text(text = stringResource(R.string.trump_scan_retake))
                }
                TextButton(onClick = onDismiss, modifier = Modifier.testTag("trump_scan_cancel")) {
                    Text(text = stringResource(R.string.trump_scan_cancel))
                }
            }
        },
        modifier = Modifier.testTag("trump_scan_dialog")
    )
}

@Composable
private fun TrumpChip(trump: Int, selected: Boolean, takenBy: String?, onClick: () -> Unit) {
    val conflicting = selected && takenBy != null
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Text(
                text = takenBy?.let { stringResource(R.string.trump_scan_taken_label, trump, it) } ?: trump.toString(),
                style = MaterialTheme.typography.bodyLarge
            )
        },
        colors = if (conflicting) {
            FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.errorContainer,
                selectedLabelColor = MaterialTheme.colorScheme.onErrorContainer
            )
        } else {
            FilterChipDefaults.filterChipColors()
        },
        modifier = Modifier
            .heightIn(min = 48.dp)
            .testTag("trump_scan_trump_$trump")
    )
}

package com.rvetiveau.rvscorecount.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.rvetiveau.rvscorecount.R
import com.rvetiveau.rvscorecount.domain.model.PartySummary
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * One finished Partie: date · Jeu · nombre de Manches, then the winner(s) or why there is none.
 * Tapping opens it; [onDeleteRequested], when given, adds a « Supprimer » accessibility action.
 */
@Composable
fun PartySummaryRow(
    entry: PartySummary,
    onSelected: () -> Unit,
    modifier: Modifier = Modifier,
    onDeleteRequested: (() -> Unit)? = null
) {
    val dateFormatter = remember { DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM) }
    val date = Instant.ofEpochMilli(entry.createdAt).atZone(ZoneId.systemDefault()).format(dateFormatter)
    val deleteLabel = stringResource(R.string.history_delete_action)
    val outcome = when {
        !entry.isConsistent -> stringResource(R.string.history_inconsistent)
        entry.teamScores.size == 2 -> stringResource(
            R.string.belote_history_outcome,
            entry.teamScores[0].name,
            entry.teamScores[0].points,
            entry.teamScores[1].name,
            entry.teamScores[1].points,
            entry.winners.joinToString(", ")
        )
        entry.winningScore == null -> stringResource(R.string.history_no_round)
        else -> stringResource(
            R.string.history_winner,
            pluralStringResource(R.plurals.results_winner_label, entry.winners.size),
            entry.winners.joinToString(", "),
            pluralStringResource(R.plurals.results_winner_score, entry.winningScore, entry.winningScore)
        )
    }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onSelected)
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .semantics(mergeDescendants = true) {
                if (onDeleteRequested != null) {
                    customActions = listOf(CustomAccessibilityAction(deleteLabel) { onDeleteRequested(); true })
                }
            }
    ) {
        Text(
            text = stringResource(
                R.string.history_entry_header,
                date,
                stringResource(gamePresentation(entry.gameId).nameRes),
                pluralStringResource(R.plurals.history_round_count, entry.roundCount, entry.roundCount)
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = outcome,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

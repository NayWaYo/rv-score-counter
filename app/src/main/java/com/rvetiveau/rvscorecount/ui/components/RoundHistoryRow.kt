package com.rvetiveau.rvscorecount.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import com.rvetiveau.rvscorecount.R
import com.rvetiveau.rvscorecount.domain.model.CardSuit
import com.rvetiveau.rvscorecount.ui.theme.color
import com.rvetiveau.rvscorecount.ui.theme.nameRes
import com.rvetiveau.rvscorecount.ui.theme.pictogramRes
import com.rvetiveau.rvscorecount.ui.theme.tint

/** One validated Manche, scores in table order (only the Participants who played it). */
data class RoundHistoryItem(
    val roundNumber: Int,
    val suit: CardSuit,
    val scores: List<RoundHistoryScore>,
    val total: Int
)

data class RoundHistoryScore(
    val participantName: String,
    val points: Int
)

/**
 * Read-only round-history-row (UX DESIGN) : fond `suit-*-tint`, numéro de Manche, scores et total,
 * pictogramme de l'enseigne à droite. Lu d'un bloc par TalkBack.
 */
@Composable
fun RoundHistoryRow(item: RoundHistoryItem, modifier: Modifier = Modifier) {
    val scoreSeparator = stringResource(R.string.round_history_score_separator)
    val scoresText = item.scores
        .map { score -> stringResource(R.string.round_history_score, score.participantName, score.points) }
        .joinToString(scoreSeparator)
    val description = stringResource(
        R.string.round_history_content_description,
        item.roundNumber,
        stringResource(item.suit.nameRes),
        scoresText,
        item.total
    )
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(item.suit.tint)
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clearAndSetSemantics { contentDescription = description }
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.round_history_title, item.roundNumber),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = scoresText,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = stringResource(R.string.round_history_total, item.total),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = stringResource(item.suit.pictogramRes),
            style = MaterialTheme.typography.headlineSmall,
            color = item.suit.color,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}

package com.rvetiveau.rvscorecount.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.rvetiveau.rvscorecount.R
import com.rvetiveau.rvscorecount.domain.model.Standing
import java.util.UUID

data class StandingRow(
    val participantId: UUID,
    val rank: Int,
    val name: String,
    val cumulativeScore: Int,
    val isDealer: Boolean
)

/**
 * Lignes d'affichage d'un classement déjà trié par [com.rvetiveau.rvscorecount.domain.model.computeStandings].
 * Rang « 1, 1, 3 » : un ex æquo prend le rang du premier de son score.
 */
fun List<Standing>.toStandingRows(dealerId: UUID? = null): List<StandingRow> = map { standing ->
    StandingRow(
        participantId = standing.participant.id,
        rank = indexOfFirst { it.cumulativeScore == standing.cumulativeScore } + 1,
        name = standing.participant.name,
        cumulativeScore = standing.cumulativeScore,
        isDealer = standing.participant.id == dealerId
    )
}

/**
 * scoreboard-row : rang (meta), nom + badge Donneur (body), Score Cumulé (title), 56dp minimum.
 * [highlightRank] accentue le rang (podium des Résultats finaux).
 */
@Composable
fun StandingRowItem(row: StandingRow, modifier: Modifier = Modifier, highlightRank: Boolean = false) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .semantics(mergeDescendants = true) {}
    ) {
        Text(
            text = stringResource(R.string.party_standing_rank, row.rank),
            style = if (highlightRank) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodySmall,
            color = if (highlightRank) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.widthIn(min = 32.dp)
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp)
        ) {
            Text(
                text = row.name,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f, fill = false)
            )
            if (row.isDealer) {
                Text(
                    text = stringResource(R.string.party_dealer_badge),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primary)
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                        .testTag("dealer_badge")
                )
            }
        }
        Text(
            text = row.cumulativeScore.toString(),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

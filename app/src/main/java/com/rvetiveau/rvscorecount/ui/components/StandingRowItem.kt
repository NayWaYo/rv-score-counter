package com.rvetiveau.rvscorecount.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.rvetiveau.rvscorecount.R
import com.rvetiveau.rvscorecount.domain.model.PlayerStats
import com.rvetiveau.rvscorecount.domain.model.Standing
import java.util.UUID

/** [stats] is null when the Participant has not played a Manche, or the screen does not show them. */
data class StandingRow(
    val participantId: UUID,
    val rank: Int,
    val name: String,
    val cumulativeScore: Int,
    val isDealer: Boolean,
    val stats: PlayerStats? = null
)

/**
 * Lignes d'affichage d'un classement déjà trié par [com.rvetiveau.rvscorecount.domain.model.computeStandings].
 * Rang « 1, 1, 3 » : un ex æquo prend le rang du premier de son score.
 */
fun List<Standing>.toStandingRows(
    dealerId: UUID? = null,
    stats: Map<UUID, PlayerStats> = emptyMap()
): List<StandingRow> = map { standing ->
    StandingRow(
        participantId = standing.participant.id,
        rank = indexOfFirst { it.cumulativeScore == standing.cumulativeScore } + 1,
        name = standing.participant.name,
        cumulativeScore = standing.cumulativeScore,
        isDealer = standing.participant.id == dealerId,
        stats = stats[standing.participant.id]
    )
}

/**
 * scoreboard-row : rang (meta), nom + badge Donneur (body), Score Cumulé (title), 56dp minimum.
 * [highlightRank] accentue le rang (podium des Résultats finaux). With [StandingRow.stats], a tap
 * unfolds the Participant's statistics under the row and a chevron shows it can.
 */
@Composable
fun StandingRowItem(row: StandingRow, modifier: Modifier = Modifier, highlightRank: Boolean = false) {
    var expanded by rememberSaveable(row.participantId) { mutableStateOf(false) }
    val stats = row.stats
    val stateText = when {
        stats == null -> null
        expanded -> stringResource(R.string.standing_state_expanded)
        else -> stringResource(R.string.standing_state_collapsed)
    }
    val clickLabel = stringResource(if (expanded) R.string.standing_collapse else R.string.standing_expand)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .let { base ->
                if (stats != null) base.clickable(onClickLabel = clickLabel) { expanded = !expanded } else base
            }
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .semantics(mergeDescendants = true) { stateText?.let { stateDescription = it } }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.heightIn(min = 40.dp)
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
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
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
            if (stats != null) {
                Icon(
                    painter = painterResource(R.drawable.ic_expand),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .size(24.dp)
                        .rotate(if (expanded) 180f else 0f)
                )
            }
        }
        if (stats != null) {
            AnimatedVisibility(visible = expanded) {
                PlayerStatsGrid(stats, modifier = Modifier.testTag("standing_stats_${row.participantId}"))
            }
        }
    }
}

/** The Participant's figures as small label-over-value cells that wrap on narrow screens. */
@Composable
private fun PlayerStatsGrid(stats: PlayerStats, modifier: Modifier = Modifier) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 4.dp)
    ) {
        StatCell(
            label = stringResource(R.string.player_stats_average_label),
            value = stringResource(R.string.player_stats_average_value, stats.averagePoints)
        )
        StatCell(
            label = stringResource(R.string.player_stats_zero_label),
            value = stats.zeroRounds.toString()
        )
        stats.sevensTaken?.let { sevens ->
            StatCell(label = stringResource(R.string.player_stats_sevens_label), value = sevens.toString())
        }
        StatCell(
            label = stringResource(R.string.player_stats_best_label),
            value = stringResource(R.string.player_stats_round_value, stats.best.points, stats.best.roundNumber)
        )
        StatCell(
            label = stringResource(R.string.player_stats_worst_label),
            value = stringResource(R.string.player_stats_round_value, stats.worst.points, stats.worst.roundNumber)
        )
    }
}

@Composable
private fun StatCell(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

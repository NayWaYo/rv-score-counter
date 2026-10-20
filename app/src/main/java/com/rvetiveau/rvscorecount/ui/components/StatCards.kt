package com.rvetiveau.rvscorecount.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rvetiveau.rvscorecount.R
import com.rvetiveau.rvscorecount.domain.model.RoundRecord
import com.rvetiveau.rvscorecount.domain.model.SevenChampion
import com.rvetiveau.rvscorecount.ui.theme.ChampionInk
import com.rvetiveau.rvscorecount.ui.theme.ChampionTint
import com.rvetiveau.rvscorecount.ui.theme.RecordInk
import com.rvetiveau.rvscorecount.ui.theme.RecordTint

/** Display model of the « Poissard ou Nul ? » statistic (ties included). */
data class SevenChampionUi(
    val names: List<String>,
    val count: Int
)

fun SevenChampion.toUi() = SevenChampionUi(names = participants.map { it.name }, count = count)

/** Display model of the « La fusée » statistic (ties included). */
data class RoundRecordUi(
    val entries: List<RoundRecordEntryUi>,
    val points: Int
)

data class RoundRecordEntryUi(
    val participantName: String,
    val roundNumber: Int
)

fun RoundRecord.toUi() = RoundRecordUi(
    entries = entries.map { RoundRecordEntryUi(it.participant.name, it.roundNumber) },
    points = points
)

/**
 * The Partie's statistics as side-by-side cards of equal height, each on its own tint; a statistic
 * that does not exist yet (no 7 holder, no Manche) has no card, and nothing is drawn when neither
 * exists. The holders' names come first, the figure below them.
 */
@Composable
fun StatCards(
    sevenChampion: SevenChampionUi?,
    roundRecord: RoundRecordUi?,
    modifier: Modifier = Modifier
) {
    if (sevenChampion == null && roundRecord == null) return
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
    ) {
        sevenChampion?.let { champion ->
            val names = champion.names.joinToString(", ")
            StatCard(
                emoji = stringResource(R.string.stat_seven_champion_emoji),
                label = stringResource(R.string.stat_seven_champion_label),
                names = names,
                value = stringResource(R.string.stat_seven_champion_value, champion.count),
                description = stringResource(R.string.seven_champion, names, champion.count),
                tint = ChampionTint,
                ink = ChampionInk,
                modifier = Modifier
                    .weight(1f)
                    .testTag("stat_card_seven_champion")
            )
        }
        roundRecord?.let { record ->
            val holders = record.entries
                .map { stringResource(R.string.round_record_entry, it.participantName, it.roundNumber) }
                .joinToString(", ")
            val roundNumbers = record.entries.map { it.roundNumber }.distinct()
            StatCard(
                emoji = stringResource(R.string.stat_round_record_emoji),
                label = stringResource(R.string.stat_round_record_label),
                names = record.entries.map { it.participantName }.distinct().joinToString(", "),
                value = stringResource(
                    R.string.stat_round_record_value,
                    record.points,
                    pluralStringResource(R.plurals.stat_round_record_rounds, roundNumbers.size, roundNumbers.joinToString(", "))
                ),
                description = stringResource(R.string.round_record, holders, record.points),
                tint = RecordTint,
                ink = RecordInk,
                modifier = Modifier
                    .weight(1f)
                    .testTag("stat_card_round_record")
            )
        }
    }
}

/** One statistic: emoji and label in the card's ink, the holders' names in large type, then the figure. */
@Composable
private fun StatCard(
    emoji: String,
    label: String,
    names: String,
    value: String,
    description: String,
    tint: Color,
    ink: Color,
    modifier: Modifier
) {
    val shape = RoundedCornerShape(12.dp)
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier
            .fillMaxHeight()
            .clip(shape)
            .background(tint)
            .border(1.dp, ink.copy(alpha = 0.4f), shape)
            .padding(12.dp)
            .clearAndSetSemantics { contentDescription = description }
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = emoji, style = MaterialTheme.typography.titleLarge)
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = ink,
                modifier = Modifier.padding(start = 6.dp)
            )
        }
        Text(
            text = names,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = ink
        )
    }
}

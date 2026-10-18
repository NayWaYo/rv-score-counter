package com.rvetiveau.rvscorecount.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.rvetiveau.rvscorecount.R
import com.rvetiveau.rvscorecount.domain.model.RoundRecord

/** Display model of the « Record en une manche » statistic (ties included). */
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

/** « Record en une manche : Bob (manche 4) — 112 points ». */
@Composable
fun RoundRecordLine(record: RoundRecordUi, modifier: Modifier = Modifier) {
    val holders = record.entries
        .map { stringResource(R.string.round_record_entry, it.participantName, it.roundNumber) }
        .joinToString(", ")
    Text(
        text = stringResource(R.string.round_record, holders, record.points),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = modifier
    )
}

package com.rvetiveau.rvscorecount.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rvetiveau.rvscorecount.R
import com.rvetiveau.rvscorecount.ui.theme.RvTheme

/** A player's takes in a Partie and how many contracts they made (Belote, Tarot). */
data class TakerUiModel(val name: String, val takes: Int, val made: Int)

/**
 * « 🗿 Chad de la game » (Stories 8.3, 13.4): on the lagoon tint, in the look of the Papayoo statistic cards,
 * the player who took the most in large type, then every player ranked by takes.
 */
@Composable
fun ChadCard(takers: List<TakerUiModel>) {
    val ink = RvTheme.colors.chadInk
    val shape = RoundedCornerShape(12.dp)
    val lines = takers.map { taker ->
        stringResource(
            R.string.stat_chad_value,
            pluralStringResource(R.plurals.stat_chad_takes, taker.takes, taker.takes),
            pluralStringResource(R.plurals.stat_chad_made, taker.made, taker.made)
        )
    }
    val chad = takers.first()
    val description = stringResource(
        R.string.stat_chad_content_description,
        chad.name,
        takers.mapIndexed { index, taker -> stringResource(R.string.stat_chad_rank, index + 1, "${taker.name}, ${lines[index]}") }
            .joinToString(" ; ")
    )
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(RvTheme.colors.chadTint)
            .border(1.dp, ink.copy(alpha = 0.4f), shape)
            .padding(12.dp)
            .testTag("stat_card_chad")
            .clearAndSetSemantics { contentDescription = description }
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = stringResource(R.string.stat_chad_emoji), style = MaterialTheme.typography.titleLarge)
            Text(
                text = stringResource(R.string.stat_chad_label),
                style = MaterialTheme.typography.labelLarge,
                color = ink,
                modifier = Modifier.padding(start = 6.dp)
            )
        }
        Text(
            text = chad.name,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(text = lines.first(), style = MaterialTheme.typography.bodyMedium, color = ink)
        takers.forEachIndexed { index, taker ->
            Row(modifier = Modifier.padding(top = if (index == 0) 8.dp else 0.dp)) {
                Text(
                    text = stringResource(R.string.stat_chad_rank, index + 1, taker.name),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Text(text = lines[index], style = MaterialTheme.typography.bodyMedium, color = ink)
            }
        }
    }
}

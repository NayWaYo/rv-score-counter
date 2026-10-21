package com.rvetiveau.rvscorecount.ui.screens.belote

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rvetiveau.rvscorecount.R
import com.rvetiveau.rvscorecount.ui.components.ScoreChart
import com.rvetiveau.rvscorecount.ui.components.ScreenHeader
import com.rvetiveau.rvscorecount.ui.theme.RvTheme

/** Stateless Statistiques of a Partie of Belote (Story 7.4, AC 3): the cumulated score of each Équipe. */
@Composable
fun BeloteStatsScreen(state: BeloteStatsUiState, modifier: Modifier = Modifier) {
    when (state) {
        BeloteStatsUiState.Loading -> Centered(modifier) {
            CircularProgressIndicator(modifier = Modifier.testTag("belote_stats_loading"))
        }
        BeloteStatsUiState.Unavailable -> Centered(modifier) {
            Text(text = stringResource(R.string.party_not_found), modifier = Modifier.testTag("belote_stats_unavailable"))
        }
        is BeloteStatsUiState.Loaded -> Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            ScreenHeader(
                title = stringResource(R.string.stats_title),
                titleTag = "belote_stats_title",
                modifier = Modifier.padding(top = 8.dp)
            )
            Text(text = stringResource(R.string.belote_stats_curve_title), style = MaterialTheme.typography.titleMedium)
            if (state.roundCount == 0) {
                Text(
                    text = stringResource(R.string.belote_stats_no_round),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.testTag("belote_stats_no_round")
                )
            } else {
                ScoreChart(series = state.series, modifier = Modifier.testTag("belote_stats_chart"))
            }
            if (state.takers.isNotEmpty()) ChadCard(state.takers)
        }
    }
}

/**
 * « 🗿 Chad de la game » (Story 8.3): on the lagoon tint, in the look of the Papayoo statistic cards,
 * the player who took the most in large type, then every player ranked by takes.
 */
@Composable
private fun ChadCard(takers: List<BeloteTakerUiModel>) {
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

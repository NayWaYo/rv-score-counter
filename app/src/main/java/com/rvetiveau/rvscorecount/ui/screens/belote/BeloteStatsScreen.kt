package com.rvetiveau.rvscorecount.ui.screens.belote

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.rvetiveau.rvscorecount.R
import com.rvetiveau.rvscorecount.ui.components.ChadCard
import com.rvetiveau.rvscorecount.ui.components.ScoreChart
import com.rvetiveau.rvscorecount.ui.components.ScreenHeader

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

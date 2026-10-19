package com.rvetiveau.rvscorecount.ui.screens.results

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.rvetiveau.rvscorecount.R
import com.rvetiveau.rvscorecount.ui.components.RoundTable
import com.rvetiveau.rvscorecount.ui.components.ScoreChart
import com.rvetiveau.rvscorecount.ui.components.StatCards
import com.rvetiveau.rvscorecount.ui.components.StandingRowItem
import com.rvetiveau.rvscorecount.ui.components.WinnerBanner

/**
 * Stateless Résultats finaux (Story 5.1, FR-13): winner banner, full final Classement with the
 * podium ranks accented, statistic cards, the score sheet of the Manches, « Rejouer » (Story 5.2,
 * FR-14) and « Retour à l'accueil ».
 */
@Composable
fun ResultsScreen(
    state: ResultsUiState,
    modifier: Modifier = Modifier,
    onRetry: () -> Unit = {},
    onBackToHome: () -> Unit = {},
    onReplay: () -> Unit = {}
) {
    when (state) {
        is ResultsUiState.Loading -> CenteredMessage(modifier) {
            CircularProgressIndicator(modifier = Modifier.testTag("results_loading"))
        }

        is ResultsUiState.LoadError -> CenteredMessage(modifier) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = stringResource(R.string.results_load_error),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.testTag("results_load_error")
                )
                Button(onClick = onRetry, modifier = Modifier.padding(top = 16.dp)) {
                    Text(text = stringResource(R.string.root_error_retry))
                }
            }
        }

        is ResultsUiState.Unavailable -> CenteredMessage(modifier) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = stringResource(R.string.results_unavailable),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.testTag("results_unavailable")
                )
                OutlinedButton(onClick = onBackToHome, modifier = Modifier.padding(top = 16.dp)) {
                    Text(text = stringResource(R.string.results_back_home))
                }
            }
        }

        is ResultsUiState.Loaded -> Column(
            modifier = modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text(
                text = stringResource(R.string.results_title),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier
                    .padding(top = 16.dp, bottom = 16.dp)
                    .testTag("results_title")
            )
            if (state.winners.isNotEmpty()) {
                WinnerBanner(state.winners, modifier = Modifier.testTag("results_winner"))
            }
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(top = 16.dp)
                    .testTag("results_standings")
            ) {
                items(state.standings, key = { it.participantId }) { row ->
                    StandingRowItem(
                        row = row,
                        highlightRank = row.rank <= 3,
                        modifier = Modifier.testTag("results_standing_row_${row.participantId}")
                    )
                }
                if (state.sevenChampion != null || state.roundRecord != null) {
                    item(key = "stats") {
                        StatCards(
                            sevenChampion = state.sevenChampion,
                            roundRecord = state.roundRecord,
                            modifier = Modifier
                                .padding(top = 8.dp)
                                .testTag("results_stats")
                        )
                    }
                }
                if (state.roundTable.rows.size >= 2) {
                    item(key = "score_chart") {
                        Column {
                            Text(
                                text = stringResource(R.string.score_chart_title),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                            )
                            ScoreChart(table = state.roundTable, modifier = Modifier.testTag("results_score_chart"))
                        }
                    }
                }
                if (state.roundTable.rows.isNotEmpty()) {
                    item(key = "rounds_title") {
                        Text(
                            text = stringResource(R.string.party_rounds_title),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.padding(top = 16.dp)
                        )
                    }
                    item(key = "round_table") {
                        RoundTable(table = state.roundTable, modifier = Modifier.testTag("results_round_table"))
                    }
                }
            }
            state.replayErrorRes?.let { errorRes ->
                Text(
                    text = stringResource(errorRes),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .testTag("results_replay_error")
                )
            }
            Button(
                onClick = onReplay,
                enabled = !state.isReplaying,
                modifier = Modifier
                    .padding(top = 16.dp)
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .testTag("results_replay_button")
            ) {
                Text(text = stringResource(R.string.results_replay))
            }
            OutlinedButton(
                onClick = onBackToHome,
                modifier = Modifier
                    .padding(top = 8.dp)
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .testTag("results_back_home_button")
            ) {
                Text(text = stringResource(R.string.results_back_home))
            }
        }
    }
}

@Composable
private fun CenteredMessage(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        content()
    }
}

package com.rvetiveau.rvscorecount.ui.screens.stats

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.rvetiveau.rvscorecount.R
import com.rvetiveau.rvscorecount.ui.components.ScreenHeader
import com.rvetiveau.rvscorecount.ui.components.RoundTable
import com.rvetiveau.rvscorecount.ui.components.ScoreChart
import com.rvetiveau.rvscorecount.ui.components.StandingRowItem
import com.rvetiveau.rvscorecount.ui.components.StatCards

/**
 * Stateless Statistiques screen, the single place for a Partie's statistics: Classement whose rows
 * unfold each Participant's statistics, statistic cards, score chart and score sheet. While the
 * Partie is in progress, touching a Manche of the sheet asks for its correction.
 */
@Composable
fun StatsScreen(
    state: StatsUiState,
    modifier: Modifier = Modifier,
    onRetry: () -> Unit = {},
    onRoundSelected: (Int) -> Unit = {}
) {
    when (state) {
        is StatsUiState.Loading -> CenteredContent(modifier) {
            CircularProgressIndicator(modifier = Modifier.testTag("stats_loading"))
        }

        is StatsUiState.LoadError -> CenteredContent(modifier) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = stringResource(R.string.stats_load_error),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.testTag("stats_load_error")
                )
                Button(onClick = onRetry, modifier = Modifier.padding(top = 16.dp)) {
                    Text(text = stringResource(R.string.root_error_retry))
                }
            }
        }

        is StatsUiState.Unavailable -> CenteredContent(modifier) {
            Text(
                text = stringResource(R.string.stats_unavailable),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.testTag("stats_unavailable")
            )
        }

        is StatsUiState.Loaded -> LoadedContent(state, onRoundSelected, modifier)
    }
}

@Composable
private fun LoadedContent(state: StatsUiState.Loaded, onRoundSelected: (Int) -> Unit, modifier: Modifier) {
    val hasRounds = state.roundTable.rows.isNotEmpty()
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("stats_content")
    ) {
        item(key = "title") {
            ScreenHeader(
                title = stringResource(R.string.stats_title),
                titleTag = "stats_title",
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }
        item(key = "standings_title") { Heading(stringResource(R.string.stats_standings_title)) }
        items(state.standings, key = { it.participantId }) { row ->
            StandingRowItem(
                row = row,
                highlightRank = hasRounds && row.rank <= 3,
                modifier = Modifier.testTag("stats_standing_row_${row.participantId}")
            )
        }
        if (state.sevenChampion != null || state.roundRecord != null) {
            item(key = "stats") {
                StatCards(
                    sevenChampion = state.sevenChampion,
                    roundRecord = state.roundRecord,
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .testTag("stats_cards")
                )
            }
        }
        if (state.roundTable.rows.size >= 2) {
            item(key = "score_chart") {
                Column {
                    Heading(stringResource(R.string.score_chart_title))
                    ScoreChart(table = state.roundTable, modifier = Modifier.testTag("stats_score_chart"))
                }
            }
        }
        item(key = "rounds_title") { Heading(stringResource(R.string.party_rounds_title)) }
        if (!hasRounds) {
            item(key = "no_round") {
                Text(
                    text = stringResource(R.string.history_no_round),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.testTag("stats_no_round")
                )
            }
        } else {
            if (state.canCorrectRounds) {
                item(key = "correction_hint") {
                    Text(
                        text = stringResource(R.string.stats_correction_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.testTag("stats_correction_hint")
                    )
                }
            }
            item(key = "round_table") {
                RoundTable(
                    table = state.roundTable,
                    onRoundClick = onRoundSelected.takeIf { state.canCorrectRounds },
                    modifier = Modifier
                        .padding(bottom = 32.dp)
                        .testTag("stats_round_table")
                )
            }
        }
    }
}

@Composable
private fun Heading(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp)
    )
}

@Composable
private fun CenteredContent(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        content()
    }
}

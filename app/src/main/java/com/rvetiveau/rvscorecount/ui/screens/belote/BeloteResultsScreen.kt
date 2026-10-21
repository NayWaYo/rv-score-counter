package com.rvetiveau.rvscorecount.ui.screens.belote

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import com.rvetiveau.rvscorecount.ui.components.ScreenHeader

/** Stateless Résultats of a Partie of Belote (Story 7.3, AC 6): the winning Équipe first, then « Rejouer ». */
@Composable
fun BeloteResultsScreen(
    state: BeloteResultsUiState,
    modifier: Modifier = Modifier,
    onReplay: () -> Unit = {},
    onBackToHome: () -> Unit = {},
    onShowStats: () -> Unit = {}
) {
    when (state) {
        BeloteResultsUiState.Loading -> Centered(modifier) {
            CircularProgressIndicator(modifier = Modifier.testTag("belote_results_loading"))
        }
        BeloteResultsUiState.Unavailable -> Centered(modifier) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = stringResource(R.string.results_unavailable), modifier = Modifier.testTag("belote_results_unavailable"))
                OutlinedButton(onClick = onBackToHome, modifier = Modifier.padding(top = 16.dp)) {
                    Text(text = stringResource(R.string.results_back_home))
                }
            }
        }
        is BeloteResultsUiState.Loaded -> Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            ScreenHeader(
                title = stringResource(R.string.results_title),
                titleTag = "belote_results_title",
                modifier = Modifier.padding(top = 8.dp)
            )
            Text(
                text = stringResource(R.string.belote_results_winner, state.teams.first().name),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.testTag("belote_results_winner")
            )
            state.teams.forEach { team -> ResultCard(team) }
            Spacer(modifier = Modifier.weight(1f))
            state.replayErrorRes?.let { errorRes ->
                Text(
                    text = stringResource(errorRes),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.testTag("belote_results_replay_error")
                )
            }
            Button(
                onClick = onReplay,
                enabled = !state.isReplaying,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .testTag("belote_results_replay")
            ) {
                Text(text = stringResource(R.string.results_replay))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                if (state.roundCount > 0) {
                    OutlinedButton(
                        onClick = onShowStats,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 56.dp)
                            .testTag("belote_results_stats")
                    ) {
                        Text(text = stringResource(R.string.stats_short_link, state.roundCount))
                    }
                }
                OutlinedButton(
                    onClick = onBackToHome,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 56.dp)
                        .testTag("belote_results_home")
                ) {
                    Text(text = stringResource(R.string.results_home_short))
                }
            }
        }
    }
}

@Composable
internal fun ResultCard(team: BeloteTeamUiModel) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = if (team.isLeader) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("belote_results_team_${team.side.name}")
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(16.dp)) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = team.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = team.playerNames.joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
            Text(
                text = stringResource(R.string.belote_results_team_score, team.total),
                style = MaterialTheme.typography.titleLarge,
                color = if (team.isLeader) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

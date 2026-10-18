package com.rvetiveau.rvscorecount.ui.screens.results

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.rvetiveau.rvscorecount.R
import com.rvetiveau.rvscorecount.ui.components.StandingRow
import com.rvetiveau.rvscorecount.ui.components.StandingRowItem

/**
 * Stateless Résultats finaux (Story 5.1, FR-13): winner banner, full final Classement with the
 * podium ranks accented, « Rejouer » (Story 5.2) and « Retour à l'accueil ».
 */
@Composable
fun ResultsScreen(
    state: ResultsUiState,
    modifier: Modifier = Modifier,
    onRetry: () -> Unit = {},
    onBackToHome: () -> Unit = {}
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
            if (state.winners.isNotEmpty()) WinnerBanner(state.winners)
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
            }
            // « Rejouer » est livré par la Story 5.2 : visible mais inactif d'ici là.
            Button(
                onClick = {},
                enabled = false,
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

/** Vainqueur(s) : Score Cumulé le plus bas, ex æquo compris (FR-13). */
@Composable
private fun WinnerBanner(winners: List<StandingRow>) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.primary)
            .padding(16.dp)
            .semantics(mergeDescendants = true) {}
            .testTag("results_winner")
    ) {
        Text(
            text = pluralStringResource(R.plurals.results_winner_label, winners.size),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onPrimary
        )
        Text(
            text = winners.joinToString(", ") { it.name },
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onPrimary
        )
        Text(
            text = pluralStringResource(R.plurals.results_winner_score, winners.first().cumulativeScore, winners.first().cumulativeScore),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onPrimary
        )
    }
}

@Composable
private fun CenteredMessage(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        content()
    }
}

package com.rvetiveau.rvscorecount.ui.screens.partydetail

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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.rvetiveau.rvscorecount.R
import com.rvetiveau.rvscorecount.ui.components.RoundHistoryRow
import com.rvetiveau.rvscorecount.ui.components.SevenChampionLine
import com.rvetiveau.rvscorecount.ui.components.StandingRowItem
import com.rvetiveau.rvscorecount.ui.components.WinnerBanner
import com.rvetiveau.rvscorecount.ui.components.gamePresentation
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * Stateless Détail de Partie (Story 6.2): date and Jeu, final Classement with the winner banner,
 * Manches in chronological order (read-only) and « Rejouer avec ces joueurs ».
 */
@Composable
fun PartyDetailScreen(
    state: PartyDetailUiState,
    modifier: Modifier = Modifier,
    onRetry: () -> Unit = {},
    onReplay: () -> Unit = {}
) {
    when (state) {
        is PartyDetailUiState.Loading -> CenteredContent(modifier) {
            CircularProgressIndicator(modifier = Modifier.testTag("party_detail_loading"))
        }

        is PartyDetailUiState.LoadError -> CenteredContent(modifier) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = stringResource(R.string.party_detail_load_error),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.testTag("party_detail_load_error")
                )
                Button(onClick = onRetry, modifier = Modifier.padding(top = 16.dp)) {
                    Text(text = stringResource(R.string.root_error_retry))
                }
            }
        }

        is PartyDetailUiState.Unavailable -> CenteredContent(modifier) {
            Text(
                text = stringResource(R.string.party_detail_unavailable),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.testTag("party_detail_unavailable")
            )
        }

        is PartyDetailUiState.Loaded -> LoadedContent(state, onReplay, modifier)
    }
}

@Composable
private fun LoadedContent(state: PartyDetailUiState.Loaded, onReplay: () -> Unit, modifier: Modifier) {
    val dateFormatter = remember { DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM) }
    val date = Instant.ofEpochMilli(state.createdAt).atZone(ZoneId.systemDefault()).format(dateFormatter)
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = stringResource(R.string.party_detail_title),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier
                .padding(top = 16.dp)
                .testTag("party_detail_title")
        )
        Text(
            text = stringResource(
                R.string.party_detail_subtitle,
                date,
                stringResource(gamePresentation(state.gameId).nameRes)
            ),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier
                .padding(top = 4.dp, bottom = 16.dp)
                .testTag("party_detail_subtitle")
        )
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .testTag("party_detail_content")
        ) {
            if (state.winners.isNotEmpty()) {
                item(key = "winner") {
                    WinnerBanner(state.winners, modifier = Modifier.testTag("party_detail_winner"))
                }
            }
            state.sevenChampion?.let { champion ->
                item(key = "seven_champion") {
                    SevenChampionLine(champion = champion, modifier = Modifier.testTag("party_detail_seven_champion"))
                }
            }
            items(state.standings, key = { it.participantId }) { row ->
                StandingRowItem(
                    row = row,
                    highlightRank = state.rounds.isNotEmpty() && row.rank <= 3,
                    modifier = Modifier.testTag("party_detail_standing_row_${row.participantId}")
                )
            }
            item(key = "rounds_title") {
                Text(
                    text = stringResource(R.string.party_rounds_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }
            if (state.rounds.isEmpty()) {
                item(key = "no_round") {
                    Text(
                        text = stringResource(R.string.history_no_round),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.testTag("party_detail_no_round")
                    )
                }
            }
            items(state.rounds, key = { "round_${it.roundNumber}" }) { round ->
                RoundHistoryRow(item = round, modifier = Modifier.testTag("party_detail_round_${round.roundNumber}"))
            }
        }
        state.replayErrorRes?.let { errorRes ->
            Text(
                text = stringResource(errorRes),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier
                    .padding(top = 8.dp)
                    .testTag("party_detail_replay_error")
            )
        }
        Button(
            onClick = onReplay,
            enabled = !state.isReplaying,
            modifier = Modifier
                .padding(top = 16.dp)
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .testTag("party_detail_replay_button")
        ) {
            Text(text = stringResource(R.string.party_detail_replay))
        }
    }
}

@Composable
private fun CenteredContent(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        content()
    }
}

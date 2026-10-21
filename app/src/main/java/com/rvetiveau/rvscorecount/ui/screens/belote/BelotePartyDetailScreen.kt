package com.rvetiveau.rvscorecount.ui.screens.belote

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.rvetiveau.rvscorecount.R
import com.rvetiveau.rvscorecount.domain.engine.GameIds
import com.rvetiveau.rvscorecount.ui.components.ScreenHeader
import com.rvetiveau.rvscorecount.ui.components.gamePresentation
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * Stateless Détail of a finished Partie of Belote (Story 7.4, AC 2): the two Équipes, the winner
 * first in emphasis, then the Manches as during the Partie, read-only, and « Rejouer ».
 */
@Composable
fun BelotePartyDetailScreen(
    state: BelotePartyDetailUiState,
    modifier: Modifier = Modifier,
    onReplay: () -> Unit = {},
    onShowStats: () -> Unit = {}
) {
    when (state) {
        BelotePartyDetailUiState.Loading -> Centered(modifier) {
            CircularProgressIndicator(modifier = Modifier.testTag("belote_detail_loading"))
        }
        BelotePartyDetailUiState.Unavailable -> Centered(modifier) {
            Text(text = stringResource(R.string.party_detail_unavailable), modifier = Modifier.testTag("belote_detail_unavailable"))
        }
        is BelotePartyDetailUiState.Loaded -> {
            val dateFormatter = remember { DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM) }
            val date = Instant.ofEpochMilli(state.createdAt).atZone(ZoneId.systemDefault()).format(dateFormatter)
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                ScreenHeader(
                    title = stringResource(R.string.party_detail_title),
                    titleTag = "belote_detail_title",
                    modifier = Modifier.padding(top = 8.dp)
                )
                Text(
                    text = stringResource(
                        R.string.party_detail_subtitle,
                        date,
                        stringResource(gamePresentation(GameIds.BELOTE).nameRes)
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier
                        .padding(bottom = 8.dp)
                        .testTag("belote_detail_subtitle")
                )
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .testTag("belote_detail_content")
                ) {
                    items(state.teams.sortedByDescending { it.isLeader }, key = { it.side }) { team -> ResultCard(team) }
                    beloteRoundsSection(state.teams, state.rounds, onShowStats, onRoundSelected = null)
                }
                state.replayErrorRes?.let { errorRes ->
                    Text(
                        text = stringResource(errorRes),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.testTag("belote_detail_replay_error")
                    )
                }
                Button(
                    onClick = onReplay,
                    enabled = !state.isReplaying,
                    modifier = Modifier
                        .padding(top = 16.dp)
                        .fillMaxWidth()
                        .heightIn(min = 56.dp)
                        .testTag("belote_detail_replay")
                ) {
                    Text(text = stringResource(R.string.party_detail_replay))
                }
            }
        }
    }
}

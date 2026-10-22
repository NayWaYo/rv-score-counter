package com.rvetiveau.rvscorecount.ui.screens.free

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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.rvetiveau.rvscorecount.R
import com.rvetiveau.rvscorecount.domain.model.EndRule
import com.rvetiveau.rvscorecount.domain.model.WinDirection
import com.rvetiveau.rvscorecount.ui.components.RoundTable
import com.rvetiveau.rvscorecount.ui.components.ScreenHeader
import com.rvetiveau.rvscorecount.ui.components.StandingRowItem
import com.rvetiveau.rvscorecount.ui.screens.belote.Centered
import com.rvetiveau.rvscorecount.ui.screens.party.EndPartyDialog

/** The events of [FreePartyScreen]. */
data class FreePartyActions(
    val onRetry: () -> Unit = {},
    val onPlayRound: () -> Unit = {},
    val onRoundSelected: (Int) -> Unit = {},
    val onEndPartyClick: () -> Unit = {},
    val onEndPartyDismissed: () -> Unit = {},
    val onEndPartyConfirmed: () -> Unit = {},
    val onBackHome: () -> Unit = {}
)

/**
 * Stateless Partie of Jeu libre (Story 11.3): its game as title and its rules, then, in progress,
 * how far the end is, the Classement with the Donneur, the correctable Manches, « Manche N » and
 * « Fin de partie »; once finished, the winners, the final Classement and the Manches, read only.
 */
@Composable
fun FreePartyScreen(state: FreePartyUiState, modifier: Modifier = Modifier, actions: FreePartyActions = FreePartyActions()) {
    when (state) {
        FreePartyUiState.Loading -> Centered(modifier) {
            CircularProgressIndicator(modifier = Modifier.testTag("free_party_loading"))
        }
        FreePartyUiState.LoadError -> Centered(modifier) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = stringResource(R.string.party_load_error), modifier = Modifier.testTag("free_party_load_error"))
                Button(onClick = actions.onRetry, modifier = Modifier.padding(top = 16.dp)) {
                    Text(text = stringResource(R.string.root_error_retry))
                }
            }
        }
        FreePartyUiState.NotFound -> Centered(modifier) {
            Text(text = stringResource(R.string.party_not_found), modifier = Modifier.testTag("free_party_not_found"))
        }
        is FreePartyUiState.Loaded -> LoadedParty(state, modifier, actions)
    }
}

@Composable
private fun LoadedParty(state: FreePartyUiState.Loaded, modifier: Modifier, actions: FreePartyActions) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        ScreenHeader(
            title = state.gameName,
            titleTag = "free_party_title",
            modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
        )
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .testTag("free_party_content")
        ) {
            item {
                Text(
                    text = rulesText(state.winDirection, state.endRule),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.testTag("free_party_rules")
                )
            }
            if (state.isFinished) {
                item { FinishedBanner(state.winnerNames) }
            } else {
                state.progress?.let { progress ->
                    item {
                        Text(
                            text = progressText(progress),
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.testTag("free_party_progress")
                        )
                    }
                }
            }
            item {
                Text(
                    text = stringResource(R.string.free_party_standings_title),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            items(state.standings, key = { it.participantId }) { row ->
                StandingRowItem(
                    row = row,
                    isWinner = state.winnerNames.isNotEmpty() && row.rank == 1,
                    compact = true,
                    modifier = Modifier.testTag("free_party_standing_${row.participantId}")
                )
            }
            item {
                Text(
                    text = stringResource(R.string.party_rounds_title),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            item {
                if (state.roundTable.rows.isEmpty()) {
                    Text(
                        text = stringResource(R.string.belote_party_no_round),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.testTag("free_party_no_round")
                    )
                } else {
                    Column {
                        RoundTable(
                            table = state.roundTable,
                            onRoundClick = if (state.isFinished) null else actions.onRoundSelected,
                            legend = stringResource(R.string.round_table_free_legend)
                        )
                        if (!state.isFinished) {
                            Text(
                                text = stringResource(R.string.stats_correction_hint),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
        if (state.isFinished) {
            Button(
                onClick = actions.onBackHome,
                modifier = Modifier
                    .padding(top = 16.dp)
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .testTag("free_party_back_home")
            ) {
                Text(text = stringResource(R.string.results_back_home))
            }
        } else {
            InProgressActions(state, actions)
        }
    }
}

@Composable
private fun FinishedBanner(winnerNames: List<String>) {
    Column(modifier = Modifier.testTag("free_party_finished")) {
        Text(text = stringResource(R.string.free_party_finished), style = MaterialTheme.typography.titleMedium)
        if (winnerNames.isNotEmpty()) {
            Text(
                text = stringResource(
                    R.string.free_party_winners,
                    pluralStringResource(R.plurals.results_winner_label, winnerNames.size),
                    winnerNames.joinToString(", ")
                ),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.testTag("free_party_winners")
            )
        }
    }
}

@Composable
private fun InProgressActions(state: FreePartyUiState.Loaded, actions: FreePartyActions) {
    state.closure.errorRes?.let { errorRes ->
        Text(
            text = stringResource(errorRes),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier
                .padding(top = 8.dp)
                .testTag("free_party_end_error")
        )
    }
    Button(
        onClick = actions.onPlayRound,
        enabled = !state.closure.isClosing,
        modifier = Modifier
            .padding(top = 16.dp)
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .testTag("free_party_play_round")
    ) {
        Text(text = stringResource(R.string.free_party_play_round, state.nextRoundNumber))
    }
    OutlinedButton(
        onClick = actions.onEndPartyClick,
        enabled = !state.closure.isClosing,
        modifier = Modifier
            .padding(top = 8.dp)
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .testTag("free_party_end")
    ) {
        Text(text = stringResource(R.string.party_end_button))
    }
    if (state.closure.isConfirmationOpen) {
        EndPartyDialog(
            isClosing = state.closure.isClosing,
            onConfirm = actions.onEndPartyConfirmed,
            onDismiss = actions.onEndPartyDismissed
        )
    }
}

@Composable
private fun progressText(progress: FreeProgress): String = when (progress) {
    is FreeProgress.Rounds -> stringResource(R.string.free_party_progress_rounds, progress.current, progress.total)
    is FreeProgress.Target ->
        stringResource(R.string.free_party_progress_target, progress.closestNames.joinToString(", "), progress.remaining, progress.target)
}

/** « Le plus petit total gagne · Fin à 100 points ». */
@Composable
private fun rulesText(direction: WinDirection, endRule: EndRule?): String {
    val winner = stringResource(if (direction == WinDirection.LOWEST_WINS) R.string.free_rules_lowest else R.string.free_rules_highest)
    val end = when (endRule) {
        null -> stringResource(R.string.free_rules_manual)
        is EndRule.TargetScore -> stringResource(R.string.free_rules_target, endRule.points)
        is EndRule.RoundCount -> stringResource(R.string.free_rules_rounds, endRule.rounds)
    }
    return "$winner · $end"
}

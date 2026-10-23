package com.rvetiveau.rvscorecount.ui.screens.tarot

import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.keepScreenOn
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rvetiveau.rvscorecount.R
import com.rvetiveau.rvscorecount.domain.model.TarotContract
import com.rvetiveau.rvscorecount.ui.components.RoundTable
import com.rvetiveau.rvscorecount.ui.components.ScreenHeader
import com.rvetiveau.rvscorecount.ui.components.StandingRowItem
import com.rvetiveau.rvscorecount.ui.navigation.NavigationEvent
import com.rvetiveau.rvscorecount.ui.screens.belote.Centered
import com.rvetiveau.rvscorecount.ui.screens.party.EndPartyDialog
import java.util.UUID

/** The events of [TarotPartyScreen]. */
data class TarotPartyActions(
    val onRetry: () -> Unit = {},
    val onPlayRound: () -> Unit = {},
    val onRoundSelected: (Int) -> Unit = {},
    val onEndPartyClick: () -> Unit = {},
    val onEndPartyDismissed: () -> Unit = {},
    val onEndPartyConfirmed: () -> Unit = {},
    val onBackHome: () -> Unit = {},
    val onReplay: () -> Unit = {}
)

/**
 * Compose/Hilt adapter for a Partie of Tarot (AD-4), or for its Résultats when [isResults]; the
 * screen stays on while it is in progress.
 */
@Composable
fun TarotPartyRoute(
    partyId: UUID,
    onNavigate: (NavigationEvent) -> Unit,
    modifier: Modifier = Modifier,
    isResults: Boolean = false,
    viewModel: TarotPartyViewModel = hiltViewModel()
) {
    LaunchedEffect(partyId) { viewModel.initialize(partyId, isResults) }

    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val currentOnNavigate by rememberUpdatedState(onNavigate)

    LaunchedEffect(viewModel) {
        viewModel.navigationEvents.collect { event -> currentOnNavigate(event) }
    }

    val isFinished = (state as? TarotPartyUiState.Loaded)?.isFinished == true
    TarotPartyScreen(
        state = state,
        actions = TarotPartyActions(
            onRetry = viewModel::retry,
            onPlayRound = viewModel::onPlayRound,
            onRoundSelected = viewModel::onRoundSelected,
            onEndPartyClick = viewModel::onEndPartyClick,
            onEndPartyDismissed = viewModel::onEndPartyDismissed,
            onEndPartyConfirmed = viewModel::onEndPartyConfirmed,
            onBackHome = viewModel::onBackHome,
            onReplay = viewModel::onReplay
        ),
        modifier = if (isFinished) modifier else modifier.keepScreenOn()
    )
}

/**
 * Stateless Partie of Tarot (Story 13.3): its end, then, in progress, the Manches left, the
 * Classement with the Donneur, the dealing reminder, the contracts and points of the correctable
 * Manches, « Manche N » and « Fin de partie »; once finished, the winners, the final Classement and
 * the Manches, read only, with « Rejouer » and « Retour à l'accueil » in its Résultats.
 */
@Composable
fun TarotPartyScreen(state: TarotPartyUiState, modifier: Modifier = Modifier, actions: TarotPartyActions = TarotPartyActions()) {
    when (state) {
        TarotPartyUiState.Loading -> Centered(modifier) {
            CircularProgressIndicator(modifier = Modifier.testTag("tarot_party_loading"))
        }
        TarotPartyUiState.LoadError -> Centered(modifier) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = stringResource(R.string.party_load_error), modifier = Modifier.testTag("tarot_party_load_error"))
                Button(onClick = actions.onRetry, modifier = Modifier.padding(top = 16.dp)) {
                    Text(text = stringResource(R.string.root_error_retry))
                }
            }
        }
        TarotPartyUiState.NotFound -> Centered(modifier) {
            Text(text = stringResource(R.string.party_not_found), modifier = Modifier.testTag("tarot_party_not_found"))
        }
        is TarotPartyUiState.Loaded -> LoadedParty(state, modifier, actions)
    }
}

@Composable
private fun LoadedParty(state: TarotPartyUiState.Loaded, modifier: Modifier, actions: TarotPartyActions) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        ScreenHeader(
            title = stringResource(R.string.game_name_tarot),
            titleTag = "tarot_party_title",
            modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
        )
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .testTag("tarot_party_content")
        ) {
            item {
                Text(
                    text = state.endRule?.let { stringResource(R.string.tarot_rules_rounds, it.rounds) }
                        ?: stringResource(R.string.tarot_rules_manual),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.testTag("tarot_party_rules")
                )
            }
            if (state.isFinished) {
                item { FinishedBanner(state.winnerNames) }
            } else {
                state.progress?.let { progress ->
                    item {
                        Text(
                            text = stringResource(R.string.tarot_party_progress_rounds, progress.current, progress.total),
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.testTag("tarot_party_progress")
                        )
                    }
                }
                item {
                    Text(
                        text = stringResource(
                            R.string.tarot_party_dealing,
                            state.playerCount,
                            state.dealing.cardsPerPlayer,
                            state.dealing.dogSize
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.testTag("tarot_party_dealing")
                    )
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
                    modifier = Modifier.testTag("tarot_party_standing_${row.participantId}")
                )
            }
            item {
                Text(
                    text = stringResource(R.string.party_rounds_title),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            if (state.contracts.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.belote_party_no_round),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.testTag("tarot_party_no_round")
                    )
                }
            } else {
                items(state.contracts, key = { "contract_${it.roundNumber}" }) { line ->
                    ContractLine(line, onClick = if (state.isFinished) null else actions.onRoundSelected)
                }
                item {
                    Column {
                        RoundTable(
                            table = state.roundTable,
                            onRoundClick = if (state.isFinished) null else actions.onRoundSelected,
                            legend = stringResource(R.string.round_table_tarot_legend)
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
        when {
            !state.isFinished -> InProgressActions(state, actions)
            state.isResults -> ResultsActions(state, actions)
        }
    }
}

/** « 3. Alice avec Chloé · Garde · faite de 9 », a tap opening its correction while in progress. */
@Composable
private fun ContractLine(line: TarotContractLine, onClick: ((Int) -> Unit)?) {
    val taker = when {
        line.partnerName != null -> stringResource(R.string.tarot_round_line_partner, line.takerName, line.partnerName)
        line.isAlone -> stringResource(R.string.tarot_round_line_alone, line.takerName)
        else -> line.takerName
    }
    val outcome = stringResource(if (line.made) R.string.tarot_round_made else R.string.tarot_round_down, line.margin)
    Text(
        text = stringResource(R.string.tarot_round_line, line.roundNumber, taker, stringResource(line.contract.labelRes), outcome),
        style = MaterialTheme.typography.bodyMedium,
        color = if (line.made) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error,
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick(line.roundNumber) } else Modifier)
            .heightIn(min = if (onClick != null) 48.dp else 0.dp)
            .padding(vertical = 4.dp)
            .testTag("tarot_party_contract_${line.roundNumber}")
    )
}

@Composable
private fun FinishedBanner(winnerNames: List<String>) {
    Column(modifier = Modifier.testTag("tarot_party_finished")) {
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
                modifier = Modifier.testTag("tarot_party_winners")
            )
        }
    }
}

/** « Rejouer » then « Retour à l'accueil »: the Résultats only, never the detail from the Historique. */
@Composable
private fun ResultsActions(state: TarotPartyUiState.Loaded, actions: TarotPartyActions) {
    state.replayErrorRes?.let { errorRes ->
        Text(
            text = stringResource(errorRes),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier
                .padding(top = 8.dp)
                .testTag("tarot_results_replay_error")
        )
    }
    Button(
        onClick = actions.onReplay,
        enabled = !state.isReplaying,
        modifier = Modifier
            .padding(top = 16.dp)
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .testTag("tarot_results_replay")
    ) {
        Text(text = stringResource(R.string.results_replay))
    }
    OutlinedButton(
        onClick = actions.onBackHome,
        enabled = !state.isReplaying,
        modifier = Modifier
            .padding(top = 8.dp)
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .testTag("tarot_party_back_home")
    ) {
        Text(text = stringResource(R.string.results_back_home))
    }
}

@Composable
private fun InProgressActions(state: TarotPartyUiState.Loaded, actions: TarotPartyActions) {
    state.closure.errorRes?.let { errorRes ->
        Text(
            text = stringResource(errorRes),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier
                .padding(top = 8.dp)
                .testTag("tarot_party_end_error")
        )
    }
    Button(
        onClick = actions.onPlayRound,
        enabled = !state.closure.isClosing,
        modifier = Modifier
            .padding(top = 16.dp)
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .testTag("tarot_party_play_round")
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
            .testTag("tarot_party_end")
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

internal val TarotContract.labelRes: Int
    get() = when (this) {
        TarotContract.PRISE -> R.string.tarot_contract_prise
        TarotContract.GARDE -> R.string.tarot_contract_garde
        TarotContract.GARDE_SANS -> R.string.tarot_contract_garde_sans
        TarotContract.GARDE_CONTRE -> R.string.tarot_contract_garde_contre
    }

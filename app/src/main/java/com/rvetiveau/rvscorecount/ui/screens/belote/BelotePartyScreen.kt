package com.rvetiveau.rvscorecount.ui.screens.belote

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rvetiveau.rvscorecount.R
import com.rvetiveau.rvscorecount.domain.model.BeloteOutcome
import com.rvetiveau.rvscorecount.ui.components.ScreenHeader

/**
 * Stateless Partie of Belote in progress (Story 7.3, AC 3, 5): the two Équipes and their scores,
 * how far the end is, the Donneur, the litige reserve and the Manches, each one correctable.
 */
@Composable
fun BelotePartyScreen(
    state: BelotePartyUiState,
    modifier: Modifier = Modifier,
    onRetry: () -> Unit = {},
    onPlayRound: () -> Unit = {},
    onRoundSelected: (Int) -> Unit = {},
    onShowStats: () -> Unit = {}
) {
    when (state) {
        BelotePartyUiState.Loading -> Centered(modifier) {
            CircularProgressIndicator(modifier = Modifier.testTag("belote_party_loading"))
        }
        BelotePartyUiState.LoadError -> Centered(modifier) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = stringResource(R.string.party_load_error), modifier = Modifier.testTag("belote_party_load_error"))
                Button(onClick = onRetry, modifier = Modifier.padding(top = 16.dp)) {
                    Text(text = stringResource(R.string.root_error_retry))
                }
            }
        }
        BelotePartyUiState.NotFound -> Centered(modifier) {
            Text(text = stringResource(R.string.party_not_found), modifier = Modifier.testTag("belote_party_not_found"))
        }
        is BelotePartyUiState.Loaded -> LoadedParty(state, modifier, onPlayRound, onRoundSelected, onShowStats)
    }
}

@Composable
private fun LoadedParty(
    state: BelotePartyUiState.Loaded,
    modifier: Modifier,
    onPlayRound: () -> Unit,
    onRoundSelected: (Int) -> Unit,
    onShowStats: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        ScreenHeader(
            title = stringResource(R.string.belote_party_title),
            titleTag = "belote_party_title",
            modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
        )
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .testTag("belote_party_content")
        ) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.teams.forEach { team ->
                        TeamScoreCard(team, Modifier.weight(1f))
                    }
                }
            }
            item {
                Text(
                    text = progressText(state.progress),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.testTag("belote_party_progress")
                )
                Text(
                    text = stringResource(R.string.belote_party_dealer, state.dealerName),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.testTag("belote_party_dealer")
                )
            }
            if (state.pendingReserve > 0) {
                item {
                    Text(
                        text = stringResource(R.string.belote_party_reserve, state.pendingReserve),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.testTag("belote_party_reserve")
                    )
                }
            }
            item {
                Text(
                    text = stringResource(R.string.belote_party_rules),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
            beloteRoundsSection(state.teams, state.rounds, onShowStats, onRoundSelected)
        }
        Button(
            onClick = onPlayRound,
            modifier = Modifier
                .padding(top = 16.dp)
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .testTag("belote_party_play_round")
        ) {
            Text(text = stringResource(R.string.belote_party_play_round, state.nextRoundNumber))
        }
    }
}

@Composable
internal fun TeamScoreCard(team: BeloteTeamUiModel, modifier: Modifier = Modifier) {
    val description = if (team.isLeader) {
        stringResource(R.string.belote_party_leader_content_description, team.name)
    } else {
        team.name
    }
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = if (team.isLeader) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
        modifier = modifier.testTag("belote_team_${team.side.name}")
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Text(
                text = team.name,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.semantics { contentDescription = description }
            )
            Text(
                text = team.playerNames.joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
                textAlign = TextAlign.Center
            )
            Text(
                text = team.total.toString(),
                style = MaterialTheme.typography.displaySmall,
                color = if (team.isLeader) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .padding(top = 4.dp)
                    .testTag("belote_team_total_${team.side.name}")
            )
        }
    }
}

/**
 * « Manches jouées » with the link to the Statistiques once a Manche is played, then the table of
 * the Manches, each one correctable when [onRoundSelected] is given (read-only otherwise).
 */
internal fun LazyListScope.beloteRoundsSection(
    teams: List<BeloteTeamUiModel>,
    rounds: List<BeloteRoundRowUiModel>,
    onShowStats: () -> Unit,
    onRoundSelected: ((Int) -> Unit)?
) {
    item {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
            Text(
                text = stringResource(R.string.party_rounds_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )
            if (rounds.isNotEmpty()) {
                TextButton(onClick = onShowStats, modifier = Modifier.testTag("belote_stats_link")) {
                    Text(text = stringResource(R.string.stats_short_link, rounds.size))
                }
            }
        }
    }
    if (rounds.isEmpty()) {
        item {
            Text(
                text = stringResource(R.string.belote_party_no_round),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.testTag("belote_party_no_round")
            )
        }
    } else {
        item { RoundsHeader(teams[0].name, teams[1].name) }
        items(rounds, key = { it.roundNumber }) { row ->
            RoundRow(
                row = row,
                firstTeamName = teams[0].name,
                secondTeamName = teams[1].name,
                onClick = onRoundSelected?.let { select -> { select(row.roundNumber) } }
            )
            HorizontalDivider()
        }
    }
}

/** Names the points columns of [RoundRow]; decorative for TalkBack, each row describing itself. */
@Composable
private fun RoundsHeader(firstTeamName: String, secondTeamName: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clearAndSetSemantics {}
    ) {
        Spacer(modifier = Modifier.weight(1f))
        listOf(firstTeamName, secondTeamName).forEach { name ->
            Text(
                text = name,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.outline,
                textAlign = TextAlign.End,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.width(RoundPointsWidth)
            )
        }
    }
}

private val RoundPointsWidth = 64.dp

@Composable
private fun RoundRow(
    row: BeloteRoundRowUiModel,
    firstTeamName: String,
    secondTeamName: String,
    onClick: (() -> Unit)?
) {
    val outcome = stringResource(row.outcome.labelRes)
    val description = stringResource(
        R.string.belote_party_round_content_description,
        row.roundNumber, row.takerName, outcome, firstTeamName, row.firstPoints, secondTeamName, row.secondPoints
    )
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .testTag("belote_round_row_${row.roundNumber}")
            .clearAndSetSemantics {
                contentDescription = description
                if (onClick != null) role = Role.Button
            }
            .padding(vertical = 8.dp)
    ) {
        Text(
            text = row.roundNumber.toString(),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.width(32.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(text = stringResource(R.string.belote_party_round_taker, row.takerName), style = MaterialTheme.typography.bodyMedium)
            Text(
                text = outcome,
                style = MaterialTheme.typography.bodySmall,
                color = if (row.outcome == BeloteOutcome.MADE) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.primary
            )
        }
        Text(
            text = row.firstPoints.toString(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.End,
            modifier = Modifier.width(RoundPointsWidth)
        )
        Text(
            text = row.secondPoints.toString(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.End,
            modifier = Modifier.width(RoundPointsWidth)
        )
    }
}

@Composable
private fun progressText(progress: BeloteProgress): String = when (progress) {
    is BeloteProgress.Target -> if (progress.leaderNames.size == 1) {
        stringResource(R.string.belote_party_progress_target, progress.target, progress.remaining, progress.leaderNames.single())
    } else {
        stringResource(R.string.belote_party_progress_target_tied, progress.target, progress.remaining)
    }
    is BeloteProgress.Rounds -> stringResource(R.string.belote_party_progress_rounds, progress.current, progress.total)
    BeloteProgress.ExtraRound -> stringResource(R.string.belote_party_progress_extra)
}

internal val BeloteOutcome.labelRes: Int
    get() = when (this) {
        BeloteOutcome.MADE -> R.string.belote_outcome_made
        BeloteOutcome.DOWN -> R.string.belote_outcome_down
        BeloteOutcome.DISPUTED -> R.string.belote_outcome_disputed
        BeloteOutcome.CAPOT -> R.string.belote_outcome_capot
    }

@Composable
internal fun Centered(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        content()
    }
}

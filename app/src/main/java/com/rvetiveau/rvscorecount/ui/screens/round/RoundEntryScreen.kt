package com.rvetiveau.rvscorecount.ui.screens.round

import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.rvetiveau.rvscorecount.R
import com.rvetiveau.rvscorecount.domain.model.CardSuit
import com.rvetiveau.rvscorecount.ui.components.DieRoller
import com.rvetiveau.rvscorecount.ui.components.RoundHistoryRow
import com.rvetiveau.rvscorecount.ui.components.RoundRecordLine
import com.rvetiveau.rvscorecount.ui.components.SevenChampionLine
import com.rvetiveau.rvscorecount.ui.components.StandingRowItem
import com.rvetiveau.rvscorecount.ui.components.ScoreField
import com.rvetiveau.rvscorecount.ui.components.SumIndicator
import com.rvetiveau.rvscorecount.ui.theme.nameRes
import java.util.UUID

/**
 * Stateless Saisie de Manche screen (AC 1-12): a large die in the middle, then « Classement » and
 * « Saisir les scores », each showing its panel (Classement, statistics and Manches, or free-order
 * score entry and running sum)
 * once the Partie/Participants aggregate is loaded. Loading/error states never render partial
 * fields and gate the die's actions until the content is ready (AC 10).
 */
@Composable
fun RoundEntryScreen(
    state: RoundEntryUiState,
    onRoll: () -> Unit,
    onScoreChanged: (UUID, String) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    onValidate: () -> Unit = {},
    onSevenHolderSelected: (UUID) -> Unit = {},
    onPanelSelected: (RoundEntryPanel) -> Unit = {}
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        Text(
            text = stringResource(R.string.round_entry_title),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier
                .padding(vertical = 16.dp)
                .testTag("round_entry_title")
        )

        val content = state.content
        when {
            state.isLoading -> CircularProgressIndicator(
                modifier = Modifier.testTag("round_entry_loading")
            )

            state.loadError -> Column {
                Text(
                    text = stringResource(R.string.round_entry_load_error),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.testTag("round_entry_load_error")
                )
                Button(
                    onClick = onRetry,
                    modifier = Modifier
                        .padding(top = 16.dp)
                        .testTag("round_entry_retry_button")
                ) {
                    Text(text = stringResource(R.string.root_error_retry))
                }
            }

            content != null -> {
                Text(
                    text = stringResource(R.string.round_entry_suit_label),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp, bottom = 16.dp)
                        .testTag("round_entry_suit_label")
                )
                DieRoller(
                    suit = state.suit,
                    onRoll = onRoll,
                    rerollEnabled = state.rerollEnabled,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 32.dp)
                        .testTag("round_entry_die_roller")
                )
                PanelButtons(panel = state.panel, onPanelSelected = onPanelSelected)
                when (state.panel) {
                    RoundEntryPanel.NONE -> Unit
                    RoundEntryPanel.STANDINGS -> StandingsPanel(summary = content.summary)
                    RoundEntryPanel.SCORES -> ScoreForm(
                        suit = state.suit,
                        content = content,
                        canValidate = state.canValidate,
                        saveError = state.saveError,
                        onScoreChanged = onScoreChanged,
                        onValidate = onValidate,
                        onSevenHolderSelected = onSevenHolderSelected
                    )
                }
            }
        }
    }
}

/** « Classement » and « Saisir les scores »: each shows its panel under the die, or hides it. */
@Composable
private fun PanelButtons(panel: RoundEntryPanel, onPanelSelected: (RoundEntryPanel) -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
    ) {
        PanelButton(
            selected = panel == RoundEntryPanel.STANDINGS,
            label = stringResource(R.string.round_entry_standings_button),
            onClick = { onPanelSelected(RoundEntryPanel.STANDINGS) },
            modifier = Modifier
                .weight(1f)
                .testTag("round_entry_standings_button")
        )
        PanelButton(
            selected = panel == RoundEntryPanel.SCORES,
            label = stringResource(R.string.round_entry_scores_button),
            onClick = { onPanelSelected(RoundEntryPanel.SCORES) },
            modifier = Modifier
                .weight(1f)
                .testTag("round_entry_scores_button")
        )
    }
}

@Composable
private fun PanelButton(selected: Boolean, label: String, onClick: () -> Unit, modifier: Modifier) {
    val selectable = modifier
        .heightIn(min = 56.dp)
        .semantics { this.selected = selected }
    if (selected) {
        Button(onClick = onClick, modifier = selectable) {
            Text(text = label, textAlign = TextAlign.Center)
        }
    } else {
        OutlinedButton(onClick = onClick, modifier = selectable) {
            Text(text = label, textAlign = TextAlign.Center)
        }
    }
}

/** The Partie so far: Classement, statistics, then the validated Manches, most recent first. */
@Composable
private fun StandingsPanel(summary: RoundEntrySummary) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
            .testTag("round_entry_standings_panel")
    ) {
        summary.standings.forEach { row ->
            StandingRowItem(row, modifier = Modifier.testTag("round_entry_standing_row_${row.participantId}"))
        }
        if (summary.sevenChampion != null || summary.roundRecord != null) {
            PanelHeading(text = stringResource(R.string.round_entry_stats_title))
            summary.sevenChampion?.let { champion ->
                SevenChampionLine(champion = champion, modifier = Modifier.testTag("round_entry_seven_champion"))
            }
            summary.roundRecord?.let { record ->
                RoundRecordLine(record = record, modifier = Modifier.testTag("round_entry_round_record"))
            }
        }
        PanelHeading(text = stringResource(R.string.party_rounds_title))
        if (summary.roundHistory.isEmpty()) {
            Text(
                text = stringResource(R.string.history_no_round),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.testTag("round_entry_no_round")
            )
        }
        summary.roundHistory.forEach { round ->
            RoundHistoryRow(item = round, modifier = Modifier.testTag("round_entry_round_history_${round.roundNumber}"))
        }
    }
}

@Composable
private fun PanelHeading(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.padding(top = 8.dp)
    )
}

/** Score entry, « Qui a eu le 40 ? », running sum and « Valider la Manche » (Stories 3.2-3.4). */
@Composable
private fun ScoreForm(
    suit: CardSuit?,
    content: RoundEntryContent,
    canValidate: Boolean,
    saveError: Boolean,
    onScoreChanged: (UUID, String) -> Unit,
    onValidate: () -> Unit,
    onSevenHolderSelected: (UUID) -> Unit
) {
    val focusManager = LocalFocusManager.current
    val participantIds = content.participants.map { it.id }
    val focusRequesters = remember(participantIds) {
        participantIds.associateWith { FocusRequester() }
    }

    // Runs each time the form is shown (AC 7): focus goes to the first empty field.
    LaunchedEffect(Unit) {
        val firstEmpty = content.participants.firstOrNull { content.scores[it.id].isNullOrEmpty() }
        firstEmpty?.let { focusRequesters.getValue(it.id).requestFocus() }
    }

    content.participants.forEachIndexed { index, participant ->
        val imeAction = if (index == content.participants.lastIndex) ImeAction.Done else ImeAction.Next
        ScoreField(
            participantId = participant.id,
            participantName = participant.name,
            value = content.scores[participant.id].orEmpty(),
            onValueChange = { onScoreChanged(participant.id, it) },
            imeAction = imeAction,
            onImeAction = { action ->
                when (action) {
                    ImeAction.Next -> content.participants.getOrNull(index + 1)
                        ?.let { next -> focusRequesters.getValue(next.id).requestFocus() }
                    ImeAction.Done -> focusManager.clearFocus()
                    else -> Unit
                }
            },
            focusRequester = focusRequesters[participant.id],
            isPrecomputed = participant.id == content.precomputedParticipantId,
            isError = content.sumIndicatorState == SumIndicatorState.ERROR,
            modifier = Modifier.padding(bottom = 8.dp)
        )
    }

    content.sevenPoints?.let { sevenPoints ->
        SevenHolderSelector(
            suit = suit,
            sevenPoints = sevenPoints,
            content = content,
            onSevenHolderSelected = onSevenHolderSelected
        )
    }

    SumIndicator(
        currentSum = content.currentSum,
        state = content.sumIndicatorState,
        totalPointsPerRound = content.totalPointsPerRound,
        modifier = Modifier.padding(top = 16.dp)
    )
    if (saveError) {
        Text(
            text = stringResource(R.string.round_entry_save_error),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier
                .padding(top = 16.dp)
                .testTag("round_entry_save_error")
        )
    }
    Button(
        onClick = onValidate,
        enabled = canValidate,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp)
            .testTag("round_entry_validate_button")
    ) {
        Text(text = stringResource(R.string.round_entry_validate_button))
    }
}

/** « Qui a eu le 40 ? » : one chip per Participant; the holder must have scored at least the 7's points. */
@Composable
private fun SevenHolderSelector(
    suit: CardSuit?,
    sevenPoints: Int,
    content: RoundEntryContent,
    onSevenHolderSelected: (UUID) -> Unit
) {
    Column(modifier = Modifier.padding(top = 16.dp)) {
        Text(
            text = if (suit != null) {
                stringResource(R.string.round_entry_seven_question_suit, stringResource(suit.nameRes), sevenPoints)
            } else {
                stringResource(R.string.round_entry_seven_question, sevenPoints)
            },
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.testTag("round_entry_seven_question")
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.testTag("round_entry_seven_holders")
        ) {
            content.participants.forEach { participant ->
                FilterChip(
                    selected = participant.id == content.sevenHolderId,
                    onClick = { onSevenHolderSelected(participant.id) },
                    label = { Text(text = participant.name) },
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("round_entry_seven_holder_${participant.id}")
                )
            }
        }
        if (content.isSevenHolderScoreTooLow) {
            val holderName = content.participants.first { it.id == content.sevenHolderId }.name
            Text(
                text = stringResource(R.string.round_entry_seven_score_too_low, holderName, sevenPoints),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.testTag("round_entry_seven_error")
            )
        }
    }
}

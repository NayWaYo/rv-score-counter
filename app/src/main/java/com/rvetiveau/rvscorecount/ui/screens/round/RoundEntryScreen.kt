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
import com.rvetiveau.rvscorecount.ui.components.ScreenHeader
import com.rvetiveau.rvscorecount.domain.model.CardSuit
import com.rvetiveau.rvscorecount.ui.components.DieRoller
import com.rvetiveau.rvscorecount.ui.components.StandingRowItem
import com.rvetiveau.rvscorecount.ui.components.ScoreField
import com.rvetiveau.rvscorecount.ui.components.SumIndicator
import com.rvetiveau.rvscorecount.ui.theme.color
import com.rvetiveau.rvscorecount.ui.theme.nameRes
import com.rvetiveau.rvscorecount.ui.theme.pictogramRes
import java.util.UUID

/**
 * Stateless Saisie de Manche screen (AC 1-12): a large die in the middle, then « Classement » and
 * « Saisir les scores », each showing its panel (Classement, or free-order
 * score entry); while the form is shown, the running sum and « Valider » stay pinned at the bottom
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
    onPanelSelected: (RoundEntryPanel) -> Unit = {},
    scanActions: TrumpScanActions = TrumpScanActions()
) {
    val content = state.content
    val scan = state.scan
    val trumps = content?.faceValueCards
    if (scan != null && trumps != null) {
        TrumpScanDialog(
            scan = scan,
            participantName = content.participants.firstOrNull { it.id == scan.participantId }?.name.orEmpty(),
            trumps = trumps,
            takenByOthers = content.trumpsTakenByOthers(scan.participantId),
            sevenPoints = content.sevenPoints,
            onTrumpToggled = scanActions.onTrumpToggled,
            onRetake = scanActions.onRetake,
            onDismiss = scanActions.onDismiss,
            onConfirm = scanActions.onConfirm
        )
    }
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        ScrollingContent(
            state = state,
            onRoll = onRoll,
            onScoreChanged = onScoreChanged,
            onRetry = onRetry,
            onSevenHolderSelected = onSevenHolderSelected,
            onScanRequested = scanActions.onScanRequested,
            onPanelSelected = onPanelSelected,
            modifier = Modifier.weight(1f)
        )
        if (content != null && (state.isCorrection || state.panel == RoundEntryPanel.SCORES)) {
            ValidationBar(
                content = content,
                canValidate = state.canValidate,
                saveError = state.saveError,
                validateLabel = stringResource(
                    if (state.isCorrection) R.string.round_correction_save_button else R.string.round_entry_validate_button
                ),
                onValidate = onValidate
            )
        }
    }
}

/** Everything above the validation bar; scrolls on its own so the bar stays above the keyboard. */
@Composable
private fun ScrollingContent(
    state: RoundEntryUiState,
    onRoll: () -> Unit,
    onScoreChanged: (UUID, String) -> Unit,
    onRetry: () -> Unit,
    onSevenHolderSelected: (UUID) -> Unit,
    onScanRequested: (UUID) -> Unit,
    onPanelSelected: (RoundEntryPanel) -> Unit,
    modifier: Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        ScreenHeader(
            title = state.correctedRoundNumber
                ?.let { stringResource(R.string.round_correction_title, it) }
                ?: stringResource(R.string.round_entry_title),
            titleTag = "round_entry_title",
            modifier = Modifier.padding(vertical = 8.dp)
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

            content != null && state.isCorrection -> {
                state.suit?.let { suit -> CorrectedSuit(suit) }
                ScoreForm(
                    suit = state.suit,
                    content = content,
                    onScoreChanged = onScoreChanged,
                    onSevenHolderSelected = onSevenHolderSelected,
                    onScanRequested = onScanRequested
                )
            }

            content != null -> {
                // A small die while entering scores leaves the fields room above the keyboard.
                val compactDie = state.panel == RoundEntryPanel.SCORES
                Text(
                    text = stringResource(R.string.round_entry_suit_label),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = if (compactDie) TextAlign.Start else TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = if (compactDie) 0.dp else 16.dp, bottom = if (compactDie) 8.dp else 16.dp)
                        .testTag("round_entry_suit_label")
                )
                DieRoller(
                    suit = state.suit,
                    onRoll = onRoll,
                    rerollEnabled = state.rerollEnabled,
                    compact = compactDie,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = if (compactDie) 16.dp else 32.dp)
                        .testTag("round_entry_die_roller")
                )
                PanelButtons(panel = state.panel, onPanelSelected = onPanelSelected)
                when (state.panel) {
                    RoundEntryPanel.NONE -> Unit
                    RoundEntryPanel.STANDINGS -> StandingsPanel(summary = content.summary)
                    RoundEntryPanel.SCORES -> ScoreForm(
                        suit = state.suit,
                        content = content,
                        onScoreChanged = onScoreChanged,
                        onSevenHolderSelected = onSevenHolderSelected,
                        onScanRequested = onScanRequested
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

/** The Partie so far: its Classement with the Donneur badge. */
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
    }
}

/**
 * « Qui a eu le 40 ? » first, since it sets a floor on its holder's score, then the score fields
 * (Stories 3.2-3.4). The running sum and « Valider » live in [ValidationBar].
 */
@Composable
private fun ScoreForm(
    suit: CardSuit?,
    content: RoundEntryContent,
    onScoreChanged: (UUID, String) -> Unit,
    onSevenHolderSelected: (UUID) -> Unit,
    onScanRequested: (UUID) -> Unit
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

    content.sevenPoints?.let { sevenPoints ->
        SevenHolderSelector(
            suit = suit,
            sevenPoints = sevenPoints,
            content = content,
            onSevenHolderSelected = onSevenHolderSelected
        )
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
            suggestion = content.suggestion?.takeIf { it.participantId == participant.id }?.score,
            onScan = content.faceValueCards?.let { { onScanRequested(participant.id) } },
            isError = content.sumIndicatorState == SumIndicatorState.ERROR,
            modifier = Modifier.padding(bottom = 8.dp)
        )
    }
}

/** Running sum, save error and « Valider », pinned under the form so they stay in reach with 6 to 8 players. */
@Composable
private fun ValidationBar(
    content: RoundEntryContent,
    canValidate: Boolean,
    saveError: Boolean,
    validateLabel: String,
    onValidate: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag("round_entry_validation_bar")
    ) {
        SumIndicator(
            currentSum = content.currentSum,
            state = content.sumIndicatorState,
            totalPointsPerRound = content.totalPointsPerRound
        )
        if (saveError) {
            Text(
                text = stringResource(R.string.round_entry_save_error),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier
                    .padding(top = 8.dp)
                    .testTag("round_entry_save_error")
            )
        }
        Button(
            onClick = onValidate,
            enabled = canValidate,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .padding(top = 8.dp)
                .testTag("round_entry_validate_button")
        ) {
            Text(text = validateLabel)
        }
    }
}

/** The corrected Manche keeps the suit drawn when it was played: shown, never rolled again. */
@Composable
private fun CorrectedSuit(suit: CardSuit) {
    Text(
        text = stringResource(
            R.string.round_correction_suit,
            stringResource(suit.pictogramRes),
            stringResource(suit.nameRes)
        ),
        style = MaterialTheme.typography.titleMedium,
        color = suit.color,
        modifier = Modifier
            .padding(bottom = 16.dp)
            .testTag("round_correction_suit")
    )
}

/** « Qui a eu le 40 ? » : one chip per Participant; the holder must have scored at least the 7's points. */
@Composable
private fun SevenHolderSelector(
    suit: CardSuit?,
    sevenPoints: Int,
    content: RoundEntryContent,
    onSevenHolderSelected: (UUID) -> Unit
) {
    Column(modifier = Modifier.padding(bottom = 16.dp)) {
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

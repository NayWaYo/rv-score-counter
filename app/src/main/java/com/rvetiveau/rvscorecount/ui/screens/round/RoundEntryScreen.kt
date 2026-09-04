package com.rvetiveau.rvscorecount.ui.screens.round

import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.rvetiveau.rvscorecount.R
import com.rvetiveau.rvscorecount.domain.model.CardSuit
import com.rvetiveau.rvscorecount.ui.components.DieRoller
import com.rvetiveau.rvscorecount.ui.components.ScoreField
import com.rvetiveau.rvscorecount.ui.components.SumIndicator
import java.util.UUID

/**
 * Stateless Saisie de Manche screen (AC 1-12): die roll, free-order score entry and running sum
 * once the Partie/Participants aggregate is loaded. Loading/error states never render partial
 * fields and gate the die's actions until the content is ready (AC 10).
 */
@Composable
fun RoundEntryScreen(
    state: RoundEntryUiState,
    onRoll: () -> Unit,
    onScoreChanged: (UUID, String) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
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

            content != null -> RoundEntryContentColumn(
                suit = state.suit,
                rerollEnabled = state.rerollEnabled,
                content = content,
                onRoll = onRoll,
                onScoreChanged = onScoreChanged
            )
        }
    }
}

@Composable
private fun RoundEntryContentColumn(
    suit: CardSuit?,
    rerollEnabled: Boolean,
    content: RoundEntryContent,
    onRoll: () -> Unit,
    onScoreChanged: (UUID, String) -> Unit
) {
    val focusManager = LocalFocusManager.current
    val participantIds = content.participants.map { it.id }
    val focusRequesters = remember(participantIds) {
        participantIds.associateWith { FocusRequester() }
    }

    // Runs once per entry into this composable (i.e. once per loading -> ready transition, AC 7).
    LaunchedEffect(Unit) {
        val firstEmpty = content.participants.firstOrNull { content.scores[it.id].isNullOrEmpty() }
        firstEmpty?.let { focusRequesters.getValue(it.id).requestFocus() }
    }

    Text(
        text = stringResource(R.string.round_entry_suit_label),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .padding(bottom = 8.dp)
            .testTag("round_entry_suit_label")
    )
    DieRoller(
        suit = suit,
        onRoll = onRoll,
        rerollEnabled = rerollEnabled,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 24.dp)
            .testTag("round_entry_die_roller")
    )

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

    SumIndicator(
        currentSum = content.currentSum,
        state = content.sumIndicatorState,
        totalPointsPerRound = content.totalPointsPerRound,
        modifier = Modifier.padding(top = 16.dp)
    )
    Button(
        onClick = {},
        enabled = content.canValidate,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp)
            .testTag("round_entry_validate_button")
    ) {
        Text(text = stringResource(R.string.round_entry_validate_button))
    }
}

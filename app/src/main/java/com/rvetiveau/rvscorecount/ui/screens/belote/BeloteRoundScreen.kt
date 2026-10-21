package com.rvetiveau.rvscorecount.ui.screens.belote

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.rvetiveau.rvscorecount.R
import com.rvetiveau.rvscorecount.domain.model.TeamSide
import com.rvetiveau.rvscorecount.ui.components.ScreenHeader
import java.util.UUID

/**
 * Stateless saisie of a Manche of Belote (Story 7.3, AC 4, 5): the player who took, the trick points of one
 * Équipe (the other's are deduced), the belote-rebelote and the capot, then the engine's preview.
 */
@Composable
fun BeloteRoundScreen(
    state: BeloteRoundUiState,
    modifier: Modifier = Modifier,
    onTakerPlayerSelected: (UUID) -> Unit = {},
    onPointsChanged: (TeamSide, String) -> Unit = { _, _ -> },
    onBeloteChanged: (TeamSide?) -> Unit = {},
    onCapotChanged: (TeamSide?) -> Unit = {},
    onValidate: () -> Unit = {}
) {
    when (state) {
        BeloteRoundUiState.Loading -> Centered(modifier) {
            CircularProgressIndicator(modifier = Modifier.testTag("belote_round_loading"))
        }
        BeloteRoundUiState.Unavailable -> Centered(modifier) {
            Text(text = stringResource(R.string.party_not_found), modifier = Modifier.testTag("belote_round_unavailable"))
        }
        is BeloteRoundUiState.Loaded -> Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = modifier
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            val title = if (state.isCorrection) R.string.belote_round_correction_title else R.string.belote_round_title
            ScreenHeader(
                title = stringResource(title, state.roundNumber),
                titleTag = "belote_round_title",
                modifier = Modifier.padding(top = 8.dp)
            )
            Section(R.string.belote_round_taker) {
                TakerChoice(state, onTakerPlayerSelected)
            }
            Section(R.string.belote_round_trick_points) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TeamSide.entries.forEach { side ->
                        OutlinedTextField(
                            value = state.pointsInputs[side.ordinal],
                            onValueChange = { onPointsChanged(side, it) },
                            label = { Text(state.teamNames[side.ordinal]) },
                            singleLine = true,
                            isError = state.pointsError,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("belote_round_points_${side.name}")
                        )
                    }
                }
                if (state.pointsError) {
                    Text(
                        text = stringResource(R.string.belote_round_points_error),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.testTag("belote_round_points_error")
                    )
                }
            }
            Section(R.string.belote_round_belote) {
                TeamChoice(state.teamNames, state.beloteTeam, allowNobody = true, tag = "belote_round_belote", onSelected = onBeloteChanged)
            }
            Section(R.string.belote_round_capot) {
                TeamChoice(state.teamNames, state.capotTeam, allowNobody = true, tag = "belote_round_capot", onSelected = onCapotChanged)
            }
            Preview(state)
            state.errorRes?.let { errorRes ->
                Text(
                    text = stringResource(errorRes),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.testTag("belote_round_error")
                )
            }
            Button(
                onClick = onValidate,
                enabled = state.canValidate,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .testTag("belote_round_validate")
            ) {
                val label = if (state.isCorrection) R.string.belote_round_save_correction else R.string.belote_round_validate
                Text(text = stringResource(label))
            }
        }
    }
}

@Composable
private fun Section(titleRes: Int, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = stringResource(titleRes), style = MaterialTheme.typography.titleMedium)
        content()
    }
}

/**
 * The 4 players, one row per Équipe under its name (Story 8.3). A correction of a Manche recorded
 * before knows only its Équipe: it asks which of its players took.
 */
@Composable
private fun TakerChoice(state: BeloteRoundUiState.Loaded, onTakerPlayerSelected: (UUID) -> Unit) {
    TeamSide.entries.forEach { side ->
        val seats = state.players.filter { it.side == side }
        Text(
            text = state.teamNames[side.ordinal],
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.outline
        )
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            seats.forEachIndexed { index, seat ->
                SegmentedButton(
                    selected = state.takerPlayerId == seat.id,
                    onClick = { onTakerPlayerSelected(seat.id) },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = seats.size),
                    colors = beloteSegmentedColors(),
                    modifier = Modifier.testTag("belote_round_taker_${seat.id}")
                ) {
                    Text(text = seat.name)
                }
            }
        }
    }
    val taker = state.taker
    if (taker != null && state.takerPlayerId == null) {
        Text(
            text = stringResource(R.string.belote_round_taker_player_hint, state.teamNames[taker.ordinal]),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.testTag("belote_round_taker_player_hint")
        )
    }
}

/** One segment per Équipe, after a « Personne » one when [allowNobody]. */
@Composable
private fun TeamChoice(
    teamNames: List<String>,
    selected: TeamSide?,
    allowNobody: Boolean,
    tag: String,
    onSelected: (TeamSide?) -> Unit
) {
    val options: List<TeamSide?> = (if (allowNobody) listOf(null) else emptyList<TeamSide?>()) + TeamSide.entries
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        options.forEachIndexed { index, side ->
            SegmentedButton(
                selected = selected == side,
                onClick = { onSelected(side) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                colors = beloteSegmentedColors(),
                modifier = Modifier.testTag("${tag}_${side?.name ?: "NONE"}")
            ) {
                Text(text = side?.let { teamNames[it.ordinal] } ?: stringResource(R.string.belote_round_nobody))
            }
        }
    }
}

@Composable
private fun Preview(state: BeloteRoundUiState.Loaded) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("belote_round_preview")
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(16.dp)) {
            val preview = state.preview
            if (preview == null) {
                Text(
                    text = stringResource(R.string.belote_round_preview_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline
                )
            } else {
                Text(
                    text = stringResource(preview.outcome.labelRes),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.testTag("belote_round_preview_outcome")
                )
                Text(
                    text = stringResource(
                        R.string.belote_round_preview_points,
                        state.teamNames[0], preview.points.first, state.teamNames[1], preview.points.second
                    ),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.testTag("belote_round_preview_points")
                )
                if (preview.reserveWon > 0) {
                    Text(
                        text = stringResource(R.string.belote_round_preview_reserve_won, preview.reserveWon),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                if (preview.reserveAfter > 0) {
                    Text(
                        text = stringResource(R.string.belote_round_preview_reserve_kept, preview.reserveAfter),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.testTag("belote_round_preview_reserve")
                    )
                }
            }
        }
    }
}

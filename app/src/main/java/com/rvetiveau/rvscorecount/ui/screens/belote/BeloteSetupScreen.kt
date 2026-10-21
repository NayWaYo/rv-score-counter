package com.rvetiveau.rvscorecount.ui.screens.belote

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonColors
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.rvetiveau.rvscorecount.R
import com.rvetiveau.rvscorecount.ui.components.ScreenHeader

/**
 * Stateless creation of a Partie of Belote (Story 7.3, AC 2): two Équipes of two players, the end
 * rule, then the table order and the first Donneur as they will be played.
 */
@Composable
fun BeloteSetupScreen(
    state: BeloteSetupUiState,
    modifier: Modifier = Modifier,
    onTeamNameChanged: (team: Int, name: String) -> Unit = { _, _ -> },
    onPlayerNameChanged: (team: Int, seat: Int, name: String) -> Unit = { _, _, _ -> },
    onEndModeChanged: (BeloteEndMode) -> Unit = {},
    onEndValueChanged: (String) -> Unit = {},
    onStartParty: () -> Unit = {}
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        ScreenHeader(
            title = stringResource(R.string.belote_setup_title),
            titleTag = "belote_setup_title",
            modifier = Modifier.padding(top = 8.dp)
        )
        state.teamNames.indices.forEach { team ->
            TeamCard(
                team = team,
                name = state.teamNames[team],
                players = state.playerNames[team],
                onTeamNameChanged = onTeamNameChanged,
                onPlayerNameChanged = onPlayerNameChanged
            )
        }
        EndRuleSection(state = state, onEndModeChanged = onEndModeChanged, onEndValueChanged = onEndValueChanged)
        TableOrder(state.tableOrder, state.teamNames)
        state.creationErrorRes?.let { errorRes ->
            Text(
                text = stringResource(errorRes),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.testTag("belote_setup_error")
            )
        }
        Button(
            onClick = onStartParty,
            enabled = state.canStart,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .testTag("belote_setup_start")
        ) {
            Text(text = stringResource(R.string.setup_start_button))
        }
    }
}

@Composable
private fun TeamCard(
    team: Int,
    name: String,
    players: List<String>,
    onTeamNameChanged: (Int, String) -> Unit,
    onPlayerNameChanged: (Int, Int, String) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(16.dp)) {
            NameField(
                value = name,
                onValueChange = { onTeamNameChanged(team, it) },
                label = stringResource(R.string.belote_setup_team_name, team + 1),
                tag = "belote_setup_team_$team"
            )
            players.forEachIndexed { seat, player ->
                NameField(
                    value = player,
                    onValueChange = { onPlayerNameChanged(team, seat, it) },
                    label = stringResource(R.string.belote_setup_player_hint, seat + 1),
                    tag = "belote_setup_player_${team}_$seat"
                )
            }
        }
    }
}

@Composable
private fun NameField(value: String, onValueChange: (String) -> Unit, label: String, tag: String) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
        modifier = Modifier
            .fillMaxWidth()
            .testTag(tag)
    )
}

@Composable
private fun EndRuleSection(
    state: BeloteSetupUiState,
    onEndModeChanged: (BeloteEndMode) -> Unit,
    onEndValueChanged: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = stringResource(R.string.belote_setup_end_title), style = MaterialTheme.typography.titleMedium)
        val modes = BeloteEndMode.entries
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            modes.forEachIndexed { index, mode ->
                SegmentedButton(
                    selected = state.endMode == mode,
                    onClick = { onEndModeChanged(mode) },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = modes.size),
                    colors = beloteSegmentedColors(),
                    modifier = Modifier.testTag("belote_setup_end_mode_${mode.name}")
                ) {
                    Text(text = stringResource(mode.labelRes))
                }
            }
        }
        val isError = state.endRule == null
        OutlinedTextField(
            value = state.endValueInput,
            onValueChange = onEndValueChanged,
            label = { Text(stringResource(state.endMode.labelRes)) },
            singleLine = true,
            isError = isError,
            supportingText = if (isError) {
                { Text(stringResource(R.string.belote_setup_end_value_error)) }
            } else {
                null
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("belote_setup_end_value")
        )
    }
}

/** The selected segment on an accent tint, like the rest of the app, rather than Material's default. */
@Composable
internal fun beloteSegmentedColors(): SegmentedButtonColors = SegmentedButtonDefaults.colors(
    activeContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
    activeContentColor = MaterialTheme.colorScheme.onSurface,
    activeBorderColor = MaterialTheme.colorScheme.outline,
    inactiveContainerColor = MaterialTheme.colorScheme.surface,
    inactiveContentColor = MaterialTheme.colorScheme.onSurface,
    inactiveBorderColor = MaterialTheme.colorScheme.outline
)

private val BeloteEndMode.labelRes: Int
    get() = when (this) {
        BeloteEndMode.TARGET_SCORE -> R.string.belote_setup_end_target
        BeloteEndMode.ROUND_COUNT -> R.string.belote_setup_end_rounds
    }

/** A1, B1, A2, B2, a name not entered yet shown as its Équipe and seat (« Nous 2 »); the first one deals. */
@Composable
private fun TableOrder(tableOrder: List<String>, teamNames: List<String>) {
    val seats = tableOrder.mapIndexed { index, name ->
        name.trim().ifBlank { stringResource(R.string.belote_setup_seat_placeholder, teamNames[index % 2], index / 2 + 1) }
    }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(text = stringResource(R.string.belote_setup_table_title), style = MaterialTheme.typography.titleMedium)
        Text(
            text = seats.joinToString(" → "),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.testTag("belote_setup_table_order")
        )
        Text(
            text = stringResource(R.string.belote_setup_first_dealer, seats.first()),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.testTag("belote_setup_first_dealer")
        )
    }
}

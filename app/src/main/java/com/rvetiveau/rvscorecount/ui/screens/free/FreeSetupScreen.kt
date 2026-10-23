package com.rvetiveau.rvscorecount.ui.screens.free

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.rvetiveau.rvscorecount.R
import com.rvetiveau.rvscorecount.domain.model.WinDirection
import com.rvetiveau.rvscorecount.ui.components.HistoryButton
import com.rvetiveau.rvscorecount.ui.components.PlayerSuggestionChips
import com.rvetiveau.rvscorecount.ui.components.ScreenHeader
import com.rvetiveau.rvscorecount.ui.screens.belote.beloteSegmentedColors
import com.rvetiveau.rvscorecount.ui.screens.setup.ParticipantDraftEditor
import java.util.UUID

/** What the creation of a Partie of Jeu libre reports to its ViewModel (Story 11.2). */
data class FreeSetupActions(
    val onGameNameChanged: (String) -> Unit = {},
    val onGameNameSuggestionSelected: (String) -> Unit = {},
    val onNameChanged: (String) -> Unit = {},
    val onAddParticipant: () -> Unit = {},
    val onSuggestionSelected: (String) -> Unit = {},
    val onRemoveParticipant: (UUID) -> Unit = {},
    val onMoveParticipant: (Int, Int) -> Unit = { _, _ -> },
    val onWinDirectionChanged: (WinDirection) -> Unit = {},
    val onEndModeChanged: (FreeEndMode) -> Unit = {},
    val onEndValueChanged: (String) -> Unit = {},
    val onStartParty: () -> Unit = {},
    val onHistorySelected: () -> Unit = {}
)

/**
 * Stateless creation of a Partie of Jeu libre (Story 11.2): the game's name, the Participants (the
 * same editor as the Papayoo, the list taking the room left), then who wins and how it ends.
 */
@Composable
fun FreeSetupScreen(
    state: FreeSetupUiState,
    modifier: Modifier = Modifier,
    actions: FreeSetupActions = FreeSetupActions()
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .imePadding()
            .padding(horizontal = 16.dp)
    ) {
        ScreenHeader(
            title = stringResource(R.string.free_setup_title),
            titleTag = "free_setup_title",
            modifier = Modifier.padding(vertical = 8.dp)
        ) {
            HistoryButton(onClick = actions.onHistorySelected, modifier = Modifier.testTag("free_setup_history_action"))
        }

        OutlinedTextField(
            value = state.gameNameInput,
            onValueChange = actions.onGameNameChanged,
            label = { Text(stringResource(R.string.free_setup_game_label)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("free_setup_game_name")
        )
        PlayerSuggestionChips(
            names = state.gameNameSuggestions,
            onNameSelected = actions.onGameNameSuggestionSelected,
            descriptionRes = R.string.free_setup_game_suggestion,
            tagPrefix = "free_setup_game_suggestion",
            modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
        )

        val messageRes = state.messageRes ?: R.string.setup_max_players_reached.takeIf { state.isMaxReached }
        ParticipantDraftEditor(
            nameInput = state.nameInput,
            canAdd = state.canAdd,
            isNameError = state.messageRes == R.string.setup_error_empty_name,
            suggestions = state.nameSuggestions,
            message = messageRes?.let { res ->
                if (res == R.string.setup_max_players_reached) stringResource(res, state.maxPlayers) else stringResource(res)
            },
            countText = stringResource(R.string.setup_participant_count, state.participants.size, state.minPlayers, state.maxPlayers),
            participants = state.participants,
            onNameChanged = actions.onNameChanged,
            onAddParticipant = actions.onAddParticipant,
            onSuggestionSelected = actions.onSuggestionSelected,
            onMoveParticipant = actions.onMoveParticipant,
            onRemoveParticipant = actions.onRemoveParticipant
        )

        RulesSection(state, actions)

        state.creationErrorRes?.let { errorRes ->
            Text(
                text = stringResource(errorRes),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.testTag("free_setup_error")
            )
        }
        Button(
            onClick = actions.onStartParty,
            enabled = state.canStart,
            modifier = Modifier
                .padding(vertical = 12.dp)
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .testTag("free_setup_start")
        ) {
            Text(text = stringResource(R.string.setup_start_button))
        }
    }
}

/** « Qui gagne ? » and « Fin de la partie », compact so that the list of Participants keeps its room. */
@Composable
private fun RulesSection(state: FreeSetupUiState, actions: FreeSetupActions) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 8.dp)) {
        Text(text = stringResource(R.string.free_setup_win_title), style = MaterialTheme.typography.titleSmall)
        Segments(
            options = WinDirection.entries,
            selected = state.winDirection,
            label = { stringResource(if (it == WinDirection.LOWEST_WINS) R.string.free_win_lowest else R.string.free_win_highest) },
            tag = { "free_setup_win_${it.name}" },
            onSelected = actions.onWinDirectionChanged
        )
        Text(text = stringResource(R.string.free_setup_end_title), style = MaterialTheme.typography.titleSmall)
        Segments(
            options = FreeEndMode.entries,
            selected = state.endMode,
            label = { stringResource(it.labelRes) },
            tag = { "free_setup_end_mode_${it.name}" },
            onSelected = actions.onEndModeChanged
        )
        if (state.endMode != FreeEndMode.MANUAL) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(
                        if (state.endMode == FreeEndMode.TARGET_SCORE) R.string.free_end_target_label else R.string.free_end_rounds_label
                    ),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = state.endValueInput,
                    onValueChange = actions.onEndValueChanged,
                    singleLine = true,
                    isError = !state.isEndValueValid,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .width(112.dp)
                        .testTag("free_setup_end_value")
                )
            }
            if (!state.isEndValueValid) {
                Text(
                    text = stringResource(R.string.belote_setup_end_value_error),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.testTag("free_setup_end_value_error")
                )
            }
        }
    }
}

@Composable
internal fun <T> Segments(
    options: List<T>,
    selected: T,
    label: @Composable (T) -> String,
    tag: (T) -> String,
    onSelected: (T) -> Unit
) {
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        options.forEachIndexed { index, option ->
            SegmentedButton(
                selected = option == selected,
                onClick = { onSelected(option) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                colors = beloteSegmentedColors(),
                modifier = Modifier.testTag(tag(option))
            ) {
                Text(text = label(option))
            }
        }
    }
}

private val FreeEndMode.labelRes: Int
    get() = when (this) {
        FreeEndMode.MANUAL -> R.string.free_end_manual
        FreeEndMode.TARGET_SCORE -> R.string.free_end_target
        FreeEndMode.ROUND_COUNT -> R.string.free_end_rounds
    }

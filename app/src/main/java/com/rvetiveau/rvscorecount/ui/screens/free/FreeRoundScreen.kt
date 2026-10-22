package com.rvetiveau.rvscorecount.ui.screens.free

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.rvetiveau.rvscorecount.R
import com.rvetiveau.rvscorecount.ui.components.ScoreField
import com.rvetiveau.rvscorecount.ui.components.ScreenHeader
import com.rvetiveau.rvscorecount.ui.screens.belote.Centered
import java.util.UUID

/**
 * Stateless saisie, or correction, of a Manche of Jeu libre (Story 11.3): one score field per
 * Joueur with its +/− button, then « Valider la manche » once every Joueur has a score.
 */
@Composable
fun FreeRoundScreen(
    state: FreeRoundUiState,
    modifier: Modifier = Modifier,
    onScoreChanged: (UUID, String) -> Unit = { _, _ -> },
    onToggleSign: (UUID) -> Unit = {},
    onValidate: () -> Unit = {}
) {
    when (state) {
        FreeRoundUiState.Loading -> Centered(modifier) {
            CircularProgressIndicator(modifier = Modifier.testTag("free_round_loading"))
        }
        FreeRoundUiState.Unavailable -> Centered(modifier) {
            Text(text = stringResource(R.string.round_entry_load_error), modifier = Modifier.testTag("free_round_unavailable"))
        }
        is FreeRoundUiState.Loaded -> LoadedRound(state, modifier, onScoreChanged, onToggleSign, onValidate)
    }
}

@Composable
private fun LoadedRound(
    state: FreeRoundUiState.Loaded,
    modifier: Modifier,
    onScoreChanged: (UUID, String) -> Unit,
    onToggleSign: (UUID) -> Unit,
    onValidate: () -> Unit
) {
    val focusManager = LocalFocusManager.current
    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
            .padding(16.dp)
    ) {
        ScreenHeader(
            title = if (state.isCorrection) {
                stringResource(R.string.belote_round_correction_title, state.roundNumber)
            } else {
                stringResource(R.string.belote_round_title, state.roundNumber)
            },
            titleTag = "free_round_title",
            modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
        )
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = stringResource(R.string.free_round_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            state.fields.forEachIndexed { index, field ->
                ScoreField(
                    participantId = field.participantId,
                    participantName = field.name,
                    value = field.text,
                    onValueChange = { onScoreChanged(field.participantId, it) },
                    imeAction = if (index == state.fields.lastIndex) ImeAction.Done else ImeAction.Next,
                    onImeAction = { action ->
                        if (action == ImeAction.Next) focusManager.moveFocus(FocusDirection.Down) else focusManager.clearFocus()
                    },
                    isError = field.isOutOfBounds,
                    onToggleSign = { onToggleSign(field.participantId) }
                )
            }
            if (state.fields.any { it.isOutOfBounds }) {
                Text(
                    text = stringResource(R.string.free_round_out_of_bounds),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.testTag("free_round_out_of_bounds")
                )
            }
            state.errorRes?.let { error ->
                Text(
                    text = stringResource(error),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.testTag("free_round_error")
                )
            }
        }
        Button(
            onClick = onValidate,
            enabled = state.canValidate,
            modifier = Modifier
                .padding(top = 16.dp)
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .testTag("free_round_validate")
        ) {
            Text(
                text = stringResource(if (state.isCorrection) R.string.belote_round_save_correction else R.string.belote_round_validate)
            )
        }
    }
}

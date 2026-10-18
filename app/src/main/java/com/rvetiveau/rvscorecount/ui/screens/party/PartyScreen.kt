package com.rvetiveau.rvscorecount.ui.screens.party

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.rvetiveau.rvscorecount.R

/**
 * Stateless minimal Partie en cours screen (AC 5, 10): title, Donneur and empty state only.
 * Classement temps réel, "Nouvelle manche", "Fin de partie" and later flows are delivered by
 * Epics 3-5.
 */
@Composable
fun PartyScreen(
    state: PartyUiState,
    modifier: Modifier = Modifier,
    onRetry: () -> Unit = {},
    onStartRound: () -> Unit = {}
) {
    when (state) {
        is PartyUiState.Loading -> CenteredMessage(modifier) {
            CircularProgressIndicator(modifier = Modifier.testTag("party_loading"))
        }

        is PartyUiState.PartyNotFound -> CenteredMessage(modifier) {
            Text(
                text = stringResource(R.string.party_not_found),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.testTag("party_not_found")
            )
        }

        is PartyUiState.LoadError -> CenteredMessage(modifier) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = stringResource(R.string.party_load_error),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.testTag("party_load_error")
                )
                Button(onClick = onRetry, modifier = Modifier.padding(top = 16.dp)) {
                    Text(text = stringResource(R.string.root_error_retry))
                }
            }
        }

        is PartyUiState.InconsistentParticipants -> CenteredMessage(modifier) {
            Text(
                text = stringResource(R.string.party_inconsistent_participants),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.testTag("party_inconsistent")
            )
        }

        is PartyUiState.Loaded -> Column(
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text(
                text = stringResource(R.string.party_dealer_title, state.roundNumber, state.dealerName),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier
                    .padding(vertical = 16.dp)
                    .testTag("party_dealer_title")
            )
            if (state.roundNumber == 1) {
                Text(
                    text = stringResource(R.string.party_empty_state),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.testTag("party_empty_state")
                )
            }
            if (state.canStartRound) {
                Button(
                    onClick = onStartRound,
                    modifier = Modifier
                        .padding(top = 16.dp)
                        .fillMaxWidth()
                        .heightIn(min = 56.dp)
                        .testTag("party_start_round_button")
                ) {
                    Text(text = stringResource(R.string.party_start_round_button))
                }
            }
        }
    }
}

@Composable
private fun CenteredMessage(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        content()
    }
}

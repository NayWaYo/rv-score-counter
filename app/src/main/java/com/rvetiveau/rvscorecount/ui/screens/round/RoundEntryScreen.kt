package com.rvetiveau.rvscorecount.ui.screens.round

import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.rvetiveau.rvscorecount.R
import com.rvetiveau.rvscorecount.ui.components.DieRoller

/**
 * Stateless Saisie de Manche screen (AC 2, 4-7, 10): only the die roll for Story 3.1. Score
 * fields, the running sum and validation belong to Stories 3.2-3.4.
 */
@Composable
fun RoundEntryScreen(
    state: RoundEntryUiState,
    onRoll: () -> Unit,
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
        Text(
            text = stringResource(R.string.round_entry_suit_label),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .padding(bottom = 8.dp)
                .testTag("round_entry_suit_label")
        )
        DieRoller(
            suit = state.suit,
            onRoll = onRoll,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
                .testTag("round_entry_die_roller")
        )
    }
}

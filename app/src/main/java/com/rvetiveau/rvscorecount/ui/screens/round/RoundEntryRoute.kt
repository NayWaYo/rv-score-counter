package com.rvetiveau.rvscorecount.ui.screens.round

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.util.UUID

/**
 * Compose/Hilt adapter for the Saisie de Manche screen (AC 1, 5): [partyId] stays the typed
 * destination argument reserved for the score-entry stories, unused by this story's die-only UI.
 */
@Composable
fun RoundEntryRoute(
    partyId: UUID,
    modifier: Modifier = Modifier,
    viewModel: RoundEntryViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    RoundEntryScreen(state = state, onRoll = viewModel::onRoll, modifier = modifier)
}

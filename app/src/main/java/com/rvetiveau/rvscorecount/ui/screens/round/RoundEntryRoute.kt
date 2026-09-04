package com.rvetiveau.rvscorecount.ui.screens.round

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.util.UUID

/**
 * Compose/Hilt adapter for the Saisie de Manche screen (AC 1): loads Room state for [partyId]
 * once via [RoundEntryViewModel.initialize].
 */
@Composable
fun RoundEntryRoute(
    partyId: UUID,
    modifier: Modifier = Modifier,
    viewModel: RoundEntryViewModel = hiltViewModel()
) {
    LaunchedEffect(partyId) { viewModel.initialize(partyId) }

    val state by viewModel.uiState.collectAsStateWithLifecycle()

    RoundEntryScreen(
        state = state,
        onRoll = viewModel::onRoll,
        onScoreChanged = viewModel::onScoreChanged,
        onRetry = { viewModel.retry(partyId) },
        modifier = modifier
    )
}

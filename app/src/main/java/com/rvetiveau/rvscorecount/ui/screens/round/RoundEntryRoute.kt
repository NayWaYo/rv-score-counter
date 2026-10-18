package com.rvetiveau.rvscorecount.ui.screens.round

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rvetiveau.rvscorecount.ui.navigation.NavigationEvent
import java.util.UUID

/**
 * Compose/Hilt adapter for the Saisie de Manche screen (AC 1): loads Room state for [partyId]
 * once via [RoundEntryViewModel.initialize] and relays the ViewModel's one-shot
 * [NavigationEvent]s to [onNavigate].
 */
@Composable
fun RoundEntryRoute(
    partyId: UUID,
    onNavigate: (NavigationEvent) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RoundEntryViewModel = hiltViewModel()
) {
    LaunchedEffect(partyId) { viewModel.initialize(partyId) }

    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val currentOnNavigate by rememberUpdatedState(onNavigate)

    LaunchedEffect(viewModel) {
        viewModel.navigationEvents.collect { event -> currentOnNavigate(event) }
    }

    RoundEntryScreen(
        state = state,
        onRoll = viewModel::onRoll,
        onScoreChanged = viewModel::onScoreChanged,
        onRetry = { viewModel.retry(partyId) },
        onValidate = viewModel::onValidate,
        onSevenHolderSelected = viewModel::onSevenHolderSelected,
        onPanelSelected = viewModel::onPanelSelected,
        modifier = modifier
    )
}

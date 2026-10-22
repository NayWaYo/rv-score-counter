package com.rvetiveau.rvscorecount.ui.screens.free

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.keepScreenOn
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rvetiveau.rvscorecount.ui.navigation.NavigationEvent
import java.util.UUID

/**
 * Compose/Hilt adapter for a Partie of Jeu libre (AD-4), or for its Résultats when [isResults];
 * the screen stays on while it is in progress.
 */
@Composable
fun FreePartyRoute(
    partyId: UUID,
    onNavigate: (NavigationEvent) -> Unit,
    modifier: Modifier = Modifier,
    isResults: Boolean = false,
    viewModel: FreePartyViewModel = hiltViewModel()
) {
    LaunchedEffect(partyId) { viewModel.initialize(partyId, isResults) }

    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val currentOnNavigate by rememberUpdatedState(onNavigate)

    LaunchedEffect(viewModel) {
        viewModel.navigationEvents.collect { event -> currentOnNavigate(event) }
    }

    val isFinished = (state as? FreePartyUiState.Loaded)?.isFinished == true
    FreePartyScreen(
        state = state,
        actions = FreePartyActions(
            onRetry = viewModel::retry,
            onPlayRound = viewModel::onPlayRound,
            onRoundSelected = viewModel::onRoundSelected,
            onEndPartyClick = viewModel::onEndPartyClick,
            onEndPartyDismissed = viewModel::onEndPartyDismissed,
            onEndPartyConfirmed = viewModel::onEndPartyConfirmed,
            onBackHome = viewModel::onBackHome,
            onReplay = viewModel::onReplay
        ),
        modifier = if (isFinished) modifier else modifier.keepScreenOn()
    )
}

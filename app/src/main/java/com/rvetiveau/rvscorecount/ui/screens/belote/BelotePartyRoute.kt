package com.rvetiveau.rvscorecount.ui.screens.belote

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

/** Compose/Hilt adapter for a Partie of Belote in progress (AD-4). */
@Composable
fun BelotePartyRoute(
    partyId: UUID,
    onNavigate: (NavigationEvent) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BelotePartyViewModel = hiltViewModel()
) {
    LaunchedEffect(partyId) { viewModel.initialize(partyId) }

    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val currentOnNavigate by rememberUpdatedState(onNavigate)

    LaunchedEffect(viewModel) {
        viewModel.navigationEvents.collect { event -> currentOnNavigate(event) }
    }

    BelotePartyScreen(
        state = state,
        onRetry = viewModel::retry,
        onPlayRound = viewModel::onPlayRound,
        onRoundSelected = viewModel::onRoundSelected,
        onShowStats = viewModel::onShowStats,
        modifier = modifier.keepScreenOn()
    )
}

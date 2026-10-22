package com.rvetiveau.rvscorecount.ui.screens.players

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rvetiveau.rvscorecount.ui.navigation.NavigationEvent

/** Compose/Hilt adapter for « Joueurs » (Story 10.2): collects state and relays navigation events. */
@Composable
fun PlayersRoute(
    onNavigate: (NavigationEvent) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PlayersViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val currentOnNavigate by rememberUpdatedState(onNavigate)

    LaunchedEffect(viewModel) {
        viewModel.navigationEvents.collect { event -> currentOnNavigate(event) }
    }

    PlayersScreen(
        state = state,
        onPlayerSelected = viewModel::onPlayerSelected,
        onRenameRequested = viewModel::onRenameRequested,
        onRenameInputChanged = viewModel::onRenameInputChanged,
        onRenameConfirmed = viewModel::onRenameConfirmed,
        onRenameDismissed = viewModel::onRenameDismissed,
        modifier = modifier
    )
}

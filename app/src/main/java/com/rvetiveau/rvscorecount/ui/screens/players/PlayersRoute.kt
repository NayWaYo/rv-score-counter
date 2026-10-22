package com.rvetiveau.rvscorecount.ui.screens.players

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/** Compose/Hilt adapter for « Joueurs » (Story 10.2). */
@Composable
fun PlayersRoute(
    modifier: Modifier = Modifier,
    viewModel: PlayersViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    PlayersScreen(
        state = state,
        onRenameRequested = viewModel::onRenameRequested,
        onRenameInputChanged = viewModel::onRenameInputChanged,
        onRenameConfirmed = viewModel::onRenameConfirmed,
        onRenameDismissed = viewModel::onRenameDismissed,
        modifier = modifier
    )
}

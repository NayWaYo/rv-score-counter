package com.rvetiveau.rvscorecount.ui.screens.profile

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rvetiveau.rvscorecount.ui.navigation.NavigationEvent
import java.util.UUID

/** Compose/Hilt adapter for a Joueur's profile (Stories 10.3, 10.4): relays navigation after a deletion. */
@Composable
fun PlayerProfileRoute(
    playerId: UUID,
    onNavigate: (NavigationEvent) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PlayerProfileViewModel = hiltViewModel()
) {
    LaunchedEffect(playerId) { viewModel.initialize(playerId) }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val currentOnNavigate by rememberUpdatedState(onNavigate)

    LaunchedEffect(viewModel) {
        viewModel.navigationEvents.collect { event -> currentOnNavigate(event) }
    }

    PlayerProfileScreen(
        state = state,
        actions = ProfileActions(
            onMergeRequested = viewModel::onMergeRequested,
            onMergeTargetSelected = viewModel::onMergeTargetSelected,
            onMergeConfirmed = viewModel::onMergeConfirmed,
            onDeleteRequested = viewModel::onDeleteRequested,
            onDeleteConfirmed = viewModel::onDeleteConfirmed,
            onDialogDismissed = viewModel::onDialogDismissed
        ),
        modifier = modifier
    )
}

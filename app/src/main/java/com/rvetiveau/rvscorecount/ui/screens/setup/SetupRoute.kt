package com.rvetiveau.rvscorecount.ui.screens.setup

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rvetiveau.rvscorecount.ui.navigation.NavigationEvent

/**
 * Compose/Hilt adapter for Création de Partie (AD-4): initializes the draft once per [gameId]
 * and relays user intents and one-shot [NavigationEvent]s to [SetupViewModel] / [onNavigate].
 */
@Composable
fun SetupRoute(
    gameId: String,
    onNavigate: (NavigationEvent) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SetupViewModel = hiltViewModel()
) {
    LaunchedEffect(gameId) { viewModel.initialize(gameId) }

    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val currentOnNavigate by rememberUpdatedState(onNavigate)

    LaunchedEffect(viewModel) {
        viewModel.navigationEvents.collect { event -> currentOnNavigate(event) }
    }

    SetupScreen(
        state = state,
        onNameChanged = viewModel::onNameChanged,
        onAddParticipant = viewModel::onAddParticipant,
        onRemoveParticipant = viewModel::onRemoveParticipant,
        onMoveParticipant = viewModel::onMoveParticipant,
        onStartParty = viewModel::onStartParty,
        onHistoryEntrySelected = viewModel::onHistoryEntrySelected,
        modifier = modifier
    )
}

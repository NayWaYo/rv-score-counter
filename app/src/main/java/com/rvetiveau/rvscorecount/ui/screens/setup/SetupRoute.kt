package com.rvetiveau.rvscorecount.ui.screens.setup

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * Compose/Hilt adapter for Création de Partie (AD-4): initializes the draft once per [gameId]
 * and relays user intents to [SetupViewModel]. "Commencer" has no behavior yet — its
 * confirmation and navigation are wired by Story 2.3 on this same ViewModel.
 */
@Composable
fun SetupRoute(
    gameId: String,
    modifier: Modifier = Modifier,
    viewModel: SetupViewModel = hiltViewModel()
) {
    LaunchedEffect(gameId) { viewModel.initialize(gameId) }

    val state by viewModel.uiState.collectAsStateWithLifecycle()

    SetupScreen(
        state = state,
        onNameChanged = viewModel::onNameChanged,
        onAddParticipant = viewModel::onAddParticipant,
        onRemoveParticipant = viewModel::onRemoveParticipant,
        onMoveParticipant = viewModel::onMoveParticipant,
        onStartParty = {},
        modifier = modifier
    )
}

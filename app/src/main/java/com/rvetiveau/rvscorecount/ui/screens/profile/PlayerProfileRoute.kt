package com.rvetiveau.rvscorecount.ui.screens.profile

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.util.UUID

/** Compose/Hilt adapter for a Joueur's profile (Story 10.3). */
@Composable
fun PlayerProfileRoute(
    playerId: UUID,
    modifier: Modifier = Modifier,
    viewModel: PlayerProfileViewModel = hiltViewModel()
) {
    LaunchedEffect(playerId) { viewModel.initialize(playerId) }
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    PlayerProfileScreen(state = state, modifier = modifier)
}

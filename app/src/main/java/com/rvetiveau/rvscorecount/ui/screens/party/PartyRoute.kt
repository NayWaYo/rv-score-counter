package com.rvetiveau.rvscorecount.ui.screens.party

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.util.UUID

/**
 * Compose/Hilt adapter for the freshly created Partie (AC 5, 10): loads Room state for
 * [partyId] once and renders the minimal Manche 1 / Donneur screen.
 */
@Composable
fun PartyRoute(
    partyId: UUID,
    modifier: Modifier = Modifier,
    viewModel: PartyViewModel = hiltViewModel()
) {
    LaunchedEffect(partyId) { viewModel.initialize(partyId) }

    val state by viewModel.uiState.collectAsStateWithLifecycle()

    PartyScreen(state = state, onRetry = { viewModel.retry(partyId) }, modifier = modifier)
}

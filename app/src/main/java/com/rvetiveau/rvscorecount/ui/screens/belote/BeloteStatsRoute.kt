package com.rvetiveau.rvscorecount.ui.screens.belote

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.util.UUID

/** Compose/Hilt adapter for the Statistiques of a Partie of Belote (AD-4). */
@Composable
fun BeloteStatsRoute(
    partyId: UUID,
    modifier: Modifier = Modifier,
    viewModel: BeloteStatsViewModel = hiltViewModel()
) {
    LaunchedEffect(partyId) { viewModel.initialize(partyId) }

    val state by viewModel.uiState.collectAsStateWithLifecycle()

    BeloteStatsScreen(state = state, modifier = modifier)
}

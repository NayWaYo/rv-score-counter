package com.rvetiveau.rvscorecount.ui.screens.free

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.keepScreenOn
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.util.UUID

/** Compose/Hilt adapter for a Partie of Jeu libre (Story 11.2); the screen stays on while it is shown. */
@Composable
fun FreePartyRoute(
    partyId: UUID,
    modifier: Modifier = Modifier,
    viewModel: FreePartyViewModel = hiltViewModel()
) {
    LaunchedEffect(partyId) { viewModel.initialize(partyId) }
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    FreePartyScreen(state = state, modifier = modifier.keepScreenOn())
}

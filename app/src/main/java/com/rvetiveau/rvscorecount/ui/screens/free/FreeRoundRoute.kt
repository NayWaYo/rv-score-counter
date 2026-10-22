package com.rvetiveau.rvscorecount.ui.screens.free

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

/** Compose/Hilt adapter for the saisie or correction of a Manche of Jeu libre (AD-4). */
@Composable
fun FreeRoundRoute(
    partyId: UUID,
    correctedRoundNumber: Int?,
    onNavigate: (NavigationEvent) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FreeRoundViewModel = hiltViewModel()
) {
    LaunchedEffect(partyId, correctedRoundNumber) { viewModel.initialize(partyId, correctedRoundNumber) }

    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val currentOnNavigate by rememberUpdatedState(onNavigate)

    LaunchedEffect(viewModel) {
        viewModel.navigationEvents.collect { event -> currentOnNavigate(event) }
    }

    FreeRoundScreen(
        state = state,
        onScoreChanged = viewModel::onScoreChanged,
        onToggleSign = viewModel::onToggleSign,
        onValidate = viewModel::onValidate,
        modifier = modifier.keepScreenOn()
    )
}

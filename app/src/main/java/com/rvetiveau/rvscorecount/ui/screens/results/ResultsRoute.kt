package com.rvetiveau.rvscorecount.ui.screens.results

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rvetiveau.rvscorecount.ui.navigation.NavigationEvent
import java.util.UUID

/**
 * Compose/Hilt adapter for the Résultats finaux: loads [partyId] once, relays navigation and
 * sends the system back to the Accueil rather than to the finished Partie (UX navigation).
 */
@Composable
fun ResultsRoute(
    partyId: UUID,
    onNavigate: (NavigationEvent) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ResultsViewModel = hiltViewModel()
) {
    LaunchedEffect(partyId) { viewModel.initialize(partyId) }

    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val currentOnNavigate by rememberUpdatedState(onNavigate)

    LaunchedEffect(viewModel) {
        viewModel.navigationEvents.collect { event -> currentOnNavigate(event) }
    }
    BackHandler(onBack = viewModel::onBackToHome)

    ResultsScreen(
        state = state,
        onRetry = { viewModel.retry(partyId) },
        onBackToHome = viewModel::onBackToHome,
        onReplay = viewModel::onReplay,
        modifier = modifier
    )
}

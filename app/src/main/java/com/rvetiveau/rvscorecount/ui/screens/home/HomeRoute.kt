package com.rvetiveau.rvscorecount.ui.screens.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rvetiveau.rvscorecount.ui.navigation.NavigationEvent

/**
 * Compose/Hilt adapter for the Accueil (AD-4): collects [HomeUiState] and relays the
 * ViewModel's one-shot [NavigationEvent]s to the navigation boundary via [onNavigate].
 */
@Composable
fun HomeRoute(
    onNavigate: (NavigationEvent) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val currentOnNavigate by rememberUpdatedState(onNavigate)

    LaunchedEffect(viewModel) {
        viewModel.navigationEvents.collect { event -> currentOnNavigate(event) }
    }

    HomeScreen(
        state = state,
        onGameSelected = viewModel::onGameSelected,
        onHistorySelected = viewModel::onHistorySelected,
        onPlayersSelected = viewModel::onPlayersSelected,
        onBackupSelected = viewModel::onBackupSelected,
        modifier = modifier
    )
}

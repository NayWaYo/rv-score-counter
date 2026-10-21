package com.rvetiveau.rvscorecount.ui.screens.belote

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rvetiveau.rvscorecount.R
import com.rvetiveau.rvscorecount.ui.navigation.NavigationEvent

/** Compose/Hilt adapter for the creation of a Partie of Belote (AD-4). */
@Composable
fun BeloteSetupRoute(
    onNavigate: (NavigationEvent) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BeloteSetupViewModel = hiltViewModel()
) {
    val defaultTeamNames = listOf(
        stringResource(R.string.belote_team_default_first),
        stringResource(R.string.belote_team_default_second)
    )
    LaunchedEffect(viewModel) { viewModel.initialize(defaultTeamNames) }

    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val currentOnNavigate by rememberUpdatedState(onNavigate)

    LaunchedEffect(viewModel) {
        viewModel.navigationEvents.collect { event -> currentOnNavigate(event) }
    }

    BeloteSetupScreen(
        state = state,
        onTeamNameChanged = viewModel::onTeamNameChanged,
        onPlayerNameChanged = viewModel::onPlayerNameChanged,
        onEndModeChanged = viewModel::onEndModeChanged,
        onEndValueChanged = viewModel::onEndValueChanged,
        onStartParty = viewModel::onStartParty,
        modifier = modifier
    )
}

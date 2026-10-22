package com.rvetiveau.rvscorecount.ui.screens.free

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rvetiveau.rvscorecount.ui.navigation.NavigationEvent

/** Compose/Hilt adapter for the creation of a Partie of Jeu libre (Story 11.2). */
@Composable
fun FreeSetupRoute(
    onNavigate: (NavigationEvent) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FreeSetupViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val currentOnNavigate by rememberUpdatedState(onNavigate)

    LaunchedEffect(viewModel) {
        viewModel.navigationEvents.collect { event -> currentOnNavigate(event) }
    }

    FreeSetupScreen(
        state = state,
        actions = FreeSetupActions(
            onGameNameChanged = viewModel::onGameNameChanged,
            onGameNameSuggestionSelected = viewModel::onGameNameSuggestionSelected,
            onNameChanged = viewModel::onNameChanged,
            onAddParticipant = viewModel::onAddParticipant,
            onSuggestionSelected = viewModel::onSuggestionSelected,
            onRemoveParticipant = viewModel::onRemoveParticipant,
            onMoveParticipant = viewModel::onMoveParticipant,
            onWinDirectionChanged = viewModel::onWinDirectionChanged,
            onEndModeChanged = viewModel::onEndModeChanged,
            onEndValueChanged = viewModel::onEndValueChanged,
            onStartParty = viewModel::onStartParty,
            onHistorySelected = viewModel::onHistorySelected
        ),
        modifier = modifier
    )
}

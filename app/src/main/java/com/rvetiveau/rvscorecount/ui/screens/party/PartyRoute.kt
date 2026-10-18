package com.rvetiveau.rvscorecount.ui.screens.party

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.rvetiveau.rvscorecount.ui.navigation.NavigationEvent
import java.util.UUID

/**
 * Compose/Hilt adapter for the Partie en cours: starts the Room observation for [partyId] once,
 * renders the Classement and relays the ViewModel's one-shot [NavigationEvent]s to [onNavigate].
 */
@Composable
fun PartyRoute(
    partyId: UUID,
    onNavigate: (NavigationEvent) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PartyViewModel = hiltViewModel()
) {
    LaunchedEffect(partyId) { viewModel.initialize(partyId) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.onScreenResumed()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val currentOnNavigate by rememberUpdatedState(onNavigate)

    LaunchedEffect(viewModel) {
        viewModel.navigationEvents.collect { event -> currentOnNavigate(event) }
    }

    PartyScreen(
        state = state,
        onRetry = { viewModel.retry(partyId) },
        onStartRound = viewModel::onStartRound,
        onAddParticipantClick = viewModel::onAddParticipantClick,
        onAdditionNameChanged = viewModel::onAdditionNameChanged,
        onAdditionNameConfirmed = viewModel::onAdditionNameConfirmed,
        onInsertionPositionSelected = viewModel::onInsertionPositionSelected,
        onAdditionCancelled = viewModel::onAdditionCancelled,
        onShowRoundHistory = viewModel::onShowRoundHistory,
        onHideRoundHistory = viewModel::onHideRoundHistory,
        onEndPartyClick = viewModel::onEndPartyClick,
        onEndPartyConfirmed = viewModel::onEndPartyConfirmed,
        onEndPartyDismissed = viewModel::onEndPartyDismissed,
        modifier = modifier
    )
}

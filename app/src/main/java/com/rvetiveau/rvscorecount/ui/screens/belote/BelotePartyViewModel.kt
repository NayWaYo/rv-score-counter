package com.rvetiveau.rvscorecount.ui.screens.belote

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rvetiveau.rvscorecount.domain.model.BeloteEndRule
import com.rvetiveau.rvscorecount.domain.model.BelotePartyState
import com.rvetiveau.rvscorecount.domain.model.PartyState
import com.rvetiveau.rvscorecount.domain.model.TeamSide
import com.rvetiveau.rvscorecount.domain.usecase.ObserveBelotePartyUseCase
import com.rvetiveau.rvscorecount.ui.navigation.NavigationEvent
import com.rvetiveau.rvscorecount.ui.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

/** Partie of Belote in progress (Story 7.3, AC 3, 5): observed from Room, rescored by the engine. */
@HiltViewModel
class BelotePartyViewModel @Inject constructor(
    private val observeBelotePartyUseCase: ObserveBelotePartyUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<BelotePartyUiState>(BelotePartyUiState.Loading)
    val uiState: StateFlow<BelotePartyUiState> = _uiState.asStateFlow()

    private val _navigationEvents = Channel<NavigationEvent>(Channel.BUFFERED)
    val navigationEvents: Flow<NavigationEvent> = _navigationEvents.receiveAsFlow()

    private var partyId: UUID? = null
    private var observation: Job? = null

    fun initialize(partyId: UUID) {
        if (this.partyId != null) return
        this.partyId = partyId
        observe()
    }

    fun retry() = observe()

    fun onPlayRound() = navigate { Routes.BeloteRound(it.toString()) }

    fun onRoundSelected(roundNumber: Int) = navigate { Routes.BeloteRound(it.toString(), roundNumber) }

    fun onShowStats() = navigate { Routes.BeloteStats(it.toString()) }

    private fun navigate(route: (UUID) -> Routes) {
        val id = partyId ?: return
        viewModelScope.launch { _navigationEvents.send(NavigationEvent.NavigateTo(route(id))) }
    }

    private fun observe() {
        val id = partyId ?: return
        observation?.cancel()
        _uiState.value = BelotePartyUiState.Loading
        observation = viewModelScope.launch {
            try {
                observeBelotePartyUseCase(id).collect { state ->
                    when {
                        state == null -> _uiState.value = BelotePartyUiState.NotFound
                        state.party.status == PartyState.FINISHED ->
                            _navigationEvents.send(NavigationEvent.ReplaceAll(Routes.BeloteResults(id.toString())))
                        else -> _uiState.value = state.toUiState()
                    }
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Throwable) {
                _uiState.value = BelotePartyUiState.LoadError
            }
        }
    }
}

internal fun BelotePartyState.toUiState(): BelotePartyUiState.Loaded {
    val totals = totals
    val teams = TeamSide.entries.map { side ->
        BeloteTeamUiModel(
            side = side,
            name = team(side).name,
            playerNames = playersOf(side).map { it.name },
            total = totals[side],
            isLeader = totals[side] > totals[side.opponent]
        )
    }
    val progress = when (val rule = endRule) {
        is BeloteEndRule.TargetScore -> {
            val best = maxOf(totals.first, totals.second)
            if (best >= rule.points) {
                BeloteProgress.ExtraRound
            } else {
                BeloteProgress.Target(rule.points, rule.points - best, teams.filter { it.total == best }.map { it.name })
            }
        }
        is BeloteEndRule.RoundCount ->
            if (rounds.size >= rule.rounds) BeloteProgress.ExtraRound else BeloteProgress.Rounds(rounds.size + 1, rule.rounds)
    }
    return BelotePartyUiState.Loaded(
        teams = teams,
        progress = progress,
        dealerName = dealer.name,
        pendingReserve = pendingReserve,
        rounds = rounds.zip(results) { round, result ->
            BeloteRoundRowUiModel(
                roundNumber = round.roundNumber,
                takerName = team(round.input.taker).name,
                outcome = result.outcome,
                firstPoints = result.points.first,
                secondPoints = result.points.second
            )
        },
        nextRoundNumber = rounds.size + 1
    )
}

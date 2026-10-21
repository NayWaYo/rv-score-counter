package com.rvetiveau.rvscorecount.ui.screens.belote

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rvetiveau.rvscorecount.R
import com.rvetiveau.rvscorecount.domain.model.BelotePartyStatus
import com.rvetiveau.rvscorecount.domain.model.PartyState
import com.rvetiveau.rvscorecount.domain.model.TeamSide
import com.rvetiveau.rvscorecount.domain.usecase.CreateBelotePartyUseCase
import com.rvetiveau.rvscorecount.domain.usecase.ObserveBelotePartyUseCase
import com.rvetiveau.rvscorecount.ui.navigation.NavigationEvent
import com.rvetiveau.rvscorecount.ui.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

sealed interface BeloteResultsUiState {
    data object Loading : BeloteResultsUiState
    data object Unavailable : BeloteResultsUiState

    /** [teams]: the winner first. */
    data class Loaded(
        val teams: List<BeloteTeamUiModel>,
        val isReplaying: Boolean = false,
        @StringRes val replayErrorRes: Int? = null
    ) : BeloteResultsUiState
}

/** Résultats of a finished Partie of Belote (Story 7.3, AC 6), and « Rejouer ». */
@HiltViewModel
class BeloteResultsViewModel @Inject constructor(
    private val observeBelotePartyUseCase: ObserveBelotePartyUseCase,
    private val createBelotePartyUseCase: CreateBelotePartyUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<BeloteResultsUiState>(BeloteResultsUiState.Loading)
    val uiState: StateFlow<BeloteResultsUiState> = _uiState.asStateFlow()

    private val _navigationEvents = Channel<NavigationEvent>(Channel.BUFFERED)
    val navigationEvents: Flow<NavigationEvent> = _navigationEvents.receiveAsFlow()

    private var partyId: UUID? = null

    fun initialize(partyId: UUID) {
        if (this.partyId != null) return
        this.partyId = partyId
        viewModelScope.launch {
            _uiState.value = try {
                val state = observeBelotePartyUseCase(partyId).first()
                val winner = (state?.status as? BelotePartyStatus.Won)?.winner
                if (state == null || winner == null || state.party.status != PartyState.FINISHED) {
                    BeloteResultsUiState.Unavailable
                } else {
                    val totals = state.totals
                    BeloteResultsUiState.Loaded(
                        teams = listOf(winner, winner.opponent).map { side: TeamSide ->
                            BeloteTeamUiModel(
                                side = side,
                                name = state.team(side).name,
                                playerNames = state.playersOf(side).map { it.name },
                                total = totals[side],
                                isLeader = side == winner
                            )
                        }
                    )
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Throwable) {
                BeloteResultsUiState.Unavailable
            }
        }
    }

    fun onReplay() {
        val state = _uiState.value as? BeloteResultsUiState.Loaded ?: return
        val id = partyId ?: return
        if (state.isReplaying) return
        _uiState.value = state.copy(isReplaying = true, replayErrorRes = null)
        viewModelScope.launch {
            createBelotePartyUseCase.fromParty(id).fold(
                onSuccess = { newPartyId ->
                    _navigationEvents.send(NavigationEvent.ReplaceAll(Routes.BeloteParty(newPartyId.toString())))
                },
                onFailure = {
                    _uiState.value = state.copy(isReplaying = false, replayErrorRes = R.string.results_replay_error)
                }
            )
        }
    }

    fun onBackToHome() {
        viewModelScope.launch { _navigationEvents.send(NavigationEvent.ReplaceAll(Routes.Home)) }
    }
}

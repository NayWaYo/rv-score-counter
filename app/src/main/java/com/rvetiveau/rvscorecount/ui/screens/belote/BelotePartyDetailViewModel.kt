package com.rvetiveau.rvscorecount.ui.screens.belote

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rvetiveau.rvscorecount.R
import com.rvetiveau.rvscorecount.domain.model.BelotePartyStatus
import com.rvetiveau.rvscorecount.domain.model.PartyState
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

sealed interface BelotePartyDetailUiState {
    data object Loading : BelotePartyDetailUiState

    /** Absent, still in progress, or without a winning Équipe. */
    data object Unavailable : BelotePartyDetailUiState

    /** [teams] in creation order, the winner as leader. */
    data class Loaded(
        val createdAt: Long,
        val teams: List<BeloteTeamUiModel>,
        val rounds: List<BeloteRoundRowUiModel>,
        val isReplaying: Boolean = false,
        @StringRes val replayErrorRes: Int? = null
    ) : BelotePartyDetailUiState
}

/** Détail of a finished Partie of Belote from the Historique (Story 7.4, AC 2), read-only. */
@HiltViewModel
class BelotePartyDetailViewModel @Inject constructor(
    private val observeBelotePartyUseCase: ObserveBelotePartyUseCase,
    private val createBelotePartyUseCase: CreateBelotePartyUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<BelotePartyDetailUiState>(BelotePartyDetailUiState.Loading)
    val uiState: StateFlow<BelotePartyDetailUiState> = _uiState.asStateFlow()

    private val _navigationEvents = Channel<NavigationEvent>(Channel.BUFFERED)
    val navigationEvents: Flow<NavigationEvent> = _navigationEvents.receiveAsFlow()

    private var partyId: UUID? = null

    fun initialize(partyId: UUID) {
        if (this.partyId != null) return
        this.partyId = partyId
        viewModelScope.launch {
            _uiState.value = try {
                val state = observeBelotePartyUseCase(partyId).first()
                    ?.takeIf { it.party.status == PartyState.FINISHED && it.status is BelotePartyStatus.Won }
                if (state == null) {
                    BelotePartyDetailUiState.Unavailable
                } else {
                    val party = state.toUiState()
                    BelotePartyDetailUiState.Loaded(state.party.createdAt, party.teams, party.rounds)
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Throwable) {
                BelotePartyDetailUiState.Unavailable
            }
        }
    }

    fun onReplay() {
        val state = _uiState.value as? BelotePartyDetailUiState.Loaded ?: return
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

    fun onShowStats() {
        val id = partyId ?: return
        viewModelScope.launch { _navigationEvents.send(NavigationEvent.NavigateTo(Routes.BeloteStats(id.toString()))) }
    }
}

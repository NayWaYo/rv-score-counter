package com.rvetiveau.rvscorecount.ui.screens.free

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rvetiveau.rvscorecount.domain.engine.GameIds
import com.rvetiveau.rvscorecount.domain.model.EndRule
import com.rvetiveau.rvscorecount.domain.model.Participant
import com.rvetiveau.rvscorecount.domain.model.WinDirection
import com.rvetiveau.rvscorecount.domain.repository.PartyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

sealed interface FreePartyUiState {
    data object Loading : FreePartyUiState
    data object LoadError : FreePartyUiState

    /** A Partie of Jeu libre: its rules and its Participants in table order, [dealerId] dealing next. */
    data class Loaded(
        val gameName: String,
        val winDirection: WinDirection,
        val endRule: EndRule?,
        val participants: List<Participant>,
        val dealerId: UUID?
    ) : FreePartyUiState
}

/** The Partie of Jeu libre (Story 11.2), followed live from Room; its Manches come with Story 11.3. */
@HiltViewModel
class FreePartyViewModel @Inject constructor(
    private val partyRepository: PartyRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<FreePartyUiState>(FreePartyUiState.Loading)
    val uiState: StateFlow<FreePartyUiState> = _uiState.asStateFlow()

    private var initializedPartyId: UUID? = null

    /** Idempotent: a recomposition never restarts the observation of the same Partie. */
    fun initialize(partyId: UUID) {
        if (initializedPartyId == partyId) return
        initializedPartyId = partyId
        viewModelScope.launch {
            combine(partyRepository.observeParty(partyId), partyRepository.observeParticipants(partyId)) { party, participants ->
                val gameName = party?.gameName
                val direction = party?.winDirection
                if (party == null || party.gameId != GameIds.FREE || gameName == null || direction == null) {
                    FreePartyUiState.LoadError
                } else {
                    val seats = participants.sortedBy { it.listPosition }
                    FreePartyUiState.Loaded(
                        gameName = gameName,
                        winDirection = direction,
                        endRule = party.endRule,
                        participants = seats,
                        dealerId = seats.getOrNull(party.currentDealerIndex)?.id
                    )
                }
            }
                .catch { emit(FreePartyUiState.LoadError) }
                .collect { _uiState.value = it }
        }
    }
}

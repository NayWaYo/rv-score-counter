package com.rvetiveau.rvscorecount.ui.screens.party

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rvetiveau.rvscorecount.domain.model.PartyState
import com.rvetiveau.rvscorecount.domain.repository.PartyRepository
import com.rvetiveau.rvscorecount.ui.navigation.NavigationEvent
import com.rvetiveau.rvscorecount.ui.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/**
 * Loads the just-created Party and its Participants from Room (AC 5, 10): Room is the single
 * source of truth here, never the Setup draft. The Donneur is derived from currentDealerIndex
 * against the ordered listPosition; a missing match is an explicit inconsistency, not a fallback
 * to the first Participant.
 */
@HiltViewModel
class PartyViewModel @Inject constructor(
    private val partyRepository: PartyRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<PartyUiState>(PartyUiState.Loading)
    val uiState: StateFlow<PartyUiState> = _uiState.asStateFlow()

    private val _navigationEvents = Channel<NavigationEvent>(Channel.BUFFERED)
    val navigationEvents: Flow<NavigationEvent> = _navigationEvents.receiveAsFlow()

    private var initializedPartyId: UUID? = null
    private var navigationPending = false


    /** Idempotent: a recomposition never re-triggers the Room read for the same partyId. */
    fun initialize(partyId: UUID) {
        if (initializedPartyId == partyId) return
        initializedPartyId = partyId
        _uiState.value = PartyUiState.Loading

        viewModelScope.launch {
            try {
                val party = partyRepository.getPartyById(partyId)
                if (party == null) {
                    _uiState.value = PartyUiState.PartyNotFound
                    return@launch
                }

                val participants = partyRepository.getParticipantsByPartyId(partyId)
                val positionsAreContinuous = participants
                    .map { it.listPosition }
                    .sorted() == participants.indices.toList()
                val dealers = participants.filter { it.listPosition == party.currentDealerIndex }
                val aggregateIsConsistent = positionsAreContinuous &&
                    participants.all { it.partyId == partyId && it.name.isNotBlank() } &&
                    dealers.size == 1
                _uiState.value = if (aggregateIsConsistent) {
                    PartyUiState.Loaded(
                        dealerName = dealers.single().name,
                        canStartRound = party.status == PartyState.IN_PROGRESS
                    )
                } else {
                    PartyUiState.InconsistentParticipants
                }
            } catch (cancellation: CancellationException) {
                initializedPartyId = null
                throw cancellation
            } catch (_: Throwable) {
                initializedPartyId = null
                _uiState.value = PartyUiState.LoadError
            }
        }
    }

    fun retry(partyId: UUID) {
        if (_uiState.value is PartyUiState.LoadError) initialize(partyId)
    }

    /** Emits the Saisie de Manche destination for the initialized Partie (AC 1); a no-op unless
     * Room has confirmed a consistent, Loaded Partie. */
    fun onStartRound() {
        val partyId = initializedPartyId ?: return
        val state = _uiState.value as? PartyUiState.Loaded ?: return
        if (!state.canStartRound || navigationPending) return
        navigationPending = true
        viewModelScope.launch {
            _navigationEvents.send(NavigationEvent.NavigateTo(Routes.RoundEntry(partyId.toString())))
        }
    }

    fun onScreenResumed() {
        navigationPending = false
    }
}

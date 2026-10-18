package com.rvetiveau.rvscorecount.ui.screens.party

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rvetiveau.rvscorecount.domain.model.Participant
import com.rvetiveau.rvscorecount.domain.model.Party
import com.rvetiveau.rvscorecount.domain.model.PartyState
import com.rvetiveau.rvscorecount.domain.model.computeStandings
import com.rvetiveau.rvscorecount.domain.repository.PartyRepository
import com.rvetiveau.rvscorecount.ui.navigation.NavigationEvent
import com.rvetiveau.rvscorecount.ui.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/**
 * Observes the Party and its Participants in Room (Story 4.1, AC 7): every write — a validated
 * Manche updates the `parties` row in the same transaction — re-derives the Classement, the
 * Donneur and the Manche number. The Donneur comes from currentDealerIndex against the ordered
 * listPosition; any mismatch is an explicit inconsistency, never a fallback.
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
    private var observation: Job? = null

    /** Idempotent: a recomposition never restarts the Room observation for the same partyId. */
    fun initialize(partyId: UUID) {
        if (initializedPartyId == partyId) return
        initializedPartyId = partyId
        _uiState.value = PartyUiState.Loading
        observe(partyId)
    }

    private fun observe(partyId: UUID) {
        observation?.cancel()
        observation = viewModelScope.launch {
            try {
                combine(
                    partyRepository.observeParty(partyId),
                    partyRepository.observeParticipants(partyId)
                ) { party, participants -> party to participants }
                    .collectLatest { (party, participants) ->
                        _uiState.value = loadState(partyId, party, participants)
                    }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Throwable) {
                initializedPartyId = null
                _uiState.value = PartyUiState.LoadError
            }
        }
    }

    private suspend fun loadState(partyId: UUID, party: Party?, participants: List<Participant>): PartyUiState {
        if (party == null) return PartyUiState.PartyNotFound

        val positionsAreContinuous = participants
            .map { it.listPosition }
            .sorted() == participants.indices.toList()
        val dealers = participants.filter { it.listPosition == party.currentDealerIndex }
        val aggregateIsConsistent = positionsAreContinuous &&
            participants.all { it.partyId == partyId && it.name.isNotBlank() } &&
            dealers.size == 1
        if (!aggregateIsConsistent) return PartyUiState.InconsistentParticipants
        val dealer = dealers.single()

        val rounds = partyRepository.getRoundsByPartyId(partyId)
        val scores = rounds.flatMap { partyRepository.getScoresByRoundId(it.id) }
        val standings = try {
            computeStandings(participants, scores)
        } catch (_: IllegalArgumentException) {
            return PartyUiState.InconsistentParticipants
        }

        // Rang « 1, 1, 3 » : la liste est triée, un ex æquo prend le rang du premier de son score.
        val rows = standings.map { standing ->
            StandingRow(
                participantId = standing.participant.id,
                rank = standings.indexOfFirst { it.cumulativeScore == standing.cumulativeScore } + 1,
                name = standing.participant.name,
                cumulativeScore = standing.cumulativeScore,
                isDealer = standing.participant.id == dealer.id
            )
        }
        return PartyUiState.Loaded(
            dealerName = dealer.name,
            roundNumber = (rounds.maxOfOrNull { it.roundNumber } ?: 0) + 1,
            standings = rows,
            canStartRound = party.status == PartyState.IN_PROGRESS
        )
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

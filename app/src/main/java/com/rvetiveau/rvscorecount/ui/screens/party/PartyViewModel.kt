package com.rvetiveau.rvscorecount.ui.screens.party

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rvetiveau.rvscorecount.R
import com.rvetiveau.rvscorecount.domain.model.Participant
import com.rvetiveau.rvscorecount.domain.model.Party
import com.rvetiveau.rvscorecount.domain.model.PartyState
import com.rvetiveau.rvscorecount.domain.model.computeStandings
import com.rvetiveau.rvscorecount.domain.model.sevenChampion
import com.rvetiveau.rvscorecount.domain.repository.PartyRepository
import com.rvetiveau.rvscorecount.domain.usecase.AddParticipantUseCase
import com.rvetiveau.rvscorecount.domain.usecase.ClosePartyUseCase
import com.rvetiveau.rvscorecount.domain.usecase.GetAvailableGamesUseCase
import com.rvetiveau.rvscorecount.domain.usecase.nextDealerIndexAfterInsertion
import com.rvetiveau.rvscorecount.domain.model.duplicatePlayerName
import com.rvetiveau.rvscorecount.ui.components.toStandingRows
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
    private val partyRepository: PartyRepository,
    private val getAvailableGamesUseCase: GetAvailableGamesUseCase,
    private val addParticipantUseCase: AddParticipantUseCase,
    private val closePartyUseCase: ClosePartyUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<PartyUiState>(PartyUiState.Loading)
    val uiState: StateFlow<PartyUiState> = _uiState.asStateFlow()

    private val _navigationEvents = Channel<NavigationEvent>(Channel.BUFFERED)
    val navigationEvents: Flow<NavigationEvent> = _navigationEvents.receiveAsFlow()

    private var initializedPartyId: UUID? = null
    private var navigationPending = false
    private var refreshOnResume = false
    private var observation: Job? = null

    // Room-derived state and the late-Participant draft are merged into [_uiState] by [publish].
    private var roomState: PartyUiState = PartyUiState.Loading
    private var addition: ParticipantAdditionUiState? = null
    private var closure = PartyClosureUiState()
    private var seating: List<Participant> = emptyList()
    private var currentDealerIndex: Int = 0

    /** Idempotent: a recomposition never restarts the Room observation for the same partyId. */
    fun initialize(partyId: UUID) {
        if (initializedPartyId == partyId) return
        initializedPartyId = partyId
        addition = null
        roomState = PartyUiState.Loading
        publish()
        observe(partyId)
    }

    private fun publish() {
        val state = roomState
        _uiState.value = if (state is PartyUiState.Loaded) {
            state.copy(addition = addition, closure = closure)
        } else {
            state
        }
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
                        roomState = loadState(partyId, party, participants)
                        publish()
                    }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Throwable) {
                initializedPartyId = null
                roomState = PartyUiState.LoadError
                publish()
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
        val engine = getAvailableGamesUseCase().firstOrNull { it.gameId == party.gameId }
            ?: return PartyUiState.InconsistentParticipants

        val rounds = partyRepository.getRoundsByPartyId(partyId)
        val scoresByRound = rounds.associateWith { partyRepository.getScoresByRoundId(it.id) }
        val standings = try {
            sevenChampion(participants, rounds)
            computeStandings(participants, scoresByRound.values.flatten())
        } catch (_: IllegalArgumentException) {
            return PartyUiState.InconsistentParticipants
        }

        val rows = standings.toStandingRows(dealerId = dealer.id)
        seating = participants.sortedBy { it.listPosition }
        currentDealerIndex = party.currentDealerIndex
        return PartyUiState.Loaded(
            roundNumber = (rounds.maxOfOrNull { it.roundNumber } ?: 0) + 1,
            standings = rows,
            canStartRound = party.status == PartyState.IN_PROGRESS,
            maxPlayers = engine.maxPlayers,
            dealingRules = engine.dealingRules(participants.size)
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
        // A correction made from Statistiques rewrites Rounds only, which the Partie/Participants
        // observation does not see.
        if (refreshOnResume) {
            refreshOnResume = false
            initializedPartyId?.let(::observe)
        }
    }

    /** Opens the Statistiques of the Partie once a Manche has been validated. */
    fun onShowStats() {
        val partyId = initializedPartyId ?: return
        val state = roomState as? PartyUiState.Loaded ?: return
        if (state.playedRoundCount == 0 || navigationPending) return
        navigationPending = true
        refreshOnResume = true
        viewModelScope.launch {
            _navigationEvents.send(NavigationEvent.NavigateTo(Routes.Stats(partyId.toString())))
        }
    }

    // --- Story 5.1 : clôture de Partie ---

    fun onEndPartyClick() {
        val state = roomState as? PartyUiState.Loaded ?: return
        if (!state.canStartRound || closure.isClosing) return
        closure = PartyClosureUiState(isConfirmationOpen = true)
        publish()
    }

    fun onEndPartyDismissed() {
        if (closure.isClosing) return
        closure = PartyClosureUiState()
        publish()
    }

    /** Clôt la Partie puis remplace toute la pile par les Résultats finaux (UX : pas de retour
     * vers une Partie terminée). Un échec ferme le dialog et laisse la Partie en cours. */
    fun onEndPartyConfirmed() {
        val partyId = initializedPartyId ?: return
        if (!closure.isConfirmationOpen || closure.isClosing) return
        closure = closure.copy(isClosing = true)
        publish()

        viewModelScope.launch {
            if (closePartyUseCase(partyId).isSuccess) {
                _navigationEvents.send(NavigationEvent.ReplaceAll(Routes.Results(partyId.toString())))
            } else {
                closure = PartyClosureUiState(errorRes = R.string.party_end_error)
                publish()
            }
        }
    }

    // --- Story 4.2 : ajout d'un Participant en cours de Partie ---

    fun onAddParticipantClick() {
        val state = roomState as? PartyUiState.Loaded ?: return
        if (!state.canAddParticipant || addition != null) return
        addition = ParticipantAdditionUiState()
        publish()
    }

    fun onAdditionNameChanged(name: String) {
        val draft = addition ?: return
        if (draft.isChoosingPosition) return
        addition = draft.copy(nameInput = name, errorRes = null)
        publish()
    }

    /** Opens the seat picker: one « Après {nom} » option per Participant in table order. */
    fun onAdditionNameConfirmed() {
        val draft = addition ?: return
        if (draft.nameInput.isBlank()) {
            addition = draft.copy(errorRes = R.string.party_add_error_empty_name)
        } else if (duplicatePlayerName(seating.map { it.name } + draft.nameInput) != null) {
            addition = draft.copy(errorRes = R.string.setup_error_duplicate_name)
        } else {
            val options = seating.map { participant ->
                val position = participant.listPosition + 1
                InsertionOption(
                    position = position,
                    afterName = participant.name,
                    becomesDealer = nextDealerIndexAfterInsertion(currentDealerIndex, position, seating.size) == position
                )
            }
            addition = draft.copy(insertionOptions = options, errorRes = null)
        }
        publish()
    }

    fun onInsertionPositionSelected(position: Int) {
        val partyId = initializedPartyId ?: return
        val draft = addition ?: return
        if (draft.isSaving || draft.insertionOptions.none { it.position == position }) return
        addition = draft.copy(isSaving = true, errorRes = null)
        publish()

        viewModelScope.launch {
            val result = addParticipantUseCase(partyId, draft.nameInput, position)
            addition = if (result.isSuccess) {
                null
            } else {
                addition?.copy(isSaving = false, errorRes = R.string.party_add_error_failed)
            }
            publish()
        }
    }

    fun onAdditionCancelled() {
        if (addition?.isSaving == true) return
        addition = null
        publish()
    }
}

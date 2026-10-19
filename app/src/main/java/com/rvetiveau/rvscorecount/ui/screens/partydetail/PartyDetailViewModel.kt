package com.rvetiveau.rvscorecount.ui.screens.partydetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rvetiveau.rvscorecount.R
import com.rvetiveau.rvscorecount.domain.model.PartyState
import com.rvetiveau.rvscorecount.domain.model.computeStandings
import com.rvetiveau.rvscorecount.domain.model.roundRecord
import com.rvetiveau.rvscorecount.domain.model.sevenChampion
import com.rvetiveau.rvscorecount.ui.components.toUi
import com.rvetiveau.rvscorecount.domain.repository.PartyRepository
import com.rvetiveau.rvscorecount.domain.usecase.CreatePartyUseCase
import com.rvetiveau.rvscorecount.ui.components.roundTable
import com.rvetiveau.rvscorecount.ui.components.toStandingRows
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
 * Loads a FINISHED Partie once (Story 6.2): it no longer changes. « Rejouer avec ces joueurs »
 * reuses the Story 5.2 relaunch and replaces the whole stack with the new Partie.
 */
@HiltViewModel
class PartyDetailViewModel @Inject constructor(
    private val partyRepository: PartyRepository,
    private val createPartyUseCase: CreatePartyUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<PartyDetailUiState>(PartyDetailUiState.Loading)
    val uiState: StateFlow<PartyDetailUiState> = _uiState.asStateFlow()

    private val _navigationEvents = Channel<NavigationEvent>(Channel.BUFFERED)
    val navigationEvents: Flow<NavigationEvent> = _navigationEvents.receiveAsFlow()

    private var initializedPartyId: UUID? = null

    /** Idempotent: a recomposition never re-triggers the Room read for the same partyId. */
    fun initialize(partyId: UUID) {
        if (initializedPartyId == partyId) return
        initializedPartyId = partyId
        _uiState.value = PartyDetailUiState.Loading

        viewModelScope.launch {
            _uiState.value = try {
                load(partyId)
            } catch (cancellation: CancellationException) {
                initializedPartyId = null
                throw cancellation
            } catch (_: Throwable) {
                initializedPartyId = null
                PartyDetailUiState.LoadError
            }
        }
    }

    private suspend fun load(partyId: UUID): PartyDetailUiState {
        val party = partyRepository.getPartyById(partyId)
        if (party == null || party.status != PartyState.FINISHED) return PartyDetailUiState.Unavailable

        val participants = partyRepository.getParticipantsByPartyId(partyId)
        val positionsAreContinuous = participants.map { it.listPosition }.sorted() == participants.indices.toList()
        if (participants.isEmpty() || !positionsAreContinuous || participants.any { it.partyId != partyId }) {
            return PartyDetailUiState.Unavailable
        }

        val scoresByRound = partyRepository.getRoundsByPartyId(partyId)
            .associateWith { partyRepository.getScoresByRoundId(it.id) }
        // computeStandings et sevenChampion vérifient scores et détenteurs du 7 avant roundTable.
        return try {
            val standings = computeStandings(participants, scoresByRound.values.flatten())
            val champion = sevenChampion(participants, scoresByRound.keys.toList())
            PartyDetailUiState.Loaded(
                gameId = party.gameId,
                createdAt = party.createdAt,
                standings = standings.toStandingRows(),
                roundTable = roundTable(participants, scoresByRound),
                sevenChampion = champion?.toUi(),
                roundRecord = roundRecord(participants, scoresByRound)?.toUi()
            )
        } catch (_: IllegalArgumentException) {
            PartyDetailUiState.Unavailable
        }
    }

    fun retry(partyId: UUID) {
        if (_uiState.value is PartyDetailUiState.LoadError) initialize(partyId)
    }

    fun onReplay() {
        val partyId = initializedPartyId ?: return
        val state = _uiState.value as? PartyDetailUiState.Loaded ?: return
        if (state.isReplaying) return
        _uiState.value = state.copy(isReplaying = true, replayErrorRes = null)

        viewModelScope.launch {
            createPartyUseCase.fromParty(partyId)
                .onSuccess { newPartyId ->
                    _navigationEvents.send(NavigationEvent.ReplaceAll(Routes.Party(newPartyId.toString())))
                }
                .onFailure {
                    (_uiState.value as? PartyDetailUiState.Loaded)?.let { loaded ->
                        _uiState.value = loaded.copy(isReplaying = false, replayErrorRes = R.string.results_replay_error)
                    }
                }
        }
    }
}

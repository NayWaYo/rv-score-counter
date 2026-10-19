package com.rvetiveau.rvscorecount.ui.screens.results

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rvetiveau.rvscorecount.R
import com.rvetiveau.rvscorecount.domain.model.PartyState
import com.rvetiveau.rvscorecount.domain.model.computeStandings
import com.rvetiveau.rvscorecount.domain.model.playerStats
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
 * Loads the final Classement of a FINISHED Partie once (FR-13): a finished Partie no longer
 * changes, so no observation is needed. Anything but a consistent FINISHED aggregate is
 * [ResultsUiState.Unavailable], never a partial ranking.
 */
@HiltViewModel
class ResultsViewModel @Inject constructor(
    private val partyRepository: PartyRepository,
    private val createPartyUseCase: CreatePartyUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<ResultsUiState>(ResultsUiState.Loading)
    val uiState: StateFlow<ResultsUiState> = _uiState.asStateFlow()

    private val _navigationEvents = Channel<NavigationEvent>(Channel.BUFFERED)
    val navigationEvents: Flow<NavigationEvent> = _navigationEvents.receiveAsFlow()

    private var initializedPartyId: UUID? = null

    /** Idempotent: a recomposition never re-triggers the Room read for the same partyId. */
    fun initialize(partyId: UUID) {
        if (initializedPartyId == partyId) return
        initializedPartyId = partyId
        _uiState.value = ResultsUiState.Loading

        viewModelScope.launch {
            _uiState.value = try {
                load(partyId)
            } catch (cancellation: CancellationException) {
                initializedPartyId = null
                throw cancellation
            } catch (_: Throwable) {
                initializedPartyId = null
                ResultsUiState.LoadError
            }
        }
    }

    private suspend fun load(partyId: UUID): ResultsUiState {
        val party = partyRepository.getPartyById(partyId)
        if (party == null || party.status != PartyState.FINISHED) return ResultsUiState.Unavailable

        val participants = partyRepository.getParticipantsByPartyId(partyId)
        val positionsAreContinuous = participants.map { it.listPosition }.sorted() == participants.indices.toList()
        if (participants.isEmpty() || !positionsAreContinuous || participants.any { it.partyId != partyId }) {
            return ResultsUiState.Unavailable
        }

        val rounds = partyRepository.getRoundsByPartyId(partyId)
        val scoresByRound = rounds.associateWith { partyRepository.getScoresByRoundId(it.id) }
        return try {
            ResultsUiState.Loaded(
                standings = computeStandings(participants, scoresByRound.values.flatten())
                    .toStandingRows(stats = playerStats(participants, scoresByRound)),
                sevenChampion = sevenChampion(participants, rounds)?.toUi(),
                roundRecord = roundRecord(participants, scoresByRound)?.toUi(),
                roundTable = roundTable(participants, scoresByRound)
            )
        } catch (_: IllegalArgumentException) {
            ResultsUiState.Unavailable
        }
    }

    fun retry(partyId: UUID) {
        if (_uiState.value is ResultsUiState.LoadError) initialize(partyId)
    }

    /** « Rejouer » (FR-14): one new Partie per tap, then the whole stack is replaced by it. */
    fun onReplay() {
        val partyId = initializedPartyId ?: return
        val state = _uiState.value as? ResultsUiState.Loaded ?: return
        if (state.isReplaying) return
        _uiState.value = state.copy(isReplaying = true, replayErrorRes = null)

        viewModelScope.launch {
            createPartyUseCase.fromParty(partyId)
                .onSuccess { newPartyId ->
                    _navigationEvents.send(NavigationEvent.ReplaceAll(Routes.Party(newPartyId.toString())))
                }
                .onFailure {
                    (_uiState.value as? ResultsUiState.Loaded)?.let { loaded ->
                        _uiState.value = loaded.copy(isReplaying = false, replayErrorRes = R.string.results_replay_error)
                    }
                }
        }
    }

    /** « Retour à l'accueil » and system back: the finished Partie is left for the Accueil. */
    fun onBackToHome() {
        viewModelScope.launch { _navigationEvents.send(NavigationEvent.ReplaceAll(Routes.Home)) }
    }
}

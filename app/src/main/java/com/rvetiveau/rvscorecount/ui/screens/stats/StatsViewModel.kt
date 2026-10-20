package com.rvetiveau.rvscorecount.ui.screens.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rvetiveau.rvscorecount.domain.model.PartyState
import com.rvetiveau.rvscorecount.domain.model.computeStandings
import com.rvetiveau.rvscorecount.domain.model.playerStats
import com.rvetiveau.rvscorecount.domain.model.roundRecord
import com.rvetiveau.rvscorecount.domain.model.sevenChampion
import com.rvetiveau.rvscorecount.domain.repository.PartyRepository
import com.rvetiveau.rvscorecount.ui.components.roundTable
import com.rvetiveau.rvscorecount.ui.components.toStandingRows
import com.rvetiveau.rvscorecount.ui.components.toUi
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
 * Reads the Partie once, and again when the screen comes back from a Manche correction, the only
 * change it can see while open.
 */
@HiltViewModel
class StatsViewModel @Inject constructor(
    private val partyRepository: PartyRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<StatsUiState>(StatsUiState.Loading)
    val uiState: StateFlow<StatsUiState> = _uiState.asStateFlow()

    private val _navigationEvents = Channel<NavigationEvent>(Channel.BUFFERED)
    val navigationEvents: Flow<NavigationEvent> = _navigationEvents.receiveAsFlow()

    private var initializedPartyId: UUID? = null
    private var navigationPending = false
    private var reloadOnResume = false

    /** Idempotent: a recomposition never re-triggers the Room read for the same partyId. */
    fun initialize(partyId: UUID) {
        if (initializedPartyId == partyId) return
        initializedPartyId = partyId
        _uiState.value = StatsUiState.Loading
        reload(partyId)
    }

    fun retry(partyId: UUID) {
        if (_uiState.value is StatsUiState.LoadError) initialize(partyId)
    }

    fun onScreenResumed() {
        navigationPending = false
        if (reloadOnResume) {
            reloadOnResume = false
            initializedPartyId?.let(::reload)
        }
    }

    /** Opens the correction of the validated Manche [roundNumber] while the Partie is in progress. */
    fun onRoundSelected(roundNumber: Int) {
        val partyId = initializedPartyId ?: return
        val state = _uiState.value as? StatsUiState.Loaded ?: return
        if (!state.canCorrectRounds || navigationPending) return
        if (state.roundTable.rows.none { it.roundNumber == roundNumber }) return
        navigationPending = true
        reloadOnResume = true
        viewModelScope.launch {
            _navigationEvents.send(
                NavigationEvent.NavigateTo(Routes.RoundCorrection(partyId.toString(), roundNumber))
            )
        }
    }

    private fun reload(partyId: UUID) {
        viewModelScope.launch {
            _uiState.value = try {
                load(partyId)
            } catch (cancellation: CancellationException) {
                initializedPartyId = null
                throw cancellation
            } catch (_: Throwable) {
                initializedPartyId = null
                StatsUiState.LoadError
            }
        }
    }

    private suspend fun load(partyId: UUID): StatsUiState {
        val party = partyRepository.getPartyById(partyId) ?: return StatsUiState.Unavailable

        val participants = partyRepository.getParticipantsByPartyId(partyId)
        val positionsAreContinuous = participants.map { it.listPosition }.sorted() == participants.indices.toList()
        if (participants.isEmpty() || !positionsAreContinuous || participants.any { it.partyId != partyId }) {
            return StatsUiState.Unavailable
        }

        val scoresByRound = partyRepository.getRoundsByPartyId(partyId)
            .associateWith { partyRepository.getScoresByRoundId(it.id) }
        // computeStandings et sevenChampion vérifient scores et détenteurs du 7 avant roundTable.
        return try {
            val standings = computeStandings(participants, scoresByRound.values.flatten())
            val champion = sevenChampion(participants, scoresByRound.keys.toList())
            StatsUiState.Loaded(
                standings = standings.toStandingRows(stats = playerStats(participants, scoresByRound)),
                roundTable = roundTable(participants, scoresByRound),
                sevenChampion = champion?.toUi(),
                roundRecord = roundRecord(participants, scoresByRound)?.toUi(),
                canCorrectRounds = party.status == PartyState.IN_PROGRESS
            )
        } catch (_: IllegalArgumentException) {
            StatsUiState.Unavailable
        }
    }
}

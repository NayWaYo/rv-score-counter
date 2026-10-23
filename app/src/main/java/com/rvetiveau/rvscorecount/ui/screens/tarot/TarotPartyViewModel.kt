package com.rvetiveau.rvscorecount.ui.screens.tarot

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rvetiveau.rvscorecount.R
import com.rvetiveau.rvscorecount.domain.model.EndRule
import com.rvetiveau.rvscorecount.domain.model.Participant
import com.rvetiveau.rvscorecount.domain.model.PartyState
import com.rvetiveau.rvscorecount.domain.model.TarotContract
import com.rvetiveau.rvscorecount.domain.model.TarotDealing
import com.rvetiveau.rvscorecount.domain.model.TarotPartyState
import com.rvetiveau.rvscorecount.domain.usecase.ClosePartyUseCase
import com.rvetiveau.rvscorecount.domain.usecase.CreateTarotPartyUseCase
import com.rvetiveau.rvscorecount.domain.usecase.ObserveTarotPartyUseCase
import com.rvetiveau.rvscorecount.ui.components.RoundTable
import com.rvetiveau.rvscorecount.ui.components.RoundTablePlayer
import com.rvetiveau.rvscorecount.ui.components.RoundTableRow
import com.rvetiveau.rvscorecount.ui.components.StandingRow
import com.rvetiveau.rvscorecount.ui.navigation.NavigationEvent
import com.rvetiveau.rvscorecount.ui.navigation.Routes
import com.rvetiveau.rvscorecount.ui.screens.party.PartyClosureUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import javax.inject.Inject
import kotlin.math.abs
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface TarotPartyUiState {
    data object Loading : TarotPartyUiState
    data object LoadError : TarotPartyUiState
    data object NotFound : TarotPartyUiState

    /**
     * A Partie of Tarot (Story 13.3): its [standings], the Donneur flagged while it is in progress, the
     * Manches left ([progress], null by hand or once finished), the dealing reminder, each Manche's
     * contract and the points table. Once [isFinished], [winnerNames] holds the best totals, empty
     * without a Manche. [isResults]: shown as the Résultats of a Partie just ended, with « Rejouer ».
     */
    data class Loaded(
        val endRule: EndRule.RoundCount?,
        val isFinished: Boolean,
        val standings: List<StandingRow>,
        val winnerNames: List<String>,
        val progress: TarotProgress?,
        val playerCount: Int,
        val dealing: TarotDealing,
        val contracts: List<TarotContractLine>,
        val roundTable: RoundTable,
        val nextRoundNumber: Int,
        val closure: PartyClosureUiState = PartyClosureUiState(),
        val isResults: Boolean = false,
        val isReplaying: Boolean = false,
        @StringRes val replayErrorRes: Int? = null
    ) : TarotPartyUiState
}

data class TarotProgress(val current: Int, val total: Int)

/**
 * One played Manche: the taker, their partner at 5 players ([isAlone] when they called their own
 * king), the contract, and whether it was made, by [margin] points.
 */
data class TarotContractLine(
    val roundNumber: Int,
    val takerName: String,
    val partnerName: String?,
    val isAlone: Boolean,
    val contract: TarotContract,
    val made: Boolean,
    val margin: Int
)

/**
 * The Partie of Tarot (Story 13.3), followed live from Room: in progress, its Manches are entered and
 * corrected and it ends by hand after confirmation, which opens its Résultats; finished, it is read
 * only, « Rejouer » being offered from the Résultats.
 */
@HiltViewModel
class TarotPartyViewModel @Inject constructor(
    private val observeTarotPartyUseCase: ObserveTarotPartyUseCase,
    private val closePartyUseCase: ClosePartyUseCase,
    private val createTarotPartyUseCase: CreateTarotPartyUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<TarotPartyUiState>(TarotPartyUiState.Loading)
    val uiState: StateFlow<TarotPartyUiState> = _uiState.asStateFlow()

    private val _navigationEvents = Channel<NavigationEvent>(Channel.BUFFERED)
    val navigationEvents: Flow<NavigationEvent> = _navigationEvents.receiveAsFlow()

    private var partyId: UUID? = null
    private var isResults = false
    private var observation: Job? = null

    /** Idempotent: a recomposition never restarts the observation of the same Partie. */
    fun initialize(partyId: UUID, isResults: Boolean = false) {
        if (this.partyId != null) return
        this.partyId = partyId
        this.isResults = isResults
        observe()
    }

    /** Same Joueurs in the same order and same end; the new Partie replaces the whole back stack. */
    fun onReplay() {
        val id = partyId ?: return
        val state = (_uiState.value as? TarotPartyUiState.Loaded)?.takeIf { it.isResults && it.isFinished } ?: return
        if (state.isReplaying) return
        _uiState.value = state.copy(isReplaying = true, replayErrorRes = null)
        viewModelScope.launch {
            createTarotPartyUseCase.fromParty(id).fold(
                onSuccess = { newPartyId ->
                    _navigationEvents.send(NavigationEvent.ReplaceAll(Routes.TarotParty(newPartyId.toString())))
                },
                onFailure = {
                    _uiState.update { current ->
                        (current as? TarotPartyUiState.Loaded)
                            ?.copy(isReplaying = false, replayErrorRes = R.string.results_replay_error)
                            ?: current
                    }
                }
            )
        }
    }

    fun retry() = observe()

    fun onPlayRound() {
        if (inProgress() == null) return
        navigate { Routes.TarotRound(it.toString()) }
    }

    fun onRoundSelected(roundNumber: Int) {
        if (inProgress() == null) return
        navigate { Routes.TarotRound(it.toString(), roundNumber) }
    }

    fun onBackHome() {
        viewModelScope.launch { _navigationEvents.send(NavigationEvent.ReplaceAll(Routes.Home)) }
    }

    fun onEndPartyClick() {
        val state = inProgress() ?: return
        if (state.closure.isClosing) return
        _uiState.value = state.copy(closure = PartyClosureUiState(isConfirmationOpen = true))
    }

    fun onEndPartyDismissed() {
        val state = inProgress() ?: return
        if (state.closure.isClosing) return
        _uiState.value = state.copy(closure = PartyClosureUiState())
    }

    /** Opens the Résultats in place of the Partie; a failure closes the dialog and keeps it in progress. */
    fun onEndPartyConfirmed() {
        val id = partyId ?: return
        val state = inProgress() ?: return
        if (!state.closure.isConfirmationOpen || state.closure.isClosing) return
        _uiState.value = state.copy(closure = state.closure.copy(isClosing = true))

        viewModelScope.launch {
            if (closePartyUseCase(id).isSuccess) {
                _navigationEvents.send(NavigationEvent.ReplaceAll(Routes.TarotResults(id.toString())))
            } else {
                _uiState.update { current ->
                    if (current is TarotPartyUiState.Loaded && !current.isFinished) {
                        current.copy(closure = PartyClosureUiState(errorRes = R.string.party_end_error))
                    } else {
                        current
                    }
                }
            }
        }
    }

    private fun inProgress(): TarotPartyUiState.Loaded? =
        (_uiState.value as? TarotPartyUiState.Loaded)?.takeUnless { it.isFinished }

    private fun navigate(route: (UUID) -> Routes) {
        val id = partyId ?: return
        viewModelScope.launch { _navigationEvents.send(NavigationEvent.NavigateTo(route(id))) }
    }

    private fun observe() {
        val id = partyId ?: return
        observation?.cancel()
        _uiState.value = TarotPartyUiState.Loading
        observation = viewModelScope.launch {
            try {
                observeTarotPartyUseCase(id).collect { state ->
                    _uiState.update { current ->
                        if (state == null) return@update TarotPartyUiState.NotFound
                        val next = state.toUiState().copy(isResults = isResults)
                        when {
                            current !is TarotPartyUiState.Loaded -> next
                            // A closure in flight keeps its dialog until Room reports the end.
                            !next.isFinished -> next.copy(closure = current.closure)
                            else -> next.copy(isReplaying = current.isReplaying, replayErrorRes = current.replayErrorRes)
                        }
                    }
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Throwable) {
                _uiState.value = TarotPartyUiState.LoadError
            }
        }
    }
}

internal fun TarotPartyState.toUiState(): TarotPartyUiState.Loaded {
    val isFinished = party.status == PartyState.FINISHED
    val endRule = party.endRule as? EndRule.RoundCount
    val names = participants.associate { it.id to it.name }
    return TarotPartyUiState.Loaded(
        endRule = endRule,
        isFinished = isFinished,
        standings = standings.map { standing ->
            StandingRow(
                participantId = standing.participant.id,
                rank = standing.rank,
                name = standing.participant.name,
                cumulativeScore = standing.total,
                isDealer = !isFinished && standing.participant.id == dealer.id
            )
        },
        winnerNames = if (isFinished && rounds.isNotEmpty()) leaders.map(Participant::name) else emptyList(),
        progress = endRule?.takeUnless { isFinished }?.let { TarotProgress(rounds.size + 1, it.rounds) },
        playerCount = participants.size,
        dealing = dealing,
        contracts = rounds.zip(results) { round, result ->
            TarotContractLine(
                roundNumber = round.roundNumber,
                takerName = names.getValue(round.input.takerId),
                partnerName = round.input.partnerId?.let(names::getValue),
                isAlone = callsPartner && round.input.partnerId == null,
                contract = round.input.contract,
                made = result.made,
                margin = abs(round.input.attackPoints - result.requiredPoints)
            )
        },
        roundTable = RoundTable(
            players = participants.map { RoundTablePlayer(it.id, it.name) },
            rows = rounds.zip(results) { round, result ->
                RoundTableRow(roundNumber = round.roundNumber, suit = null, points = result.points, highestIsBest = true)
            }
        ),
        nextRoundNumber = rounds.size + 1
    )
}

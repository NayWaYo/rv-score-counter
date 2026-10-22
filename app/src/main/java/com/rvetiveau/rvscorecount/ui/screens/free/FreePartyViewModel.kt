package com.rvetiveau.rvscorecount.ui.screens.free

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rvetiveau.rvscorecount.R
import com.rvetiveau.rvscorecount.domain.model.EndRule
import com.rvetiveau.rvscorecount.domain.model.FreePartyState
import com.rvetiveau.rvscorecount.domain.model.Participant
import com.rvetiveau.rvscorecount.domain.model.PartyState
import com.rvetiveau.rvscorecount.domain.model.WinDirection
import com.rvetiveau.rvscorecount.domain.usecase.ClosePartyUseCase
import com.rvetiveau.rvscorecount.domain.usecase.CreateFreePartyUseCase
import com.rvetiveau.rvscorecount.domain.usecase.ObserveFreePartyUseCase
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

sealed interface FreePartyUiState {
    data object Loading : FreePartyUiState
    data object LoadError : FreePartyUiState
    data object NotFound : FreePartyUiState

    /**
     * A Partie of Jeu libre (Story 11.3): its [standings] by its win direction, the Donneur flagged
     * while it is in progress, how far the end is ([progress], null by hand or once finished) and its
     * Manches. Once [isFinished], [winnerNames] holds the best totals, empty without a Manche.
     * [isResults]: shown as the Résultats of a Partie just ended, with « Rejouer » (Story 11.4),
     * rather than as its read-only detail from the Historique.
     */
    data class Loaded(
        val gameName: String,
        val winDirection: WinDirection,
        val endRule: EndRule?,
        val isFinished: Boolean,
        val standings: List<StandingRow>,
        val winnerNames: List<String>,
        val progress: FreeProgress?,
        val roundTable: RoundTable,
        val nextRoundNumber: Int,
        val closure: PartyClosureUiState = PartyClosureUiState(),
        val isResults: Boolean = false,
        val isReplaying: Boolean = false,
        @StringRes val replayErrorRes: Int? = null
    ) : FreePartyUiState
}

sealed interface FreeProgress {
    /** [closestNames]: the Joueur(s) at the largest total, [remaining] points from the [target]. */
    data class Target(val target: Int, val remaining: Int, val closestNames: List<String>) : FreeProgress

    data class Rounds(val current: Int, val total: Int) : FreeProgress
}

/**
 * The Partie of Jeu libre (Stories 11.2 to 11.4), followed live from Room: in progress, its Manches
 * are entered and corrected and it ends by hand after confirmation, which opens its Résultats;
 * finished, it is read only, « Rejouer » being offered from the Résultats.
 */
@HiltViewModel
class FreePartyViewModel @Inject constructor(
    private val observeFreePartyUseCase: ObserveFreePartyUseCase,
    private val closePartyUseCase: ClosePartyUseCase,
    private val createFreePartyUseCase: CreateFreePartyUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<FreePartyUiState>(FreePartyUiState.Loading)
    val uiState: StateFlow<FreePartyUiState> = _uiState.asStateFlow()

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

    /** Same game, rules and Joueurs; the new Partie replaces the whole back stack. */
    fun onReplay() {
        val id = partyId ?: return
        val state = (_uiState.value as? FreePartyUiState.Loaded)?.takeIf { it.isResults && it.isFinished } ?: return
        if (state.isReplaying) return
        _uiState.value = state.copy(isReplaying = true, replayErrorRes = null)
        viewModelScope.launch {
            createFreePartyUseCase.fromParty(id).fold(
                onSuccess = { newPartyId ->
                    _navigationEvents.send(NavigationEvent.ReplaceAll(Routes.FreeParty(newPartyId.toString())))
                },
                onFailure = {
                    _uiState.update { current ->
                        (current as? FreePartyUiState.Loaded)
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
        navigate { Routes.FreeRound(it.toString()) }
    }

    fun onRoundSelected(roundNumber: Int) {
        if (inProgress() == null) return
        navigate { Routes.FreeRound(it.toString(), roundNumber) }
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
                _navigationEvents.send(NavigationEvent.ReplaceAll(Routes.FreeResults(id.toString())))
            } else {
                _uiState.update { current ->
                    if (current is FreePartyUiState.Loaded && !current.isFinished) {
                        current.copy(closure = PartyClosureUiState(errorRes = R.string.party_end_error))
                    } else {
                        current
                    }
                }
            }
        }
    }

    private fun inProgress(): FreePartyUiState.Loaded? =
        (_uiState.value as? FreePartyUiState.Loaded)?.takeUnless { it.isFinished }

    private fun navigate(route: (UUID) -> Routes) {
        val id = partyId ?: return
        viewModelScope.launch { _navigationEvents.send(NavigationEvent.NavigateTo(route(id))) }
    }

    private fun observe() {
        val id = partyId ?: return
        observation?.cancel()
        _uiState.value = FreePartyUiState.Loading
        observation = viewModelScope.launch {
            try {
                observeFreePartyUseCase(id).collect { state ->
                    _uiState.update { current ->
                        if (state == null) return@update FreePartyUiState.NotFound
                        val next = state.toUiState().copy(isResults = isResults)
                        when {
                            current !is FreePartyUiState.Loaded -> next
                            // A closure in flight keeps its dialog until Room reports the end.
                            !next.isFinished -> next.copy(closure = current.closure)
                            else -> next.copy(isReplaying = current.isReplaying, replayErrorRes = current.replayErrorRes)
                        }
                    }
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Throwable) {
                _uiState.value = FreePartyUiState.LoadError
            }
        }
    }
}

internal fun FreePartyState.toUiState(): FreePartyUiState.Loaded {
    val isFinished = party.status == PartyState.FINISHED
    val progress = if (isFinished) null else when (val rule = party.endRule) {
        null -> null
        is EndRule.RoundCount -> FreeProgress.Rounds(rounds.size + 1, rule.rounds)
        is EndRule.TargetScore -> {
            // The Partie ends when any total reaches the target, whoever wins: the largest is closest.
            val largest = standings.maxOf { it.total }
            FreeProgress.Target(rule.points, rule.points - largest, standings.filter { it.total == largest }.map { it.participant.name })
        }
    }
    return FreePartyUiState.Loaded(
        gameName = gameName,
        winDirection = winDirection,
        endRule = party.endRule,
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
        progress = progress,
        roundTable = RoundTable(
            players = participants.map { RoundTablePlayer(it.id, it.name) },
            rows = rounds.map { round ->
                RoundTableRow(
                    roundNumber = round.roundNumber,
                    suit = null,
                    points = round.scores,
                    highestIsBest = winDirection == WinDirection.HIGHEST_WINS
                )
            }
        ),
        nextRoundNumber = rounds.size + 1
    )
}

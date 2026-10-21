package com.rvetiveau.rvscorecount.ui.screens.belote

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rvetiveau.rvscorecount.R
import com.rvetiveau.rvscorecount.domain.engine.BeloteRules
import com.rvetiveau.rvscorecount.domain.model.BelotePartyStatus
import com.rvetiveau.rvscorecount.domain.model.BeloteRoundInput
import com.rvetiveau.rvscorecount.domain.model.PartyState
import com.rvetiveau.rvscorecount.domain.model.TeamSide
import com.rvetiveau.rvscorecount.domain.usecase.CorrectBeloteRoundUseCase
import com.rvetiveau.rvscorecount.domain.usecase.ObserveBelotePartyUseCase
import com.rvetiveau.rvscorecount.domain.usecase.PlayBeloteRoundUseCase
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

/**
 * Saisie, or correction, of a Manche of Belote (Story 7.3, AC 4-6). Typing one Équipe's trick
 * points fills the other's; a capot sets 162 and 0. The engine previews the result before saving.
 */
@HiltViewModel
class BeloteRoundViewModel @Inject constructor(
    private val observeBelotePartyUseCase: ObserveBelotePartyUseCase,
    private val playBeloteRoundUseCase: PlayBeloteRoundUseCase,
    private val correctBeloteRoundUseCase: CorrectBeloteRoundUseCase,
    private val rules: BeloteRules
) : ViewModel() {

    private val _uiState = MutableStateFlow<BeloteRoundUiState>(BeloteRoundUiState.Loading)
    val uiState: StateFlow<BeloteRoundUiState> = _uiState.asStateFlow()

    private val _navigationEvents = Channel<NavigationEvent>(Channel.BUFFERED)
    val navigationEvents: Flow<NavigationEvent> = _navigationEvents.receiveAsFlow()

    private var partyId: UUID? = null

    /** Litige reserve the entered Manche can win: left by the Manches before it. */
    private var pendingReserve = 0

    fun initialize(partyId: UUID, correctedRoundNumber: Int?) {
        if (this.partyId != null) return
        this.partyId = partyId
        viewModelScope.launch {
            _uiState.value = try {
                val party = observeBelotePartyUseCase(partyId).first()
                    ?.takeIf { it.party.status == PartyState.IN_PROGRESS }
                val teamNames = party?.let { state -> TeamSide.entries.map { state.team(it).name } }
                val players = party?.players.orEmpty().map { BeloteSeatUiModel(it.id, it.name, checkNotNull(it.teamSide)) }
                when {
                    party == null || teamNames == null -> BeloteRoundUiState.Unavailable
                    correctedRoundNumber == null -> {
                        pendingReserve = party.pendingReserve
                        BeloteRoundUiState.Loaded(
                            roundNumber = party.rounds.size + 1,
                            isCorrection = false,
                            teamNames = teamNames,
                            players = players
                        )
                    }
                    else -> {
                        val round = party.rounds.firstOrNull { it.roundNumber == correctedRoundNumber }
                        if (round == null) {
                            BeloteRoundUiState.Unavailable
                        } else {
                            pendingReserve = party.results.getOrNull(correctedRoundNumber - 2)?.reserveAfter ?: 0
                            val input = round.input
                            val defensePoints = rules.otherTeamTrickPoints(input.takerTrickPoints)
                            BeloteRoundUiState.Loaded(
                                roundNumber = correctedRoundNumber,
                                isCorrection = true,
                                teamNames = teamNames,
                                players = players,
                                takerPlayerId = round.takerPlayerId,
                                taker = input.taker,
                                pointsInputs = TeamSide.entries.map { side ->
                                    (if (side == input.taker) input.takerTrickPoints else defensePoints).toString()
                                },
                                beloteTeam = input.beloteTeam,
                                capotTeam = input.capotTeam
                            ).withPreview()
                        }
                    }
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Throwable) {
                BeloteRoundUiState.Unavailable
            }
        }
    }

    /** The taking Équipe follows the player who took (Story 8.3). */
    fun onTakerPlayerSelected(playerId: UUID) = edit { state ->
        val player = state.players.firstOrNull { it.id == playerId } ?: return@edit state
        state.copy(takerPlayerId = player.id, taker = player.side)
    }

    /** Digits only; a value within 0..162 fills the other Équipe, a larger one is flagged. */
    fun onPointsChanged(side: TeamSide, text: String) {
        if (text.length > MAX_POINTS_DIGITS || !text.all(Char::isDigit)) return
        edit { state ->
            val points = text.toIntOrNull()
            val other = when {
                points == null -> ""
                points <= rules.totalTrickPoints -> rules.otherTeamTrickPoints(points).toString()
                else -> ""
            }
            state.copy(
                pointsInputs = TeamSide.entries.map { if (it == side) text else other },
                pointsError = points != null && points > rules.totalTrickPoints,
                capotTeam = null
            )
        }
    }

    fun onBeloteChanged(side: TeamSide?) = edit { it.copy(beloteTeam = side) }

    fun onCapotChanged(side: TeamSide?) = edit { state ->
        if (side == null) {
            state.copy(capotTeam = null)
        } else {
            state.copy(
                capotTeam = side,
                pointsInputs = TeamSide.entries.map { if (it == side) rules.totalTrickPoints.toString() else "0" },
                pointsError = false
            )
        }
    }

    fun onValidate() {
        val state = _uiState.value as? BeloteRoundUiState.Loaded ?: return
        val id = partyId ?: return
        val input = state.input() ?: return
        val takerPlayerId = state.takerPlayerId ?: return
        if (!state.canValidate) return
        _uiState.value = state.copy(isSaving = true, errorRes = null)

        viewModelScope.launch {
            val result = if (state.isCorrection) {
                correctBeloteRoundUseCase(id, state.roundNumber, input, takerPlayerId)
            } else {
                playBeloteRoundUseCase(id, input, takerPlayerId)
            }
            result.fold(
                onSuccess = { status ->
                    val event = if (status is BelotePartyStatus.Won) {
                        NavigationEvent.ReplaceAll(Routes.BeloteResults(id.toString()))
                    } else {
                        NavigationEvent.NavigateBack
                    }
                    _navigationEvents.send(event)
                },
                onFailure = {
                    _uiState.value = state.copy(isSaving = false, errorRes = R.string.belote_round_error_failed)
                }
            )
        }
    }

    private fun edit(change: (BeloteRoundUiState.Loaded) -> BeloteRoundUiState.Loaded) {
        val state = _uiState.value as? BeloteRoundUiState.Loaded ?: return
        if (state.isSaving) return
        _uiState.value = change(state).copy(errorRes = null).withPreview()
    }

    private fun BeloteRoundUiState.Loaded.input(): BeloteRoundInput? {
        val taker = taker ?: return null
        val points = pointsInputs[taker.ordinal].toIntOrNull()?.takeIf { it in 0..rules.totalTrickPoints } ?: return null
        return BeloteRoundInput(taker, points, beloteTeam, capotTeam)
    }

    private fun BeloteRoundUiState.Loaded.withPreview(): BeloteRoundUiState.Loaded =
        copy(preview = input()?.let { runCatching { rules.scoreRound(it, pendingReserve) }.getOrNull() })

    private companion object {
        const val MAX_POINTS_DIGITS = 3
    }
}

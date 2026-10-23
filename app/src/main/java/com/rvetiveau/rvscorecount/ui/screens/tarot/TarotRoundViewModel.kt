package com.rvetiveau.rvscorecount.ui.screens.tarot

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rvetiveau.rvscorecount.R
import com.rvetiveau.rvscorecount.domain.engine.TarotRules
import com.rvetiveau.rvscorecount.domain.model.Chelem
import com.rvetiveau.rvscorecount.domain.model.Participant
import com.rvetiveau.rvscorecount.domain.model.PartyState
import com.rvetiveau.rvscorecount.domain.model.Poignee
import com.rvetiveau.rvscorecount.domain.model.TarotContract
import com.rvetiveau.rvscorecount.domain.model.TarotPartyStatus
import com.rvetiveau.rvscorecount.domain.model.TarotRoundInput
import com.rvetiveau.rvscorecount.domain.model.TarotSide
import com.rvetiveau.rvscorecount.domain.usecase.CorrectTarotRoundUseCase
import com.rvetiveau.rvscorecount.domain.usecase.ObserveTarotPartyUseCase
import com.rvetiveau.rvscorecount.domain.usecase.PlayTarotRoundUseCase
import com.rvetiveau.rvscorecount.ui.navigation.NavigationEvent
import com.rvetiveau.rvscorecount.ui.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import javax.inject.Inject
import kotlin.math.abs
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/** At 5 players, the holder of the called king: another Joueur, or nobody when the taker called their own. */
sealed interface TarotPartnerChoice {
    data object Alone : TarotPartnerChoice
    data class Player(val participantId: UUID) : TarotPartnerChoice
}

data class TarotSeat(val participantId: UUID, val name: String)

/** What the Manche entered so far would give: the contract's outcome and every Joueur's points, in table order. */
data class TarotRoundPreview(
    val requiredPoints: Int,
    val made: Boolean,
    val margin: Int,
    val perDefender: Int,
    val points: List<Pair<String, Int>>
)

sealed interface TarotRoundUiState {
    data object Loading : TarotRoundUiState

    /** The Partie is not in progress any more, or the corrected Manche does not exist. */
    data object Unavailable : TarotRoundUiState

    /**
     * Saisie of a Manche of Tarot (Story 13.3): the taker, contract, partner ([callsPartner] tables
     * only), bouts and points of the attack, then the bonuses; [preview] once they make a possible Manche.
     */
    data class Loaded(
        val roundNumber: Int,
        val isCorrection: Boolean,
        val players: List<TarotSeat>,
        val callsPartner: Boolean,
        val totalPoints: Int,
        val takerId: UUID? = null,
        val contract: TarotContract? = null,
        val partner: TarotPartnerChoice? = null,
        val bouts: Int? = null,
        val pointsText: String = "",
        val petitAuBout: TarotSide? = null,
        val attackPoignee: Poignee? = null,
        val defensePoignee: Poignee? = null,
        val chelem: Chelem? = null,
        /** What the attack needs with the [bouts] chosen, from the engine. */
        val requiredPoints: Int? = null,
        val preview: TarotRoundPreview? = null,
        val isSaving: Boolean = false,
        @StringRes val errorRes: Int? = null
    ) : TarotRoundUiState {
        val points: Int? get() = pointsText.toIntOrNull()

        val isPointsOutOfRange: Boolean get() = points?.let { it !in 0..totalPoints } ?: false

        val canValidate: Boolean get() = preview != null && !isSaving
    }
}

/**
 * Saisie, or correction, of a Manche of Tarot (Story 13.3): the engine checks and scores what is
 * entered at each change. Back to the Partie once saved, or to its Résultats when it ends.
 */
@HiltViewModel
class TarotRoundViewModel @Inject constructor(
    private val observeTarotPartyUseCase: ObserveTarotPartyUseCase,
    private val playTarotRoundUseCase: PlayTarotRoundUseCase,
    private val correctTarotRoundUseCase: CorrectTarotRoundUseCase,
    private val rules: TarotRules
) : ViewModel() {

    private val _uiState = MutableStateFlow<TarotRoundUiState>(TarotRoundUiState.Loading)
    val uiState: StateFlow<TarotRoundUiState> = _uiState.asStateFlow()

    private val _navigationEvents = Channel<NavigationEvent>(Channel.BUFFERED)
    val navigationEvents: Flow<NavigationEvent> = _navigationEvents.receiveAsFlow()

    private var partyId: UUID? = null
    private var participants: List<Participant> = emptyList()

    fun initialize(partyId: UUID, correctedRoundNumber: Int?) {
        if (this.partyId != null) return
        this.partyId = partyId
        viewModelScope.launch {
            _uiState.value = try {
                val party = observeTarotPartyUseCase(partyId).first()?.takeIf { it.party.status == PartyState.IN_PROGRESS }
                val corrected = correctedRoundNumber?.let { number -> party?.rounds?.firstOrNull { it.roundNumber == number } }
                if (party == null || (correctedRoundNumber != null && corrected == null)) {
                    TarotRoundUiState.Unavailable
                } else {
                    participants = party.participants
                    val blank = TarotRoundUiState.Loaded(
                        roundNumber = corrected?.roundNumber ?: (party.rounds.size + 1),
                        isCorrection = corrected != null,
                        players = party.participants.map { TarotSeat(it.id, it.name) },
                        callsPartner = party.callsPartner,
                        totalPoints = rules.totalPoints
                    )
                    corrected?.input?.let { input -> withPreview(blank.filledWith(input)) } ?: blank
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Throwable) {
                TarotRoundUiState.Unavailable
            }
        }
    }

    /** A taker who was the partner leaves the partner to choose again. */
    fun onTakerSelected(participantId: UUID) = edit { state ->
        val partner = state.partner.takeUnless { it == TarotPartnerChoice.Player(participantId) }
        state.copy(takerId = participantId, partner = partner)
    }

    fun onContractSelected(contract: TarotContract) = edit { it.copy(contract = contract) }

    fun onPartnerSelected(partner: TarotPartnerChoice) = edit { it.copy(partner = partner) }

    fun onBoutsSelected(bouts: Int) = edit { it.copy(bouts = bouts) }

    /** Digits only, at most two: a number beyond the deck's points shows as such. */
    fun onPointsChanged(text: String) {
        if (text.length > MAX_POINTS_DIGITS || !text.all(Char::isDigit)) return
        edit { it.copy(pointsText = text) }
    }

    fun onPetitAuBoutSelected(side: TarotSide?) = edit { it.copy(petitAuBout = side) }

    fun onAttackPoigneeSelected(poignee: Poignee?) = edit { it.copy(attackPoignee = poignee) }

    fun onDefensePoigneeSelected(poignee: Poignee?) = edit { it.copy(defensePoignee = poignee) }

    fun onChelemSelected(chelem: Chelem?) = edit { it.copy(chelem = chelem) }

    fun onValidate() {
        val state = _uiState.value as? TarotRoundUiState.Loaded ?: return
        val id = partyId ?: return
        if (!state.canValidate) return
        val input = state.toInput() ?: return
        _uiState.value = state.copy(isSaving = true, errorRes = null)

        viewModelScope.launch {
            val result = if (state.isCorrection) {
                correctTarotRoundUseCase(id, state.roundNumber, input)
            } else {
                playTarotRoundUseCase(id, input)
            }
            result.fold(
                onSuccess = { status ->
                    val event = if (status is TarotPartyStatus.Finished) {
                        NavigationEvent.ReplaceAll(Routes.TarotResults(id.toString()))
                    } else {
                        NavigationEvent.NavigateBack
                    }
                    _navigationEvents.send(event)
                },
                onFailure = { _uiState.value = state.copy(isSaving = false, errorRes = R.string.belote_round_error_failed) }
            )
        }
    }

    private fun edit(change: (TarotRoundUiState.Loaded) -> TarotRoundUiState.Loaded) {
        val state = _uiState.value as? TarotRoundUiState.Loaded ?: return
        if (state.isSaving) return
        _uiState.value = withPreview(change(state).copy(errorRes = null))
    }

    private fun withPreview(entered: TarotRoundUiState.Loaded): TarotRoundUiState.Loaded {
        val state = entered.copy(requiredPoints = entered.bouts?.let(rules::requiredPoints))
        val input = state.toInput()?.takeIf { rules.inputError(it, participants) == null }
            ?: return state.copy(preview = null)
        val result = rules.scoreRound(input, participants)
        return state.copy(
            preview = TarotRoundPreview(
                requiredPoints = result.requiredPoints,
                made = result.made,
                margin = abs(input.attackPoints - result.requiredPoints),
                perDefender = result.perDefender,
                points = participants.map { it.name to result.points.getValue(it.id) }
            )
        )
    }

    /** Null until the taker, contract, partner (when called), bouts and points are all entered. */
    private fun TarotRoundUiState.Loaded.toInput(): TarotRoundInput? {
        if (callsPartner && partner == null) return null
        val partnerId = (partner as? TarotPartnerChoice.Player)?.participantId
        return TarotRoundInput(
            takerId = takerId ?: return null,
            contract = contract ?: return null,
            attackBouts = bouts ?: return null,
            attackPoints = points ?: return null,
            partnerId = partnerId,
            petitAuBout = petitAuBout,
            attackPoignee = attackPoignee,
            defensePoignee = defensePoignee,
            chelem = chelem
        )
    }

    private fun TarotRoundUiState.Loaded.filledWith(input: TarotRoundInput) = copy(
        takerId = input.takerId,
        contract = input.contract,
        partner = when {
            input.partnerId != null -> TarotPartnerChoice.Player(input.partnerId)
            callsPartner -> TarotPartnerChoice.Alone
            else -> null
        },
        bouts = input.attackBouts,
        pointsText = input.attackPoints.toString(),
        petitAuBout = input.petitAuBout,
        attackPoignee = input.attackPoignee,
        defensePoignee = input.defensePoignee,
        chelem = input.chelem
    )

    private companion object {
        const val MAX_POINTS_DIGITS = 2
    }
}

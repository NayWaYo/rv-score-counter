package com.rvetiveau.rvscorecount.ui.screens.free

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rvetiveau.rvscorecount.R
import com.rvetiveau.rvscorecount.domain.engine.FreeGameRules
import com.rvetiveau.rvscorecount.domain.model.PartyState
import com.rvetiveau.rvscorecount.domain.usecase.CorrectFreeRoundUseCase
import com.rvetiveau.rvscorecount.domain.usecase.ObserveFreePartyUseCase
import com.rvetiveau.rvscorecount.domain.usecase.PlayFreeRoundUseCase
import com.rvetiveau.rvscorecount.ui.navigation.NavigationEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

sealed interface FreeRoundUiState {
    data object Loading : FreeRoundUiState

    /** The Partie is not in progress any more, or the corrected Manche does not exist. */
    data object Unavailable : FreeRoundUiState

    /** Saisie of a Manche of Jeu libre: one field per Joueur in table order. */
    data class Loaded(
        val roundNumber: Int,
        val isCorrection: Boolean,
        val fields: List<FreeScoreInput>,
        val isSaving: Boolean = false,
        @StringRes val errorRes: Int? = null
    ) : FreeRoundUiState {
        /** Every Joueur has a whole score within the engine's bounds. */
        val canValidate: Boolean get() = !isSaving && fields.all { it.text.toIntOrNull() != null && !it.isOutOfBounds }
    }
}

/** [text] as typed, a leading minus included; [isOutOfBounds] once it is a number beyond a Manche's bounds. */
data class FreeScoreInput(
    val participantId: UUID,
    val name: String,
    val text: String = "",
    val isOutOfBounds: Boolean = false
)

/**
 * Saisie, or correction, of a Manche of Jeu libre (Story 11.3): any whole score, negative
 * included, for every Joueur. Back to the Partie once saved, which shows its end when reached.
 */
@HiltViewModel
class FreeRoundViewModel @Inject constructor(
    private val observeFreePartyUseCase: ObserveFreePartyUseCase,
    private val playFreeRoundUseCase: PlayFreeRoundUseCase,
    private val correctFreeRoundUseCase: CorrectFreeRoundUseCase,
    private val rules: FreeGameRules
) : ViewModel() {

    private val _uiState = MutableStateFlow<FreeRoundUiState>(FreeRoundUiState.Loading)
    val uiState: StateFlow<FreeRoundUiState> = _uiState.asStateFlow()

    private val _navigationEvents = Channel<NavigationEvent>(Channel.BUFFERED)
    val navigationEvents: Flow<NavigationEvent> = _navigationEvents.receiveAsFlow()

    private var partyId: UUID? = null

    fun initialize(partyId: UUID, correctedRoundNumber: Int?) {
        if (this.partyId != null) return
        this.partyId = partyId
        viewModelScope.launch {
            _uiState.value = try {
                val party = observeFreePartyUseCase(partyId).first()?.takeIf { it.party.status == PartyState.IN_PROGRESS }
                val corrected = correctedRoundNumber?.let { number -> party?.rounds?.firstOrNull { it.roundNumber == number } }
                when {
                    party == null -> FreeRoundUiState.Unavailable
                    correctedRoundNumber == null -> FreeRoundUiState.Loaded(
                        roundNumber = party.rounds.size + 1,
                        isCorrection = false,
                        fields = party.participants.map { FreeScoreInput(it.id, it.name) }
                    )
                    corrected == null -> FreeRoundUiState.Unavailable
                    else -> FreeRoundUiState.Loaded(
                        roundNumber = corrected.roundNumber,
                        isCorrection = true,
                        fields = party.participants.map { FreeScoreInput(it.id, it.name, corrected.scores[it.id]?.toString().orEmpty()) }
                    )
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Throwable) {
                FreeRoundUiState.Unavailable
            }
        }
    }

    /** Digits after an optional minus; the field shows when the number is beyond a Manche's bounds. */
    fun onScoreChanged(participantId: UUID, text: String) {
        val digits = text.removePrefix("-")
        if (digits.length > MAX_SCORE_DIGITS || !digits.all(Char::isDigit)) return
        edit(participantId) { text }
    }

    fun onToggleSign(participantId: UUID) = edit(participantId) { text ->
        if (text.startsWith("-")) text.removePrefix("-") else "-$text"
    }

    fun onValidate() {
        val state = _uiState.value as? FreeRoundUiState.Loaded ?: return
        val id = partyId ?: return
        if (!state.canValidate) return
        val scores = state.fields.associate { it.participantId to checkNotNull(it.text.toIntOrNull()) }
        _uiState.value = state.copy(isSaving = true, errorRes = null)

        viewModelScope.launch {
            val result = if (state.isCorrection) {
                correctFreeRoundUseCase(id, state.roundNumber, scores)
            } else {
                playFreeRoundUseCase(id, scores)
            }
            result.fold(
                onSuccess = { _navigationEvents.send(NavigationEvent.NavigateBack) },
                onFailure = { _uiState.value = state.copy(isSaving = false, errorRes = R.string.belote_round_error_failed) }
            )
        }
    }

    private fun edit(participantId: UUID, change: (String) -> String) {
        val state = _uiState.value as? FreeRoundUiState.Loaded ?: return
        if (state.isSaving) return
        _uiState.value = state.copy(
            errorRes = null,
            fields = state.fields.map { field ->
                if (field.participantId != participantId) {
                    field
                } else {
                    val text = change(field.text)
                    field.copy(text = text, isOutOfBounds = text.toIntOrNull()?.let { it !in rules.roundScores } ?: false)
                }
            }
        )
    }

    private companion object {
        /** One more than the engine's bounds need, so that a number beyond them shows as such. */
        const val MAX_SCORE_DIGITS = 6
    }
}

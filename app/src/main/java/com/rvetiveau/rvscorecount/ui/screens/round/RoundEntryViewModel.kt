package com.rvetiveau.rvscorecount.ui.screens.round

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rvetiveau.rvscorecount.domain.model.CardSuit
import com.rvetiveau.rvscorecount.domain.model.PartyState
import com.rvetiveau.rvscorecount.domain.repository.PartyRepository
import com.rvetiveau.rvscorecount.domain.usecase.RollDieUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private const val SUIT_KEY = "suit"
private const val SCORE_KEY_PREFIX = "score_"

/**
 * Holds the die roll and score entry draft in memory only (AC 9, 12): no Round is persisted
 * here, Story 3.4 builds it at validation time. Participants are Room-backed and validated
 * strictly (AC 1, 10); an unknown/corrupt saved value falls back to "not rolled"/"not entered"
 * rather than a default.
 */
@HiltViewModel
class RoundEntryViewModel @Inject constructor(
    private val partyRepository: PartyRepository,
    private val rollDieUseCase: RollDieUseCase,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val restoredSuit: CardSuit? = savedStateHandle.get<String>(SUIT_KEY)?.let { name ->
        runCatching { CardSuit.valueOf(name) }.getOrNull()
    }

    private val _uiState = MutableStateFlow(RoundEntryUiState(suit = restoredSuit))
    val uiState: StateFlow<RoundEntryUiState> = _uiState.asStateFlow()

    private var initializedPartyId: UUID? = null

    /** Idempotent: a recomposition never re-triggers the Room read for the same partyId. */
    fun initialize(partyId: UUID) {
        if (initializedPartyId == partyId) return
        initializedPartyId = partyId
        _uiState.value = _uiState.value.copy(isLoading = true, loadError = false, content = null)

        viewModelScope.launch {
            try {
                val party = partyRepository.getPartyById(partyId)
                if (party == null || party.id != partyId || party.status != PartyState.IN_PROGRESS) {
                    initializedPartyId = null
                    _uiState.value = _uiState.value.copy(isLoading = false, loadError = true)
                    return@launch
                }

                val participants = partyRepository.getParticipantsByPartyId(partyId)
                val positionsAreContinuous =
                    participants.map { it.listPosition }.sorted() == participants.indices.toList()
                val aggregateIsConsistent = participants.isNotEmpty() &&
                    positionsAreContinuous &&
                    participants.all { it.partyId == partyId } &&
                    participants.map { it.id }.distinct().size == participants.size

                if (!aggregateIsConsistent) {
                    initializedPartyId = null
                    _uiState.value = _uiState.value.copy(isLoading = false, loadError = true)
                    return@launch
                }

                val ordered = participants.sortedBy { it.listPosition }
                val restoredScores = ordered.associate { participant ->
                    val restored = savedStateHandle.get<String>("$SCORE_KEY_PREFIX${participant.id}")
                    val validated = restored
                        ?.takeIf { it.isEmpty() || (it.all(Char::isDigit) && it.toIntOrNull() != null) }
                        ?: ""
                    participant.id to validated
                }

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    loadError = false,
                    content = RoundEntryContent(
                        participants = ordered.map { RoundEntryParticipant(it.id, it.name) },
                        scores = restoredScores
                    )
                )
            } catch (cancellation: CancellationException) {
                initializedPartyId = null
                throw cancellation
            } catch (_: Exception) {
                initializedPartyId = null
                _uiState.value = _uiState.value.copy(isLoading = false, loadError = true, content = null)
            }
        }
    }

    fun retry(partyId: UUID) {
        if (_uiState.value.loadError) initialize(partyId)
    }

    /** Same intent for « Lancer » and « Relancer » (Story 3.1): always draws exactly once per
     * tap, preserving any already-loaded participants and drafts (AC 8). */
    fun onRoll() {
        if (_uiState.value.content == null || !_uiState.value.rerollEnabled) return
        val suit = rollDieUseCase()
        savedStateHandle[SUIT_KEY] = suit.name
        _uiState.value = _uiState.value.copy(suit = suit)
    }

    /** Updates only the targeted Participant's draft (AC 3, 4); unknown ids and invalid raw
     * values (signs, letters, decimals, Int overflow) are ignored rather than transformed. */
    fun onScoreChanged(participantId: UUID, rawValue: String) {
        val content = _uiState.value.content ?: return
        if (content.participants.none { it.id == participantId }) return
        val isValid = rawValue.isEmpty() || (rawValue.all(Char::isDigit) && rawValue.toIntOrNull() != null)
        if (!isValid) return

        _uiState.value = _uiState.value.copy(content = content.copy(scores = content.scores + (participantId to rawValue)))

        val key = "$SCORE_KEY_PREFIX$participantId"
        if (rawValue.isEmpty()) savedStateHandle.remove<String>(key) else savedStateHandle[key] = rawValue
    }
}

package com.rvetiveau.rvscorecount.ui.screens.setup

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import com.rvetiveau.rvscorecount.R
import com.rvetiveau.rvscorecount.domain.usecase.GetAvailableGamesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Owns the in-memory Participant draft for Création de Partie (AC 1-6, 9). No persistence:
 * Story 2.3 extends this same ViewModel to create the Party from [SetupUiState.participants].
 */
@HiltViewModel
class SetupViewModel @Inject constructor(
    private val getAvailableGamesUseCase: GetAvailableGamesUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SetupUiState())
    val uiState: StateFlow<SetupUiState> = _uiState.asStateFlow()

    /** Idempotent: a recomposition or a duplicate call for the same gameId never resets the draft. */
    fun initialize(gameId: String) {
        if (_uiState.value.isInitialized) return
        val engine = getAvailableGamesUseCase().firstOrNull { it.gameId == gameId }
            ?: error("No GameEngine registered for gameId '$gameId'")
        _uiState.update {
            it.copy(
                gameId = engine.gameId,
                minPlayers = engine.minPlayers,
                maxPlayers = engine.maxPlayers,
                isInitialized = true
            )
        }
    }

    fun onNameChanged(name: String) {
        _uiState.update { it.copy(nameInput = name, messageRes = null) }
    }

    fun onAddParticipant() {
        val state = _uiState.value
        val trimmedName = state.nameInput.trim()
        when {
            trimmedName.isEmpty() -> setMessage(R.string.setup_error_empty_name)
            !state.canAdd -> setMessage(R.string.setup_max_players_reached)
            else -> _uiState.update {
                it.copy(
                    participants = it.participants + ParticipantDraft(name = trimmedName),
                    nameInput = "",
                    messageRes = null
                )
            }
        }
    }

    fun onRemoveParticipant(id: UUID) {
        _uiState.update { state ->
            val participants = state.participants.filterNot { draft -> draft.id == id }
            if (participants.size == state.participants.size) {
                state
            } else {
                state.copy(participants = participants, messageRes = null)
            }
        }
    }

    /** Moves the Participant at [fromIndex] to [toIndex]; no-op or out-of-range moves are ignored. */
    fun onMoveParticipant(fromIndex: Int, toIndex: Int) {
        _uiState.update { state ->
            val participants = state.participants
            if (fromIndex == toIndex || fromIndex !in participants.indices || toIndex !in participants.indices) {
                return@update state
            }
            val reordered = participants.toMutableList().apply { add(toIndex, removeAt(fromIndex)) }
            state.copy(participants = reordered)
        }
    }

    private fun setMessage(@StringRes messageRes: Int) {
        _uiState.update { it.copy(messageRes = messageRes) }
    }
}

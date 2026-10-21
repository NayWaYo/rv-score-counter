package com.rvetiveau.rvscorecount.ui.screens.setup

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rvetiveau.rvscorecount.R
import com.rvetiveau.rvscorecount.domain.model.ParticipantCreationData
import com.rvetiveau.rvscorecount.domain.usecase.CreatePartyUseCase
import com.rvetiveau.rvscorecount.domain.usecase.GetAvailableGamesUseCase
import com.rvetiveau.rvscorecount.domain.usecase.InvalidPartyConfigurationException
import com.rvetiveau.rvscorecount.ui.navigation.NavigationEvent
import com.rvetiveau.rvscorecount.ui.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Owns the in-memory Participant draft for Création de Partie (AC 1-6, 9) and confirms it into
 * a persisted Party (AC 1-4, 6-7): [SetupUiState.participants] is the sole listPosition source.
 */
@HiltViewModel
class SetupViewModel @Inject constructor(
    private val getAvailableGamesUseCase: GetAvailableGamesUseCase,
    private val createPartyUseCase: CreatePartyUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SetupUiState())
    val uiState: StateFlow<SetupUiState> = _uiState.asStateFlow()

    private val _navigationEvents = Channel<NavigationEvent>(Channel.BUFFERED)
    val navigationEvents: Flow<NavigationEvent> = _navigationEvents.receiveAsFlow()

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

    /** Historique of this Jeu only (Story 8.2); the draft stays below it on the back stack. */
    fun onHistorySelected() {
        val gameId = _uiState.value.gameId.takeIf { it.isNotEmpty() } ?: return
        viewModelScope.launch { _navigationEvents.send(NavigationEvent.NavigateTo(Routes.GameHistory(gameId))) }
    }

    fun onNameChanged(name: String) {
        _uiState.update { it.copy(nameInput = name, messageRes = null, creationErrorRes = null) }
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
                    messageRes = null,
                    creationErrorRes = null
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
                state.copy(participants = participants, messageRes = null, creationErrorRes = null)
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
            state.copy(participants = reordered, creationErrorRes = null)
        }
    }

    /** Guards a double tap: [SetupUiState.canStart] already excludes an in-flight or completed creation. */
    fun onStartParty() {
        val state = _uiState.value
        if (!state.canStart) return
        _uiState.update { it.copy(isCreatingParty = true, creationErrorRes = null) }

        viewModelScope.launch {
            try {
                val participants = state.participants.map { ParticipantCreationData(id = it.id, name = it.name) }
                createPartyUseCase(state.gameId, participants).fold(
                    onSuccess = { partyId ->
                        _uiState.update { it.copy(creationCompleted = true) }
                        _navigationEvents.send(NavigationEvent.NavigateTo(Routes.Party(partyId.toString())))
                    },
                    onFailure = { failure -> setCreationError(failure) }
                )
            } finally {
                _uiState.update { it.copy(isCreatingParty = false) }
            }
        }
    }

    private fun setCreationError(failure: Throwable) {
        val messageRes = if (failure is InvalidPartyConfigurationException) {
            R.string.setup_error_invalid_state
        } else {
            R.string.setup_error_creation_failed
        }
        _uiState.update { it.copy(creationErrorRes = messageRes) }
    }

    private fun setMessage(@StringRes messageRes: Int) {
        _uiState.update { it.copy(messageRes = messageRes) }
    }
}

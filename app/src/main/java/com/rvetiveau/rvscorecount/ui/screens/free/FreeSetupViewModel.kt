package com.rvetiveau.rvscorecount.ui.screens.free

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rvetiveau.rvscorecount.R
import com.rvetiveau.rvscorecount.domain.engine.FreeGameRules
import com.rvetiveau.rvscorecount.domain.engine.GameIds
import com.rvetiveau.rvscorecount.domain.model.ParticipantCreationData
import com.rvetiveau.rvscorecount.domain.model.WinDirection
import com.rvetiveau.rvscorecount.domain.usecase.CreateFreePartyUseCase
import com.rvetiveau.rvscorecount.domain.usecase.FreePartyDraft
import com.rvetiveau.rvscorecount.domain.usecase.InvalidPartyConfigurationException
import com.rvetiveau.rvscorecount.domain.usecase.ObserveFreeGameNamesUseCase
import com.rvetiveau.rvscorecount.domain.usecase.ObserveKnownPlayerNamesUseCase
import com.rvetiveau.rvscorecount.ui.navigation.NavigationEvent
import com.rvetiveau.rvscorecount.ui.navigation.Routes
import com.rvetiveau.rvscorecount.ui.screens.setup.DraftAddition
import com.rvetiveau.rvscorecount.ui.screens.setup.moved
import com.rvetiveau.rvscorecount.ui.screens.setup.withAdded
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

/** Owns the draft of a Partie of Jeu libre (Story 11.2) and turns it into a persisted Partie. */
@HiltViewModel
class FreeSetupViewModel @Inject constructor(
    private val createFreePartyUseCase: CreateFreePartyUseCase,
    rules: FreeGameRules,
    observeKnownPlayerNamesUseCase: ObserveKnownPlayerNamesUseCase,
    observeFreeGameNamesUseCase: ObserveFreeGameNamesUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(FreeSetupUiState(minPlayers = rules.minPlayers, maxPlayers = rules.maxPlayers))
    val uiState: StateFlow<FreeSetupUiState> = _uiState.asStateFlow()

    private val _navigationEvents = Channel<NavigationEvent>(Channel.BUFFERED)
    val navigationEvents: Flow<NavigationEvent> = _navigationEvents.receiveAsFlow()

    init {
        viewModelScope.launch {
            observeKnownPlayerNamesUseCase().collect { names -> _uiState.update { it.copy(knownPlayerNames = names) } }
        }
        viewModelScope.launch {
            observeFreeGameNamesUseCase().collect { names -> _uiState.update { it.copy(knownGameNames = names) } }
        }
    }

    /** At most [FreeSetupUiState.MAX_GAME_NAME_LENGTH] characters: a longer input is ignored. */
    fun onGameNameChanged(name: String) {
        if (name.length > FreeSetupUiState.MAX_GAME_NAME_LENGTH) return
        _uiState.update { it.copy(gameNameInput = name, creationErrorRes = null) }
    }

    fun onGameNameSuggestionSelected(name: String) = onGameNameChanged(name)

    fun onNameChanged(name: String) {
        _uiState.update { it.copy(nameInput = name, messageRes = null, creationErrorRes = null) }
    }

    fun onAddParticipant() = addParticipant(_uiState.value.nameInput)

    /** A tapped known name joins the table at once, whatever was being typed. */
    fun onSuggestionSelected(name: String) = addParticipant(name)

    private fun addParticipant(name: String) {
        when (val addition = _uiState.value.participants.withAdded(name, _uiState.value.maxPlayers)) {
            is DraftAddition.Refused -> _uiState.update { it.copy(messageRes = addition.messageRes) }
            is DraftAddition.Added -> _uiState.update {
                it.copy(participants = addition.participants, nameInput = "", messageRes = null, creationErrorRes = null)
            }
        }
    }

    fun onRemoveParticipant(id: UUID) {
        _uiState.update { state ->
            state.copy(participants = state.participants.filterNot { it.id == id }, messageRes = null, creationErrorRes = null)
        }
    }

    fun onMoveParticipant(fromIndex: Int, toIndex: Int) {
        _uiState.update { state ->
            state.participants.moved(fromIndex, toIndex)?.let { state.copy(participants = it, creationErrorRes = null) } ?: state
        }
    }

    fun onWinDirectionChanged(direction: WinDirection) {
        _uiState.update { it.copy(winDirection = direction, creationErrorRes = null) }
    }

    fun onEndModeChanged(mode: FreeEndMode) {
        _uiState.update { it.copy(endMode = mode, creationErrorRes = null) }
    }

    /** Digits only, at most four: the value is checked by [FreeSetupUiState.isEndValueValid]. */
    fun onEndValueChanged(value: String) {
        if (value.length > MAX_END_DIGITS || !value.all(Char::isDigit)) return
        _uiState.update { state ->
            when (state.endMode) {
                FreeEndMode.MANUAL -> state
                FreeEndMode.TARGET_SCORE -> state.copy(targetScoreInput = value)
                FreeEndMode.ROUND_COUNT -> state.copy(roundCountInput = value)
            }.copy(creationErrorRes = null)
        }
    }

    /** Guards a double tap: [FreeSetupUiState.canStart] excludes an in-flight or completed creation. */
    fun onStartParty() {
        val state = _uiState.value
        if (!state.canStart) return
        _uiState.update { it.copy(isCreatingParty = true, creationErrorRes = null) }

        viewModelScope.launch {
            try {
                val draft = FreePartyDraft(
                    gameName = state.gameNameInput,
                    winDirection = state.winDirection,
                    endRule = state.endRule,
                    participants = state.participants.map { ParticipantCreationData(id = it.id, name = it.name) }
                )
                createFreePartyUseCase(draft).fold(
                    onSuccess = { partyId ->
                        _uiState.update { it.copy(creationCompleted = true) }
                        _navigationEvents.send(NavigationEvent.ReplaceAll(Routes.FreeParty(partyId.toString())))
                    },
                    onFailure = { failure ->
                        val messageRes = if (failure is InvalidPartyConfigurationException) {
                            R.string.setup_error_invalid_state
                        } else {
                            R.string.setup_error_creation_failed
                        }
                        _uiState.update { it.copy(creationErrorRes = messageRes) }
                    }
                )
            } finally {
                _uiState.update { it.copy(isCreatingParty = false) }
            }
        }
    }

    /** Historique of the Jeu libre only (Story 8.2); the draft stays below it on the back stack. */
    fun onHistorySelected() {
        viewModelScope.launch { _navigationEvents.send(NavigationEvent.NavigateTo(Routes.GameHistory(GameIds.FREE))) }
    }

    private companion object {
        const val MAX_END_DIGITS = 4
    }
}

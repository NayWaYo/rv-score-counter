package com.rvetiveau.rvscorecount.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rvetiveau.rvscorecount.domain.model.PlayerRecords
import com.rvetiveau.rvscorecount.domain.usecase.GetPlayerRecordsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface PlayerProfileUiState {
    data object Loading : PlayerProfileUiState
    data object NotFound : PlayerProfileUiState
    data object LoadError : PlayerProfileUiState
    data class Loaded(val records: PlayerRecords) : PlayerProfileUiState
}

/** A Joueur's profile (Story 10.3), computed once when the screen opens. */
@HiltViewModel
class PlayerProfileViewModel @Inject constructor(
    private val getPlayerRecordsUseCase: GetPlayerRecordsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<PlayerProfileUiState>(PlayerProfileUiState.Loading)
    val uiState: StateFlow<PlayerProfileUiState> = _uiState.asStateFlow()

    private var initializedPlayerId: UUID? = null

    /** Idempotent for the same [playerId]: a recomposition never computes the records again. */
    fun initialize(playerId: UUID) {
        if (initializedPlayerId == playerId) return
        initializedPlayerId = playerId
        _uiState.value = PlayerProfileUiState.Loading
        viewModelScope.launch {
            _uiState.value = try {
                getPlayerRecordsUseCase(playerId)?.let { PlayerProfileUiState.Loaded(it) } ?: PlayerProfileUiState.NotFound
            } catch (cancellation: CancellationException) {
                initializedPlayerId = null
                throw cancellation
            } catch (_: Exception) {
                PlayerProfileUiState.LoadError
            }
        }
    }
}

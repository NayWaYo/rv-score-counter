package com.rvetiveau.rvscorecount.ui.screens.players

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rvetiveau.rvscorecount.R
import com.rvetiveau.rvscorecount.domain.usecase.ObservePlayersUseCase
import com.rvetiveau.rvscorecount.domain.usecase.RenamePlayerOutcome
import com.rvetiveau.rvscorecount.domain.usecase.RenamePlayerUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Lists the Joueurs (Story 10.2) from Room and renames them; the list follows every change. */
@HiltViewModel
class PlayersViewModel @Inject constructor(
    observePlayersUseCase: ObservePlayersUseCase,
    private val renamePlayerUseCase: RenamePlayerUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<PlayersUiState>(PlayersUiState.Loading)
    val uiState: StateFlow<PlayersUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            observePlayersUseCase()
                .catch { _uiState.value = PlayersUiState.LoadError }
                .collect { players ->
                    _uiState.update { state ->
                        when {
                            players.isEmpty() -> PlayersUiState.Empty
                            // A dialog stays open while its Joueur is still listed.
                            state is PlayersUiState.Loaded -> PlayersUiState.Loaded(
                                players,
                                state.renaming?.takeIf { renaming -> players.any { it.id == renaming.playerId } }
                            )
                            else -> PlayersUiState.Loaded(players)
                        }
                    }
                }
        }
    }

    fun onRenameRequested(playerId: UUID) {
        updateLoaded { state ->
            val player = state.players.firstOrNull { it.id == playerId } ?: return@updateLoaded state
            state.copy(renaming = PlayerRenaming(playerId, player.name, input = player.name))
        }
    }

    fun onRenameInputChanged(input: String) {
        updateRenaming { it.copy(input = input, errorRes = null, takenBy = null) }
    }

    fun onRenameDismissed() {
        updateLoaded { state -> if (state.renaming?.isSaving == true) state else state.copy(renaming = null) }
    }

    fun onRenameConfirmed() {
        val renaming = (_uiState.value as? PlayersUiState.Loaded)?.renaming ?: return
        if (renaming.isSaving) return
        updateRenaming { it.copy(isSaving = true, errorRes = null, takenBy = null) }

        viewModelScope.launch {
            val outcome = try {
                renamePlayerUseCase(renaming.playerId, renaming.input)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                null
            }
            when (outcome) {
                RenamePlayerOutcome.Renamed -> updateLoaded { it.copy(renaming = null) }
                RenamePlayerOutcome.BlankName -> updateRenaming {
                    it.copy(isSaving = false, errorRes = R.string.setup_error_empty_name)
                }
                is RenamePlayerOutcome.NameTaken -> updateRenaming {
                    it.copy(isSaving = false, errorRes = R.string.players_rename_taken, takenBy = outcome.takenBy)
                }
                RenamePlayerOutcome.UnknownPlayer, null -> updateRenaming {
                    it.copy(isSaving = false, errorRes = R.string.players_rename_failed)
                }
            }
        }
    }

    private fun updateLoaded(transform: (PlayersUiState.Loaded) -> PlayersUiState.Loaded) {
        _uiState.update { state -> if (state is PlayersUiState.Loaded) transform(state) else state }
    }

    private fun updateRenaming(transform: (PlayerRenaming) -> PlayerRenaming) {
        updateLoaded { state -> state.renaming?.let { state.copy(renaming = transform(it)) } ?: state }
    }
}

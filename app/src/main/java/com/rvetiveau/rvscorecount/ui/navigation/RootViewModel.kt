package com.rvetiveau.rvscorecount.ui.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rvetiveau.rvscorecount.domain.engine.GameIds
import com.rvetiveau.rvscorecount.domain.usecase.ResumePartyUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.coroutines.coroutineContext
import javax.inject.Inject

/** Decides the app's start destination once, by resuming an in-progress Party if any (AD-6). */
@HiltViewModel
class RootViewModel @Inject constructor(
    private val resumePartyUseCase: ResumePartyUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<RootUiState>(RootUiState.Loading)
    val uiState: StateFlow<RootUiState> = _uiState.asStateFlow()
    private var resumeJob: Job? = null

    init {
        resume()
    }

    fun retry() = resume()

    private fun resume() {
        resumeJob?.cancel()
        _uiState.value = RootUiState.Loading
        resumeJob = viewModelScope.launch {
            val nextState = try {
                val party = resumePartyUseCase()
                val startDestination = when {
                    party == null -> Routes.Home
                    party.gameId == GameIds.BELOTE -> Routes.BeloteParty(party.id.toString())
                    else -> Routes.Party(party.id.toString())
                }
                RootUiState.Ready(startDestination)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (throwable: Throwable) {
                RootUiState.Error(throwable)
            }
            coroutineContext.ensureActive()
            _uiState.value = nextState
        }
    }
}

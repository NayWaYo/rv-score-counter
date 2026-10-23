package com.rvetiveau.rvscorecount.ui.screens.belote

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rvetiveau.rvscorecount.domain.model.BelotePartyState
import com.rvetiveau.rvscorecount.domain.model.TeamSide
import com.rvetiveau.rvscorecount.domain.usecase.ObserveBelotePartyUseCase
import com.rvetiveau.rvscorecount.ui.components.RoundTablePlayer
import com.rvetiveau.rvscorecount.ui.components.ScoreSeries
import com.rvetiveau.rvscorecount.ui.components.TakerUiModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

sealed interface BeloteStatsUiState {
    data object Loading : BeloteStatsUiState
    data object Unavailable : BeloteStatsUiState

    /**
     * One cumulated series per Équipe, in creation order, starting at 0 before the first Manche; the
     * [takers] ranked for « 🗿 Chad de la game », empty when no Manche records who took (Story 8.3).
     */
    data class Loaded(
        val series: List<ScoreSeries>,
        val takers: List<TakerUiModel> = emptyList()
    ) : BeloteStatsUiState {
        val roundCount: Int get() = (series.firstOrNull()?.points?.size ?: 1) - 1
    }
}

/** Statistiques of a Partie of Belote, in progress or finished (Story 7.4, AC 3), observed live. */
@HiltViewModel
class BeloteStatsViewModel @Inject constructor(
    private val observeBelotePartyUseCase: ObserveBelotePartyUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<BeloteStatsUiState>(BeloteStatsUiState.Loading)
    val uiState: StateFlow<BeloteStatsUiState> = _uiState.asStateFlow()

    private var partyId: UUID? = null

    fun initialize(partyId: UUID) {
        if (this.partyId != null) return
        this.partyId = partyId
        viewModelScope.launch {
            try {
                observeBelotePartyUseCase(partyId).collect { state ->
                    _uiState.value = if (state == null) {
                        BeloteStatsUiState.Unavailable
                    } else {
                        BeloteStatsUiState.Loaded(
                            series = state.cumulativeSeries(),
                            takers = state.takerRanking().map { TakerUiModel(it.player.name, it.takes, it.made) }
                        )
                    }
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Throwable) {
                _uiState.value = BeloteStatsUiState.Unavailable
            }
        }
    }
}

/** The chart keys a series by an id: an Équipe has none, so one is derived from its side. */
internal fun BelotePartyState.cumulativeSeries(): List<ScoreSeries> = TeamSide.entries.map { side ->
    var sum = 0
    ScoreSeries(
        player = RoundTablePlayer(UUID.nameUUIDFromBytes(side.name.toByteArray()), team(side).name),
        points = listOf(0) + results.map { result ->
            sum += result.points[side]
            sum
        }
    )
}

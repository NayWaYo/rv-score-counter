package com.rvetiveau.rvscorecount.ui.screens.tarot

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.rvetiveau.rvscorecount.R
import com.rvetiveau.rvscorecount.domain.usecase.ObserveTarotPartyUseCase
import com.rvetiveau.rvscorecount.ui.components.ChadCard
import com.rvetiveau.rvscorecount.ui.components.RoundTable
import com.rvetiveau.rvscorecount.ui.components.ScoreChart
import com.rvetiveau.rvscorecount.ui.components.ScreenHeader
import com.rvetiveau.rvscorecount.ui.components.TakerUiModel
import com.rvetiveau.rvscorecount.ui.screens.belote.Centered
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface TarotStatsUiState {
    data object Loading : TarotStatsUiState
    data object Unavailable : TarotStatsUiState

    /** The points of every Manche, for the cumulated curve, and the [takers] ranked for « 🗿 Chad de la game ». */
    data class Loaded(val table: RoundTable, val takers: List<TakerUiModel>) : TarotStatsUiState
}

/** Statistiques of a Partie of Tarot, in progress or finished (Story 13.4), observed live. */
@HiltViewModel
class TarotStatsViewModel @Inject constructor(
    private val observeTarotPartyUseCase: ObserveTarotPartyUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<TarotStatsUiState>(TarotStatsUiState.Loading)
    val uiState: StateFlow<TarotStatsUiState> = _uiState.asStateFlow()

    private var partyId: UUID? = null

    fun initialize(partyId: UUID) {
        if (this.partyId != null) return
        this.partyId = partyId
        viewModelScope.launch {
            try {
                observeTarotPartyUseCase(partyId).collect { state ->
                    _uiState.value = if (state == null) {
                        TarotStatsUiState.Unavailable
                    } else {
                        TarotStatsUiState.Loaded(
                            table = state.toUiState().roundTable,
                            takers = state.takerRanking().map { TakerUiModel(it.player.name, it.takes, it.made) }
                        )
                    }
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Throwable) {
                _uiState.value = TarotStatsUiState.Unavailable
            }
        }
    }
}

/** Compose/Hilt adapter for the Statistiques of a Partie of Tarot (AD-4). */
@Composable
fun TarotStatsRoute(
    partyId: UUID,
    modifier: Modifier = Modifier,
    viewModel: TarotStatsViewModel = hiltViewModel()
) {
    LaunchedEffect(partyId) { viewModel.initialize(partyId) }

    val state by viewModel.uiState.collectAsStateWithLifecycle()

    TarotStatsScreen(state = state, modifier = modifier)
}

/** Stateless Statistiques of a Partie of Tarot (Story 13.4): each Joueur's cumulated score, then who took. */
@Composable
fun TarotStatsScreen(state: TarotStatsUiState, modifier: Modifier = Modifier) {
    when (state) {
        TarotStatsUiState.Loading -> Centered(modifier) {
            CircularProgressIndicator(modifier = Modifier.testTag("tarot_stats_loading"))
        }
        TarotStatsUiState.Unavailable -> Centered(modifier) {
            Text(text = stringResource(R.string.party_not_found), modifier = Modifier.testTag("tarot_stats_unavailable"))
        }
        is TarotStatsUiState.Loaded -> Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            ScreenHeader(
                title = stringResource(R.string.stats_title),
                titleTag = "tarot_stats_title",
                modifier = Modifier.padding(top = 8.dp)
            )
            Text(text = stringResource(R.string.tarot_stats_curve_title), style = MaterialTheme.typography.titleMedium)
            if (state.table.rows.isEmpty()) {
                Text(
                    text = stringResource(R.string.belote_stats_no_round),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.testTag("tarot_stats_no_round")
                )
            } else {
                ScoreChart(table = state.table, modifier = Modifier.testTag("tarot_stats_chart"))
            }
            if (state.takers.isNotEmpty()) ChadCard(state.takers)
        }
    }
}

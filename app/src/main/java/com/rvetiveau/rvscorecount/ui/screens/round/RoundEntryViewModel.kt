package com.rvetiveau.rvscorecount.ui.screens.round

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.rvetiveau.rvscorecount.domain.model.CardSuit
import com.rvetiveau.rvscorecount.domain.usecase.RollDieUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private const val SUIT_KEY = "suit"

/**
 * Holds the die roll draft in memory only (AC 5, 7, 8): no Round is persisted here, Story 3.4
 * builds it at validation time. [SavedStateHandle] restores just the suit name across a
 * system-initiated recreation; an unknown/corrupt saved value falls back to "not rolled" rather
 * than a default suit.
 */
@HiltViewModel
class RoundEntryViewModel @Inject constructor(
    private val rollDieUseCase: RollDieUseCase,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val restoredSuit: CardSuit? = savedStateHandle.get<String>(SUIT_KEY)?.let { name ->
        runCatching { CardSuit.valueOf(name) }.getOrNull()
    }

    private val _uiState = MutableStateFlow(RoundEntryUiState(suit = restoredSuit))
    val uiState: StateFlow<RoundEntryUiState> = _uiState.asStateFlow()

    /** Same intent for « Lancer » and « Relancer » (AC 3, 6): always draws exactly once per tap. */
    fun onRoll() {
        val suit = rollDieUseCase()
        savedStateHandle[SUIT_KEY] = suit.name
        _uiState.value = RoundEntryUiState(suit = suit)
    }
}

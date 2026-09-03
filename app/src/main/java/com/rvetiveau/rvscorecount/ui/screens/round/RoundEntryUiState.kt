package com.rvetiveau.rvscorecount.ui.screens.round

import com.rvetiveau.rvscorecount.domain.model.CardSuit

/** [suit] is null before any roll (AC 2, 7); no Round exists until Story 3.4 validates. */
data class RoundEntryUiState(
    val suit: CardSuit? = null
)

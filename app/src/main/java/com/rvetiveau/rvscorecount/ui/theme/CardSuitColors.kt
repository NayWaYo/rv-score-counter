package com.rvetiveau.rvscorecount.ui.theme

import androidx.compose.ui.graphics.Color
import com.rvetiveau.rvscorecount.domain.model.CardSuit

val CardSuit.color: Color
    get() = when (this) {
        CardSuit.SPADE -> Spade
        CardSuit.HEART -> Heart
        CardSuit.CLUB -> Club
        CardSuit.DIAMOND -> Diamond
    }

val CardSuit.tint: Color
    get() = when (this) {
        CardSuit.SPADE -> SpadeTint
        CardSuit.HEART -> HeartTint
        CardSuit.CLUB -> ClubTint
        CardSuit.DIAMOND -> DiamondTint
    }

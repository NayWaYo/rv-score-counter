package com.rvetiveau.rvscorecount.ui.theme

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import com.rvetiveau.rvscorecount.R
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

@get:StringRes
val CardSuit.pictogramRes: Int
    get() = when (this) {
        CardSuit.SPADE -> R.string.suit_spade_pictogram
        CardSuit.HEART -> R.string.suit_heart_pictogram
        CardSuit.CLUB -> R.string.suit_club_pictogram
        CardSuit.DIAMOND -> R.string.suit_diamond_pictogram
    }

@get:StringRes
val CardSuit.nameRes: Int
    get() = when (this) {
        CardSuit.SPADE -> R.string.suit_spade
        CardSuit.HEART -> R.string.suit_heart
        CardSuit.CLUB -> R.string.suit_club
        CardSuit.DIAMOND -> R.string.suit_diamond
    }

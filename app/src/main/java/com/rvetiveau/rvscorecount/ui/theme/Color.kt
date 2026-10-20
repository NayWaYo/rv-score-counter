package com.rvetiveau.rvscorecount.ui.theme

import androidx.compose.ui.graphics.Color

// Surface tokens (UX-DR1)
val BackgroundBase = Color(0xFF121212)
val SurfaceRaised = Color(0xFF1E1E1E)
val SurfaceContainer = Color(0xFF2C2C2C)

// Text. InkMuted marks what matters less (worst score, hints, neutral badges), never an error.
val InkPrimary = Color(0xFFE8E0D5)
val InkMuted = Color(0xFFA8A097)

// Actions and the podium only, the best score of a Manche included
val Accent = Color(0xFFFFB74D)
val OnAccent = Color(0xFF1A1200)

// Feedback: the error colour only flags an input to fix
val Error = Color(0xFFCF6679)
val Success = Color(0xFF4CAF82)

// CardSuit colors (UX-DR6) — usage restreint: résultat du dé et historique des manches
val Spade = Color(0xFFD5DCE2)
val Heart = Color(0xFFEF5350)
val Club = Color(0xFF66BB6A)
val Diamond = Color(0xFF42A5F5)

// CardSuit tints (UX-DR6): Spade a neutral cool grey, Diamond a clear blue, so the two never mix up
val SpadeTint = Color(0xFF1C2024)
val HeartTint = Color(0xFF2D1517)
val ClubTint = Color(0xFF182A1A)
val DiamondTint = Color(0xFF0E2440)

// Statistic cards: a tinted card with its own ink per statistic — brown for « Poissard ou Nul ? »,
// violet for « La fusée »
val ChampionTint = Color(0xFF2B1F14)
val ChampionInk = Color(0xFFC69C6D)
val RecordTint = Color(0xFF261C38)
val RecordInk = Color(0xFFB39DDB)

// Chart series, dark steps of the reference categorical palette in this fixed order, validated on
// SurfaceRaised as adjacent pairs for lines up to eight series. The first five keep away from the
// suit colours and the accent; blue, green and red only come with a sixth player. A series keeps
// its slot from its table position.
val ChartSeries = listOf(
    Color(0xFFC98500),
    Color(0xFFD55181),
    Color(0xFF9085E9),
    Color(0xFF199E70),
    Color(0xFFD95926),
    Color(0xFF3987E5),
    Color(0xFF008300),
    Color(0xFFE66767)
)

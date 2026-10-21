package com.rvetiveau.rvscorecount.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

// Dark theme (UX-DR1). Surfaces, text, accent: actions, the podium and the best score only;
// error: an input to fix only; InkMuted: what matters less (worst score, hints, neutral badges).
val BackgroundBase = Color(0xFF121212)
val SurfaceRaised = Color(0xFF1E1E1E)
val SurfaceContainer = Color(0xFF2C2C2C)
val InkPrimary = Color(0xFFE8E0D5)
val InkMuted = Color(0xFFA8A097)
val Accent = Color(0xFFFFB74D)
val OnAccent = Color(0xFF1A1200)
val Error = Color(0xFFCF6679)

// Light theme: warm paper, the same roles. The accent darkens so it still reads as text
// (≥ 4.5:1 on the surfaces), with white text on it.
val LightBackground = Color(0xFFFAF7F2)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceContainer = Color(0xFFECE6DC)
val LightInk = Color(0xFF2B2620)
val LightInkMuted = Color(0xFF6B6359)
val LightAccent = Color(0xFF9C5700)
val LightOnAccent = Color(0xFFFFFFFF)
val LightError = Color(0xFFB3261E)

/**
 * Colours Material's scheme has no slot for, one set per theme. Suit colours (UX-DR6) are only
 * for the die and the Manches; Spade and Diamond tints stay clearly apart. Chart series follow the
 * reference categorical palette in a fixed order, validated as adjacent pairs on the chart surface:
 * the first five keep away from the suit colours and the accent, and a series keeps its slot from
 * its table position in both themes.
 */
@Immutable
data class RvColors(
    val success: Color,
    val spade: Color,
    val heart: Color,
    val club: Color,
    val diamond: Color,
    val spadeTint: Color,
    val heartTint: Color,
    val clubTint: Color,
    val diamondTint: Color,
    /** « Poissard ou Nul ? » card: brown. */
    val championTint: Color,
    val championInk: Color,
    /** « La fusée » card: violet. */
    val recordTint: Color,
    val recordInk: Color,
    /** « 🗿 Chad de la game » card (Belote): lagoon. */
    val chadTint: Color,
    val chadInk: Color,
    val chartSeries: List<Color>
)

val DarkRvColors = RvColors(
    success = Color(0xFF4CAF82),
    spade = Color(0xFFD5DCE2),
    heart = Color(0xFFEF5350),
    club = Color(0xFF66BB6A),
    diamond = Color(0xFF42A5F5),
    spadeTint = Color(0xFF1C2024),
    heartTint = Color(0xFF2D1517),
    clubTint = Color(0xFF182A1A),
    diamondTint = Color(0xFF0E2440),
    championTint = Color(0xFF2B1F14),
    championInk = Color(0xFFC69C6D),
    recordTint = Color(0xFF261C38),
    recordInk = Color(0xFFB39DDB),
    chadTint = Color(0xFF10302B),
    chadInk = Color(0xFF7FCBB8),
    chartSeries = listOf(
        Color(0xFFC98500),
        Color(0xFFD55181),
        Color(0xFF9085E9),
        Color(0xFF199E70),
        Color(0xFFD95926),
        Color(0xFF3987E5),
        Color(0xFF008300),
        Color(0xFFE66767)
    )
)

val LightRvColors = RvColors(
    success = Color(0xFF2E7D5B),
    spade = Color(0xFF37474F),
    heart = Color(0xFFC62828),
    club = Color(0xFF2E7D32),
    diamond = Color(0xFF1565C0),
    spadeTint = Color(0xFFDADDE0),
    heartTint = Color(0xFFF8D9D9),
    clubTint = Color(0xFFD8EED9),
    diamondTint = Color(0xFFC2D9FA),
    championTint = Color(0xFFF3E7D9),
    championInk = Color(0xFF7A4A1E),
    recordTint = Color(0xFFECE5F7),
    recordInk = Color(0xFF5B4596),
    chadTint = Color(0xFFD9F0EA),
    chadInk = Color(0xFF1E6B5D),
    // Light steps of the same hues; the three lightest sit under 3:1 on white, relieved by the
    // legend and end labels. Slots 7 and 8 only reach the CVD floor, also covered by the labels.
    chartSeries = listOf(
        Color(0xFFEDA100),
        Color(0xFFE87BA4),
        Color(0xFF6250D6),
        Color(0xFF1BAF7A),
        Color(0xFFEB6834),
        Color(0xFF2A78D6),
        Color(0xFF008300),
        Color(0xFFE34948)
    )
)

package com.rvetiveau.rvscorecount.domain.engine

/** Canonical, single source of truth for Game identifiers shared by engines and the UI catalog. */
object GameIds {
    const val PAPAYOO = "papayoo"
    const val BELOTE = "belote"

    /** Tarot (AD-10): 3 to 5 players, contracts, bouts and bonuses. */
    const val TAROT = "tarot"

    /** Jeu libre (AD-9): any game without its own engine. */
    const val FREE = "free"

    /**
     * Order of the Accueil tiles: the order the Jeux were added to the app. The Tarot joins it, before
     * the Jeu libre, with its creation screen (Story 13.3).
     */
    val CATALOG_ORDER = listOf(PAPAYOO, BELOTE, FREE)
}

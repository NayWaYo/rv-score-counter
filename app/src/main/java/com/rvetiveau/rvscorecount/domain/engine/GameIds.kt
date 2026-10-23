package com.rvetiveau.rvscorecount.domain.engine

/** Canonical, single source of truth for Game identifiers shared by engines and the UI catalog. */
object GameIds {
    const val PAPAYOO = "papayoo"
    const val BELOTE = "belote"

    /** Tarot (AD-10): 3 to 5 players, contracts, bouts and bonuses. */
    const val TAROT = "tarot"

    /** Jeu libre (AD-9): any game without its own engine. */
    const val FREE = "free"

    /** Every Jeu the data can hold, whether on the Accueil yet or not: what a backup may contain. */
    val STORED = listOf(PAPAYOO, BELOTE, TAROT, FREE)

    /** Order of the Accueil tiles: the order the Jeux were added, the Jeu libre for anything else last. */
    val CATALOG_ORDER = listOf(PAPAYOO, BELOTE, TAROT, FREE)
}

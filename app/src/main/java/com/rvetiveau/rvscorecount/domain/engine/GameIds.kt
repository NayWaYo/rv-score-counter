package com.rvetiveau.rvscorecount.domain.engine

/** Canonical, single source of truth for Game identifiers shared by engines and the UI catalog. */
object GameIds {
    const val PAPAYOO = "papayoo"
    const val BELOTE = "belote"

    /** Jeu libre (AD-9): any game without its own engine; on the Accueil from Story 11.2. */
    const val FREE = "free"

    /** Order of the Accueil tiles: the order the Jeux were added to the app. */
    val CATALOG_ORDER = listOf(PAPAYOO, BELOTE)
}

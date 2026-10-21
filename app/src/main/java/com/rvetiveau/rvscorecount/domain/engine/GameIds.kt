package com.rvetiveau.rvscorecount.domain.engine

/** Canonical, single source of truth for Game identifiers shared by engines and the UI catalog. */
object GameIds {
    const val PAPAYOO = "papayoo"
    const val BELOTE = "belote"

    /** Order of the Accueil tiles: the order the Jeux were added to the app. */
    val CATALOG_ORDER = listOf(PAPAYOO, BELOTE)
}

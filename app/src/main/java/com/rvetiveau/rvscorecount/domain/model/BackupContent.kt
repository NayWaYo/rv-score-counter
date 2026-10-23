package com.rvetiveau.rvscorecount.domain.model

/** What a backup made now would hold (Story 12.1): every Joueur and every Partie, in progress included. */
data class BackupContent(val players: Int, val parties: Int) {
    val isEmpty: Boolean get() = players == 0 && parties == 0
}

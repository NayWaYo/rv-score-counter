package com.rvetiveau.rvscorecount.domain.engine

/**
 * A Jeu of the Accueil catalogue (AD-8): what every game shares, whatever its scoring contract
 * ([GameEngine] by player, [BeloteRules] by Équipe).
 */
interface Game {
    val gameId: String
}

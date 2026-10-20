package com.rvetiveau.rvscorecount.game.papayoo

import com.rvetiveau.rvscorecount.domain.engine.DealingRules
import com.rvetiveau.rvscorecount.domain.engine.GameEngine
import com.rvetiveau.rvscorecount.domain.engine.GameIds
import com.rvetiveau.rvscorecount.domain.engine.RoundValidation
import com.rvetiveau.rvscorecount.domain.model.ParticipantId
import javax.inject.Inject

class PapayooEngine @Inject constructor() : GameEngine {
    override val gameId: String = GAME_ID
    override val minPlayers: Int = 3
    override val maxPlayers: Int = 8
    override val totalPointsPerRound: Int = 250
    override val suitSevenPoints: Int = 40

    /** 60 cards; at 7 and 8 players the four suit 1s are taken out (56 cards), the Payoo 1 stays. */
    override fun dealingRules(playerCount: Int): DealingRules? = when (playerCount) {
        3 -> DealingRules(cardsPerPlayer = 20, cardsPassed = 5)
        4 -> DealingRules(cardsPerPlayer = 15, cardsPassed = 5)
        5 -> DealingRules(cardsPerPlayer = 12, cardsPassed = 4)
        6 -> DealingRules(cardsPerPlayer = 10, cardsPassed = 3)
        7 -> DealingRules(cardsPerPlayer = 8, cardsPassed = 3, removedSuitRank = 1)
        8 -> DealingRules(cardsPerPlayer = 7, cardsPassed = 3, removedSuitRank = 1)
        else -> null
    }

    override fun precomputeLastScore(entered: Map<ParticipantId, Int>): Int =
        totalPointsPerRound - entered.values.sum()

    override fun validateRound(scores: Map<ParticipantId, Int>): RoundValidation {
        val difference = scores.values.sum() - totalPointsPerRound
        return if (difference == 0) RoundValidation.Success else RoundValidation.Error(difference)
    }

    companion object {
        const val GAME_ID = GameIds.PAPAYOO
    }
}

package com.rvetiveau.rvscorecount.game.papayoo

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

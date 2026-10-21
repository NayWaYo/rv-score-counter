package com.rvetiveau.rvscorecount.domain.model

/** How a Partie of Belote ends, chosen at its creation. */
sealed interface BeloteEndRule {

    /** Ends after the Manche where an Équipe reaches [points]. */
    data class TargetScore(val points: Int) : BeloteEndRule {
        init {
            require(points > 0) { "The target score must be positive" }
        }
    }

    /** Ends after [rounds] Manches. */
    data class RoundCount(val rounds: Int) : BeloteEndRule {
        init {
            require(rounds > 0) { "The round count must be positive" }
        }
    }

    companion object {
        const val DEFAULT_TARGET_SCORE = 1000
        const val DEFAULT_ROUND_COUNT = 10
    }
}

sealed interface BelotePartyStatus {
    /** The end is not reached, or the totals are equal there: one more Manche is played. */
    data object InProgress : BelotePartyStatus

    data class Won(val winner: TeamSide) : BelotePartyStatus
}

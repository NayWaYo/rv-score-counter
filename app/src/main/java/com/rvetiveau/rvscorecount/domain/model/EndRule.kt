package com.rvetiveau.rvscorecount.domain.model

/** How a Partie ends, chosen at its creation: Belote and Jeu libre (AD-9), whose manual end has no rule. */
sealed interface EndRule {

    /** Ends after the Manche where a total (an Équipe, or a Joueur of the Jeu libre) reaches [points]. */
    data class TargetScore(val points: Int) : EndRule {
        init {
            require(points > 0) { "The target score must be positive" }
        }
    }

    /** Ends after [rounds] Manches. */
    data class RoundCount(val rounds: Int) : EndRule {
        init {
            require(rounds > 0) { "The round count must be positive" }
        }
    }

    /** The Belote's defaults. */
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

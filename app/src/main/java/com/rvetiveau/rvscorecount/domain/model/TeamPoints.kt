package com.rvetiveau.rvscorecount.domain.model

/** Points of each Équipe, for one Manche or cumulated over a Partie. */
data class TeamPoints(val first: Int, val second: Int) {

    operator fun get(side: TeamSide): Int = when (side) {
        TeamSide.FIRST -> first
        TeamSide.SECOND -> second
    }

    operator fun plus(other: TeamPoints): TeamPoints = TeamPoints(first + other.first, second + other.second)

    companion object {
        val ZERO = TeamPoints(0, 0)

        fun of(side: TeamSide, sidePoints: Int, opponentPoints: Int): TeamPoints = when (side) {
            TeamSide.FIRST -> TeamPoints(sidePoints, opponentPoints)
            TeamSide.SECOND -> TeamPoints(opponentPoints, sidePoints)
        }
    }
}

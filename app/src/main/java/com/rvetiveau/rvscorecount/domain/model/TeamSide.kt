package com.rvetiveau.rvscorecount.domain.model

/** One of the two Équipes of a team game, in creation order. */
enum class TeamSide {
    FIRST,
    SECOND;

    val opponent: TeamSide
        get() = when (this) {
            FIRST -> SECOND
            SECOND -> FIRST
        }
}

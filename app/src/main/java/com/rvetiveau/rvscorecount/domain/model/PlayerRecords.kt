package com.rvetiveau.rvscorecount.domain.model

import java.util.UUID

/** A Joueur's profile (Story 10.3): finished Parties only; a section is null when the Jeu has none. */
data class PlayerRecords(
    val player: PlayerSummary,
    val papayoo: PapayooRecords?,
    val belote: BeloteRecords?,
    val free: FreeRecords? = null,
    val tarot: TarotRecords? = null
)

/** [wins] over [parties]: the headline of every section. */
data class WinRecord(val wins: Int, val parties: Int) {
    val rate: Double get() = wins.toDouble() / parties
}

/**
 * Papayoo figures over the Manches the Joueur played. [sevensRecorded] counts those where the
 * holder of the 7 is known, the base of [sevenRate]; [fullRounds] are Manches where they took all the
 * [fullRoundPoints] of a Manche (250).
 */
data class PapayooRecords(
    val wins: WinRecord,
    val roundsPlayed: Int,
    val sevensTaken: Int,
    val sevensRecorded: Int,
    val zeroRounds: Int,
    val worstRound: WorstRound?,
    val fullRounds: Int,
    val fullRoundPoints: Int
) {
    val sevenRate: Double? get() = sevensTaken.toDouble().div(sevensRecorded).takeIf { sevensRecorded > 0 }
    val zeroRate: Double? get() = zeroRounds.toDouble().div(roundsPlayed).takeIf { roundsPlayed > 0 }
}

data class WorstRound(val points: Int, val partyId: UUID, val playedAt: Long)

/** Belote figures: [takes] and [made] count the Manches whose taker player is known. */
data class BeloteRecords(
    val wins: WinRecord,
    val takes: Int,
    val made: Int,
    val bestTeammate: TeammateRecord?,
    val worstTeammate: TeammateRecord?
) {
    val takesPerParty: Double get() = takes.toDouble() / wins.parties
    val contractRate: Double? get() = made.toDouble().div(takes).takeIf { takes > 0 }
}

data class TeammateRecord(val playerId: UUID, val name: String, val wins: WinRecord)

/** One finished Partie of Papayoo with every stored row, and the seats the Joueur had in it. */
data class PapayooPlay(
    val party: Party,
    val participants: List<Participant>,
    val rounds: List<Round>,
    val scores: List<RoundScore>,
    val seats: Set<UUID>
)

/**
 * One finished Partie of Belote and the Joueur's seat in it; [playerIdBySeat] gives the Joueur of
 * every seat, so that teammates are counted by Joueur, whatever their name in that Partie.
 */
data class BelotePlay(
    val state: BelotePartyState,
    val seat: UUID,
    val playerIdBySeat: Map<UUID, UUID>
)

/**
 * The Joueur's Papayoo record, null without any Partie. A win is the lowest Score Cumulé, ties
 * included; [fullRoundPoints] is the total of a Manche (250), taking all of it is a « Manche à 250 ».
 * A Partie whose rows do not match is an aggregate inconsistency: [computeStandings] throws.
 */
fun papayooRecords(plays: List<PapayooPlay>, fullRoundPoints: Int): PapayooRecords? {
    if (plays.isEmpty()) return null
    var wins = 0
    var roundsPlayed = 0
    var sevensTaken = 0
    var sevensRecorded = 0
    var zeroRounds = 0
    var fullRounds = 0
    var worst: WorstRound? = null
    plays.sortedBy { it.party.createdAt }.forEach { play ->
        val standings = computeStandings(play.participants, play.scores)
        val best = standings.minOfOrNull { it.cumulativeScore }
        if (best != null && standings.any { it.participant.id in play.seats && it.cumulativeScore == best }) wins++

        val roundsById = play.rounds.associateBy { it.id }
        play.scores
            .filter { it.participantId in play.seats }
            .sortedBy { roundsById[it.roundId]?.roundNumber ?: Int.MAX_VALUE }
            .forEach { score ->
                val round = roundsById[score.roundId]
                roundsPlayed++
                if (score.points == 0) zeroRounds++
                if (score.points == fullRoundPoints) fullRounds++
                if (round?.sevenHolderId != null) {
                    sevensRecorded++
                    if (round.sevenHolderId in play.seats) sevensTaken++
                }
                // Strictly greater: on a tie, the oldest Manche stays.
                val current = worst
                if (current == null || score.points > current.points) {
                    worst = WorstRound(score.points, play.party.id, play.party.createdAt)
                }
            }
    }
    return PapayooRecords(
        wins = WinRecord(wins, plays.size),
        roundsPlayed = roundsPlayed,
        sevensTaken = sevensTaken,
        sevensRecorded = sevensRecorded,
        zeroRounds = zeroRounds,
        worstRound = worst,
        fullRounds = fullRounds,
        fullRoundPoints = fullRoundPoints
    )
}

/**
 * The Joueur's Belote record, null without any Partie. Teammates are ranked by win rate together,
 * then by Parties together, then by name; the worst is only told apart when there are two or more.
 */
fun beloteRecords(plays: List<BelotePlay>, nameOf: (UUID) -> String): BeloteRecords? {
    if (plays.isEmpty()) return null
    var wins = 0
    var takes = 0
    var made = 0
    val withTeammate = mutableMapOf<UUID, WinRecord>()
    plays.forEach { play ->
        val state = play.state
        val side = checkNotNull(state.players.firstOrNull { it.id == play.seat }?.teamSide) {
            "Seat ${play.seat} is not a player of Belote party ${state.party.id}"
        }
        val won = (state.status as? BelotePartyStatus.Won)?.winner == side
        if (won) wins++
        state.takerRanking().firstOrNull { it.player.id == play.seat }?.let { stat ->
            takes += stat.takes
            made += stat.made
        }
        state.playersOf(side).filter { it.id != play.seat }.forEach { teammate ->
            val teammateId = play.playerIdBySeat[teammate.id] ?: return@forEach
            val record = withTeammate[teammateId] ?: WinRecord(0, 0)
            withTeammate[teammateId] = WinRecord(record.wins + if (won) 1 else 0, record.parties + 1)
        }
    }
    val ranked = withTeammate.map { (id, record) -> TeammateRecord(id, nameOf(id), record) }
    val best = ranked.sortedWith(
        compareByDescending<TeammateRecord> { it.wins.rate }.thenByDescending { it.wins.parties }.thenBy { playerNameKey(it.name) }
    ).firstOrNull()
    // Chosen among the others, so that equal rates never make the best teammate the worst too.
    val worst = ranked.filter { it != best }.sortedWith(
        compareBy<TeammateRecord> { it.wins.rate }.thenByDescending { it.wins.parties }.thenBy { playerNameKey(it.name) }
    ).firstOrNull()
    return BeloteRecords(WinRecord(wins, plays.size), takes, made, best, worst)
}

/** Jeu libre figures (Story 11.4): every game together, then [byGame], the most played first. */
data class FreeRecords(
    val wins: WinRecord,
    val byGame: List<FreeGameRecord>
)

data class FreeGameRecord(val gameName: String, val wins: WinRecord)

/** One finished Partie of Jeu libre and the Joueur's seat in it. */
data class FreePlay(val state: FreePartyState, val seat: UUID)

/**
 * The Joueur's Jeu libre record, null without any Partie. A win is a total at the best rank, ties
 * included; a Partie ended before its first Manche counts without a win. Game names that differ
 * only by case or surrounding spaces are one game, named as in its latest Partie; games are ranked
 * by Parties played, then by name.
 */
fun freeRecords(plays: List<FreePlay>): FreeRecords? {
    if (plays.isEmpty()) return null
    fun FreePlay.isWin() = state.rounds.isNotEmpty() && state.leaders.any { it.id == seat }
    fun List<FreePlay>.record() = WinRecord(count { it.isWin() }, size)
    val byGame = plays
        .groupBy { playerNameKey(it.state.gameName) }
        .values
        .map { games ->
            FreeGameRecord(games.maxBy { it.state.party.createdAt }.state.gameName.trim(), games.record())
        }
        .sortedWith(compareByDescending<FreeGameRecord> { it.wins.parties }.thenBy { playerNameKey(it.gameName) })
    return FreeRecords(plays.record(), byGame)
}

/**
 * Tarot figures (Story 13.4): [takes] over every finished Partie, [made] of them, and the contract
 * taken most often, null without any take.
 */
data class TarotRecords(
    val wins: WinRecord,
    val takes: Int,
    val made: Int,
    val favoriteContract: TarotContract?
) {
    val takesPerParty: Double get() = takes.toDouble() / wins.parties
    val contractRate: Double? get() = made.toDouble().div(takes).takeIf { takes > 0 }
}

/** One finished Partie of Tarot and the Joueur's seat in it. */
data class TarotPlay(val state: TarotPartyState, val seat: UUID)

/**
 * The Joueur's Tarot record, null without any Partie. A win is a total at the best rank, ties
 * included; a Partie ended before its first Manche counts without a win. Contracts taken equally
 * often go to the highest one.
 */
fun tarotRecords(plays: List<TarotPlay>): TarotRecords? {
    if (plays.isEmpty()) return null
    val wins = plays.count { play -> play.state.rounds.isNotEmpty() && play.state.leaders.any { it.id == play.seat } }
    val taken = plays.flatMap { play ->
        play.state.rounds.zip(play.state.results).filter { (round, _) -> round.input.takerId == play.seat }
    }
    val favorite = taken.groupingBy { (round, _) -> round.input.contract }.eachCount()
        .entries
        .sortedWith(compareByDescending<Map.Entry<TarotContract, Int>> { it.value }.thenByDescending { it.key.multiplier })
        .firstOrNull()
        ?.key
    return TarotRecords(WinRecord(wins, plays.size), taken.size, taken.count { (_, result) -> result.made }, favorite)
}

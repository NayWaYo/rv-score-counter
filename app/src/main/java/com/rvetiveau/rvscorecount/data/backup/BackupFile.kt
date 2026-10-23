package com.rvetiveau.rvscorecount.data.backup

import com.rvetiveau.rvscorecount.data.entity.BeloteRoundEntity
import com.rvetiveau.rvscorecount.data.entity.ParticipantEntity
import com.rvetiveau.rvscorecount.data.entity.PartyEntity
import com.rvetiveau.rvscorecount.data.entity.PlayerEntity
import com.rvetiveau.rvscorecount.data.entity.RoundEntity
import com.rvetiveau.rvscorecount.data.entity.RoundScoreEntity
import com.rvetiveau.rvscorecount.data.entity.TeamEntity
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * A backup file (Story 12.1): every row of the database, each Partie holding its own rows. It mirrors
 * the Room tables rather than the domain, so a restore (Story 12.2) writes back exactly the same rows;
 * enum values keep their persisted `name`. [format] changes only when this file changes, not with Room.
 */
@Serializable
data class BackupFile(
    val format: Int,
    val createdAt: Long,
    val players: List<BackupPlayer>,
    val parties: List<BackupParty>
)

@Serializable
data class BackupPlayer(val id: String, val name: String)

@Serializable
data class BackupParty(
    val id: String,
    val gameId: String,
    val status: String,
    val currentDealerIndex: Int,
    val createdAt: Long,
    val endRuleType: String? = null,
    val endRuleValue: Int? = null,
    val gameName: String? = null,
    val winDirection: String? = null,
    val participants: List<BackupParticipant>,
    val teams: List<BackupTeam> = emptyList(),
    val rounds: List<BackupRound> = emptyList(),
    val beloteRounds: List<BackupBeloteRound> = emptyList()
)

@Serializable
data class BackupParticipant(
    val id: String,
    val playerId: String,
    val name: String,
    val listPosition: Int,
    val teamSide: String? = null
)

@Serializable
data class BackupTeam(val side: String, val name: String)

@Serializable
data class BackupRound(
    val id: String,
    val roundNumber: Int,
    val suit: String? = null,
    val sevenHolderId: String? = null,
    val scores: List<BackupScore>
)

@Serializable
data class BackupScore(val participantId: String, val points: Int)

@Serializable
data class BackupBeloteRound(
    val id: String,
    val roundNumber: Int,
    val takerSide: String,
    val takerTrickPoints: Int,
    val beloteSide: String? = null,
    val capotSide: String? = null,
    val takerPlayerId: String? = null
)

/** Every row of the database, table by table. */
data class BackupTables(
    val players: List<PlayerEntity>,
    val parties: List<PartyEntity>,
    val participants: List<ParticipantEntity>,
    val teams: List<TeamEntity>,
    val rounds: List<RoundEntity>,
    val roundScores: List<RoundScoreEntity>,
    val beloteRounds: List<BeloteRoundEntity>
)

const val BACKUP_FORMAT = 1

private val backupJson = Json { prettyPrint = true }

fun BackupFile.encode(): String = backupJson.encodeToString(BackupFile.serializer(), this)

/** Throws [kotlinx.serialization.SerializationException] or [IllegalArgumentException] when [text] is not a [BackupFile]. */
fun decodeBackupFile(text: String): BackupFile = backupJson.decodeFromString(BackupFile.serializer(), text)

/** Rows are nested under their Partie; a row of a Partie missing from [BackupTables.parties] fails. */
fun BackupTables.toBackupFile(createdAt: Long): BackupFile {
    val partyIds = parties.map { it.id }.toSet()
    fun <T> List<T>.byParty(partyId: (T) -> String): Map<String, List<T>> = groupBy(partyId).onEach { (id, _) ->
        check(id in partyIds) { "A row belongs to Partie $id, which does not exist" }
    }
    val participantsByParty = participants.byParty { it.partyId }
    val teamsByParty = teams.byParty { it.partyId }
    val roundsByParty = rounds.byParty { it.partyId }
    val beloteRoundsByParty = beloteRounds.byParty { it.partyId }
    val roundIds = rounds.map { it.id }.toSet()
    val scoresByRound = roundScores.groupBy { it.roundId }.onEach { (id, _) ->
        check(id in roundIds) { "A score belongs to Manche $id, which does not exist" }
    }

    return BackupFile(
        format = BACKUP_FORMAT,
        createdAt = createdAt,
        players = players.map { BackupPlayer(it.id, it.name) },
        parties = parties.map { party ->
            BackupParty(
                id = party.id,
                gameId = party.gameId,
                status = party.status,
                currentDealerIndex = party.currentDealerIndex,
                createdAt = party.createdAt,
                endRuleType = party.endRuleType,
                endRuleValue = party.endRuleValue,
                gameName = party.gameName,
                winDirection = party.winDirection,
                participants = participantsByParty[party.id].orEmpty().sortedBy { it.listPosition }.map {
                    BackupParticipant(it.id, it.playerId, it.name, it.listPosition, it.teamSide)
                },
                teams = teamsByParty[party.id].orEmpty().map { BackupTeam(it.side, it.name) },
                rounds = roundsByParty[party.id].orEmpty().sortedBy { it.roundNumber }.map { round ->
                    BackupRound(
                        id = round.id,
                        roundNumber = round.roundNumber,
                        suit = round.suit,
                        sevenHolderId = round.sevenHolderId,
                        scores = scoresByRound[round.id].orEmpty().map { BackupScore(it.participantId, it.points) }
                    )
                },
                beloteRounds = beloteRoundsByParty[party.id].orEmpty().sortedBy { it.roundNumber }.map {
                    BackupBeloteRound(
                        id = it.id,
                        roundNumber = it.roundNumber,
                        takerSide = it.takerSide,
                        takerTrickPoints = it.takerTrickPoints,
                        beloteSide = it.beloteSide,
                        capotSide = it.capotSide,
                        takerPlayerId = it.takerPlayerId
                    )
                }
            )
        }
    )
}

/** The rows [BackupFile] holds; the Joueur's name key is recomputed by [nameKey]. */
fun BackupFile.toTables(nameKey: (String) -> String): BackupTables = BackupTables(
    players = players.map { PlayerEntity(it.id, it.name, nameKey(it.name)) },
    parties = parties.map {
        PartyEntity(
            id = it.id,
            gameId = it.gameId,
            status = it.status,
            currentDealerIndex = it.currentDealerIndex,
            createdAt = it.createdAt,
            endRuleType = it.endRuleType,
            endRuleValue = it.endRuleValue,
            gameName = it.gameName,
            winDirection = it.winDirection
        )
    },
    participants = parties.flatMap { party ->
        party.participants.map { ParticipantEntity(it.id, party.id, it.name, it.listPosition, it.teamSide, it.playerId) }
    },
    teams = parties.flatMap { party -> party.teams.map { TeamEntity(party.id, it.side, it.name) } },
    rounds = parties.flatMap { party ->
        party.rounds.map { RoundEntity(it.id, party.id, it.roundNumber, it.suit, it.sevenHolderId) }
    },
    roundScores = parties.flatMap { party ->
        party.rounds.flatMap { round -> round.scores.map { RoundScoreEntity(round.id, it.participantId, it.points) } }
    },
    beloteRounds = parties.flatMap { party ->
        party.beloteRounds.map {
            BeloteRoundEntity(
                id = it.id,
                partyId = party.id,
                roundNumber = it.roundNumber,
                takerSide = it.takerSide,
                takerTrickPoints = it.takerTrickPoints,
                beloteSide = it.beloteSide,
                capotSide = it.capotSide,
                takerPlayerId = it.takerPlayerId
            )
        }
    }
)

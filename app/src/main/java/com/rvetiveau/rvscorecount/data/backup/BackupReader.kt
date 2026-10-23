package com.rvetiveau.rvscorecount.data.backup

import com.rvetiveau.rvscorecount.data.entity.toDomain
import com.rvetiveau.rvscorecount.domain.engine.GameIds
import com.rvetiveau.rvscorecount.domain.model.playerNameKey
import java.util.UUID
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull

/** What a text holds once read as a backup file (Story 12.2). */
sealed interface BackupFileReading {
    data class Valid(val file: BackupFile, val tables: BackupTables) : BackupFileReading
    /** Not JSON, or not shaped like a [BackupFile]. */
    data object NotABackup : BackupFileReading
    /** Written by a newer version of the app, with a [BackupFile.format] above [BACKUP_FORMAT]. */
    data object NewerFormat : BackupFileReading
    /** A backup whose rows cannot all be written back: invalid value, duplicate or broken reference. */
    data object Damaged : BackupFileReading
}

/**
 * Reads [text] in full before anything is written. The format is read first: a newer file may hold
 * fields this version does not know, and must be refused as newer rather than as not a backup.
 */
fun readBackupFile(text: String): BackupFileReading {
    val format = try {
        ((Json.parseToJsonElement(text) as? JsonObject)?.get("format") as? JsonPrimitive)?.intOrNull
    } catch (_: SerializationException) {
        null
    } ?: return BackupFileReading.NotABackup
    if (format > BACKUP_FORMAT) return BackupFileReading.NewerFormat
    if (format < 1) return BackupFileReading.NotABackup

    val file = try {
        decodeBackupFile(text)
    } catch (_: SerializationException) {
        return BackupFileReading.NotABackup
    } catch (_: IllegalArgumentException) {
        return BackupFileReading.NotABackup
    }
    val tables = file.toTables(::playerNameKey)
    return try {
        tables.checkConsistent()
        BackupFileReading.Valid(file, tables)
    } catch (_: IllegalStateException) {
        BackupFileReading.Damaged
    } catch (_: IllegalArgumentException) {
        BackupFileReading.Damaged
    }
}

/**
 * Fails on any row Room would refuse or the app could not read back: values go through the Room
 * mappings, which reject unknown enum names and invalid ids. Each row's Partie is given by the file's
 * nesting; each kind of Manche must belong to a Partie of its Jeu, and references between Participants,
 * Manches and Joueurs must stay within one Partie.
 */
internal fun BackupTables.checkConsistent() {
    players.forEach { player ->
        UUID.fromString(player.id)
        check(player.name.isNotBlank()) { "Joueur ${player.id} has no name" }
    }
    checkUnique(players.map { it.id }, "Joueur")
    checkUnique(players.map { it.nameKey }, "Joueur name")
    val playerIds = players.map { it.id }.toSet()

    parties.forEach { party ->
        party.toDomain()
        check(party.gameId in GameIds.STORED) { "Partie ${party.id} has an unknown game ${party.gameId}" }
    }
    checkUnique(parties.map { it.id }, "Partie")
    val gameByParty = parties.associate { it.id to it.gameId }

    participants.forEach { participant ->
        participant.toDomain()
        check(participant.playerId in playerIds) { "Participant ${participant.id} has no Joueur" }
    }
    checkUnique(participants.map { it.id }, "Participant")
    checkUnique(participants.map { it.partyId to it.listPosition }, "seat")
    val partyByParticipant = participants.associate { it.id to it.partyId }

    teams.forEach { it.toDomain() }
    checkUnique(teams.map { it.partyId to it.side }, "Équipe")

    rounds.forEach { round ->
        UUID.fromString(round.id)
        when (gameByParty[round.partyId]) {
            GameIds.FREE -> check(round.suit == null) { "Manche ${round.id} of a Jeu libre has a suit" }
            GameIds.PAPAYOO -> round.toDomain()
            else -> error("Manche ${round.id} belongs to a Partie without score rows")
        }
        round.sevenHolderId?.let { holder ->
            check(partyByParticipant[holder] == round.partyId) { "Manche ${round.id} has a 7 holder of another Partie" }
        }
    }
    checkUnique(rounds.map { it.id }, "Manche")
    checkUnique(rounds.map { it.partyId to it.roundNumber }, "Manche number")
    val partyByRound = rounds.associate { it.id to it.partyId }

    roundScores.forEach { score ->
        score.toDomain()
        check(partyByParticipant[score.participantId] == partyByRound[score.roundId]) {
            "Score of Manche ${score.roundId} belongs to a Participant of another Partie"
        }
    }
    checkUnique(roundScores.map { it.roundId to it.participantId }, "score")

    beloteRounds.forEach { round ->
        check(gameByParty[round.partyId] == GameIds.BELOTE) { "Manche ${round.id} of Belote belongs to another Jeu" }
        round.toDomain()
        round.takerPlayerId?.let { check(it in playerIds) { "Manche ${round.id} has an unknown taker" } }
    }
    checkUnique(beloteRounds.map { it.id }, "Manche de Belote")
    checkUnique(beloteRounds.map { it.partyId to it.roundNumber }, "Manche de Belote number")

    tarotRounds.forEach { round ->
        check(gameByParty[round.partyId] == GameIds.TAROT) { "Manche ${round.id} of Tarot belongs to another Jeu" }
        val input = round.toDomain().input
        check(input.attackBouts in 0..TAROT_BOUTS) { "Manche ${round.id} has ${input.attackBouts} bouts" }
        check(input.attackPoints in 0..TAROT_POINTS) { "Manche ${round.id} has ${input.attackPoints} points" }
        check(partyByParticipant[round.takerId] == round.partyId) { "Manche ${round.id} has a taker of another Partie" }
        round.partnerId?.let { partner ->
            check(partner != round.takerId) { "Manche ${round.id} has the taker as partner" }
            check(partyByParticipant[partner] == round.partyId) { "Manche ${round.id} has a partner of another Partie" }
        }
    }
    checkUnique(tarotRounds.map { it.id }, "Manche de Tarot")
    checkUnique(tarotRounds.map { it.partyId to it.roundNumber }, "Manche de Tarot number")
}

// The Tarot's bounds (`games/tarot.md`); `data` does not depend on the engines (AD-1).
private const val TAROT_BOUTS = 3
private const val TAROT_POINTS = 91

private fun checkUnique(keys: List<Any>, what: String) {
    val duplicate = keys.groupingBy { it }.eachCount().entries.firstOrNull { it.value > 1 }
    check(duplicate == null) { "Duplicate $what ${duplicate?.key}" }
}

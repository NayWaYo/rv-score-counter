package com.rvetiveau.rvscorecount.data.backup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

/** A backup is read and checked in full before a restore (Story 12.2); anything doubtful is refused. */
class BackupReaderTest {

    private fun id() = UUID.randomUUID().toString()

    private val alice = BackupPlayer(id(), "Alice")
    private val bob = BackupPlayer(id(), "Bob")
    private val chloe = BackupPlayer(id(), "Chloé")
    private val david = BackupPlayer(id(), "David")

    private fun seat(player: BackupPlayer, position: Int, teamSide: String? = null) =
        BackupParticipant(id(), player.id, player.name, position, teamSide)

    private val papayooSeats = listOf(seat(alice, 0), seat(bob, 1), seat(chloe, 2))
    private val papayoo = BackupParty(
        id = id(), gameId = "papayoo", status = "IN_PROGRESS", currentDealerIndex = 1, createdAt = 1_000,
        participants = papayooSeats,
        rounds = listOf(
            BackupRound(id(), 1, "SPADE", papayooSeats[1].id, papayooSeats.map { BackupScore(it.id, 50) }),
            BackupRound(id(), 2, "DIAMOND", null, papayooSeats.map { BackupScore(it.id, 0) })
        )
    )

    private val belote = BackupParty(
        id = id(), gameId = "belote", status = "FINISHED", currentDealerIndex = 0, createdAt = 2_000,
        endRuleType = "TARGET_SCORE", endRuleValue = 1000,
        participants = listOf(seat(alice, 0, "FIRST"), seat(bob, 1, "SECOND"), seat(chloe, 2, "FIRST"), seat(david, 3, "SECOND")),
        teams = listOf(BackupTeam("FIRST", "Nous"), BackupTeam("SECOND", "Eux")),
        beloteRounds = listOf(
            BackupBeloteRound(id(), 1, "FIRST", 100, beloteSide = "SECOND", takerPlayerId = alice.id),
            BackupBeloteRound(id(), 2, "SECOND", 162, capotSide = "SECOND")
        )
    )

    private val freeSeats = listOf(seat(alice, 0), seat(david, 1))
    private val free = BackupParty(
        id = id(), gameId = "free", status = "FINISHED", currentDealerIndex = 0, createdAt = 3_000,
        endRuleType = "ROUND_COUNT", endRuleValue = 1, gameName = "Skyjo", winDirection = "LOWEST_WINS",
        participants = freeSeats,
        rounds = listOf(BackupRound(id(), 1, null, null, listOf(BackupScore(freeSeats[0].id, -12), BackupScore(freeSeats[1].id, 7))))
    )

    private val valid = BackupFile(BACKUP_FORMAT, 1_761_000_000_000, listOf(alice, bob, chloe, david), listOf(papayoo, belote, free))

    private fun read(file: BackupFile) = readBackupFile(file.encode())

    private fun assertDamaged(file: BackupFile) = assertEquals(BackupFileReading.Damaged, read(file))

    private fun BackupFile.withParty(party: BackupParty) = copy(parties = parties.map { if (it.id == party.id) party else it })

    @Test
    fun `a backup of every Jeu is valid and gives back its rows`() {
        val reading = read(valid)
        assertTrue(reading is BackupFileReading.Valid)
        reading as BackupFileReading.Valid
        assertEquals(valid, reading.file)
        assertEquals(valid.toTables { it.trim().lowercase() }, reading.tables)
    }

    @Test
    fun `each Jeu alone, an empty backup and a Joueur without Partie are valid`() {
        listOf(papayoo, belote, free).forEach { party ->
            assertTrue(read(valid.copy(parties = listOf(party))) is BackupFileReading.Valid)
        }
        assertTrue(read(BackupFile(BACKUP_FORMAT, 0, emptyList(), emptyList())) is BackupFileReading.Valid)
    }

    @Test
    fun `what is not a backup is refused as such`() {
        listOf(
            "",
            "hello",
            "[1, 2]",
            """{"name": "Skyjo"}""",
            """{"format": "un"}""",
            """{"format": 0, "createdAt": 1, "players": [], "parties": []}""",
            """{"format": 1, "createdAt": 1}""",
            """{"format": 1, "createdAt": 1, "players": [], "parties": [], "theme": "dark"}"""
        ).forEach { text -> assertEquals(text, BackupFileReading.NotABackup, readBackupFile(text)) }
    }

    @Test
    fun `a newer format is refused as newer, whatever it holds`() {
        assertEquals(BackupFileReading.NewerFormat, readBackupFile("""{"format": 2, "something": [1]}"""))
        assertEquals(BackupFileReading.NewerFormat, read(valid.copy(format = BACKUP_FORMAT + 1)))
    }

    @Test
    fun `invalid ids and values are refused`() {
        assertDamaged(valid.copy(players = valid.players + BackupPlayer("not-a-uuid", "Eve")))
        assertDamaged(valid.copy(players = valid.players + BackupPlayer(id(), "  ")))
        assertDamaged(valid.withParty(papayoo.copy(status = "PAUSED")))
        assertDamaged(valid.withParty(papayoo.copy(gameId = "tarot")))
        assertDamaged(valid.withParty(belote.copy(endRuleType = "TARGET_SCORE", endRuleValue = null)))
        assertDamaged(valid.withParty(belote.copy(endRuleType = "TIME")))
        assertDamaged(valid.withParty(free.copy(winDirection = "SIDEWAYS")))
        assertDamaged(valid.withParty(belote.copy(teams = listOf(BackupTeam("THIRD", "Eux")))))
        assertDamaged(valid.withParty(papayoo.copy(rounds = listOf(papayoo.rounds[0].copy(suit = "STAR")))))
        assertDamaged(valid.withParty(papayoo.copy(rounds = listOf(papayoo.rounds[0].copy(suit = null)))))
        assertDamaged(valid.withParty(free.copy(rounds = listOf(free.rounds[0].copy(suit = "HEART")))))
        assertDamaged(valid.withParty(belote.copy(beloteRounds = listOf(belote.beloteRounds[0].copy(takerSide = "NONE")))))
        assertDamaged(valid.withParty(papayoo.copy(participants = papayoo.participants + seat(david, 3, teamSide = "BOTH"))))
    }

    @Test
    fun `duplicates are refused`() {
        assertDamaged(valid.copy(players = valid.players + alice))
        assertDamaged(valid.copy(players = valid.players + BackupPlayer(id(), " ALICE ")))
        assertDamaged(valid.copy(parties = valid.parties + papayoo))
        assertDamaged(valid.withParty(papayoo.copy(participants = papayoo.participants + papayoo.participants[0].copy(listPosition = 9))))
        assertDamaged(valid.withParty(papayoo.copy(participants = papayoo.participants + seat(david, 0))))
        assertDamaged(valid.withParty(papayoo.copy(rounds = papayoo.rounds + papayoo.rounds[0].copy(id = id()))))
        assertDamaged(
            valid.withParty(papayoo.copy(rounds = listOf(papayoo.rounds[0].copy(scores = papayoo.rounds[0].scores + papayoo.rounds[0].scores[0]))))
        )
        assertDamaged(valid.withParty(belote.copy(teams = belote.teams + belote.teams[0])))
        assertDamaged(valid.withParty(belote.copy(beloteRounds = belote.beloteRounds + belote.beloteRounds[0].copy(id = id()))))
    }

    @Test
    fun `references to a missing Joueur, Participant or another Partie are refused`() {
        assertDamaged(valid.copy(players = valid.players - david))
        assertDamaged(valid.withParty(papayoo.copy(rounds = listOf(papayoo.rounds[0].copy(sevenHolderId = id())))))
        assertDamaged(valid.withParty(papayoo.copy(rounds = listOf(papayoo.rounds[0].copy(sevenHolderId = freeSeats[0].id)))))
        assertDamaged(
            valid.withParty(papayoo.copy(rounds = listOf(papayoo.rounds[0].copy(scores = listOf(BackupScore(freeSeats[0].id, 10))))))
        )
        assertDamaged(valid.withParty(belote.copy(beloteRounds = listOf(belote.beloteRounds[0].copy(takerPlayerId = id())))))
    }
}

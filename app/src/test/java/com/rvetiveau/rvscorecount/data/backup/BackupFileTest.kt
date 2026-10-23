package com.rvetiveau.rvscorecount.data.backup

import com.rvetiveau.rvscorecount.data.entity.BeloteRoundEntity
import com.rvetiveau.rvscorecount.data.entity.ParticipantEntity
import com.rvetiveau.rvscorecount.data.entity.PartyEntity
import com.rvetiveau.rvscorecount.data.entity.PlayerEntity
import com.rvetiveau.rvscorecount.data.entity.RoundEntity
import com.rvetiveau.rvscorecount.data.entity.RoundScoreEntity
import com.rvetiveau.rvscorecount.data.entity.TarotRoundEntity
import com.rvetiveau.rvscorecount.data.entity.TeamEntity
import com.rvetiveau.rvscorecount.domain.model.playerNameKey
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/** The backup file (Story 12.1) holds every row of every Jeu, and gives them back unchanged. */
class BackupFileTest {

    private val alice = PlayerEntity("p-alice", "Alice", "alice")
    private val bob = PlayerEntity("p-bob", "Bob", "bob")
    private val chloe = PlayerEntity("p-chloe", "Chloé", "chloé")
    private val david = PlayerEntity("p-david", "David", "david")
    private val unseated = PlayerEntity("p-eve", "Eve", "eve")

    private fun seat(id: String, partyId: String, player: PlayerEntity, position: Int, teamSide: String? = null) =
        ParticipantEntity(id, partyId, player.name, position, teamSide, player.id)

    /** A Papayoo Partie in progress: Manches with and without a 7 holder. */
    private val papayoo = BackupTables(
        players = listOf(alice, bob, chloe, unseated),
        parties = listOf(PartyEntity("papayoo-1", "papayoo", "IN_PROGRESS", 2, 1_000)),
        participants = listOf(
            seat("pa-a", "papayoo-1", alice, 0),
            seat("pa-b", "papayoo-1", bob, 1),
            seat("pa-c", "papayoo-1", chloe, 2)
        ),
        teams = emptyList(),
        rounds = listOf(
            RoundEntity("pr-1", "papayoo-1", 1, "SPADE", sevenHolderId = "pa-b"),
            RoundEntity("pr-2", "papayoo-1", 2, "HEART", sevenHolderId = null)
        ),
        roundScores = listOf(
            RoundScoreEntity("pr-1", "pa-a", 10), RoundScoreEntity("pr-1", "pa-b", 200), RoundScoreEntity("pr-1", "pa-c", 40),
            RoundScoreEntity("pr-2", "pa-a", 0), RoundScoreEntity("pr-2", "pa-b", 150), RoundScoreEntity("pr-2", "pa-c", 100)
        ),
        beloteRounds = emptyList(),
        tarotRounds = emptyList()
    )

    /** A finished Belote Partie to a target score, with and without belote, capot or taker player. */
    private val belote = BackupTables(
        players = listOf(alice, bob, chloe, david),
        parties = listOf(PartyEntity("belote-1", "belote", "FINISHED", 1, 2_000, "TARGET_SCORE", 1000)),
        participants = listOf(
            seat("be-a", "belote-1", alice, 0, "FIRST"),
            seat("be-b", "belote-1", bob, 1, "SECOND"),
            seat("be-c", "belote-1", chloe, 2, "FIRST"),
            seat("be-d", "belote-1", david, 3, "SECOND")
        ),
        teams = listOf(TeamEntity("belote-1", "FIRST", "Nous"), TeamEntity("belote-1", "SECOND", "Eux")),
        rounds = emptyList(),
        roundScores = emptyList(),
        beloteRounds = listOf(
            BeloteRoundEntity("br-1", "belote-1", 1, "FIRST", 100, beloteSide = "SECOND", capotSide = null, takerPlayerId = "p-alice"),
            BeloteRoundEntity("br-2", "belote-1", 2, "SECOND", 162, beloteSide = null, capotSide = "SECOND", takerPlayerId = null)
        ),
        tarotRounds = emptyList()
    )

    /** A Jeu libre Partie: no 7, negative scores, its own name and win direction. */
    private val free = BackupTables(
        players = listOf(alice, bob),
        parties = listOf(
            PartyEntity("free-1", "free", "FINISHED", 0, 3_000, "ROUND_COUNT", 2, gameName = "Skyjo", winDirection = "LOWEST_WINS")
        ),
        participants = listOf(seat("fr-a", "free-1", alice, 0), seat("fr-b", "free-1", bob, 1)),
        teams = emptyList(),
        rounds = listOf(RoundEntity("fr-1", "free-1", 1, suit = null), RoundEntity("fr-2", "free-1", 2, suit = null)),
        roundScores = listOf(
            RoundScoreEntity("fr-1", "fr-a", -12), RoundScoreEntity("fr-1", "fr-b", 30),
            RoundScoreEntity("fr-2", "fr-a", 0), RoundScoreEntity("fr-2", "fr-b", -3)
        ),
        beloteRounds = emptyList(),
        tarotRounds = emptyList()
    )

    /** A Tarot Partie at 5 (format 2): with and without partner and every bonus. */
    private val tarot = BackupTables(
        players = listOf(alice, bob, chloe, david, unseated),
        parties = listOf(PartyEntity("tarot-1", "tarot", "IN_PROGRESS", 2, 4_000, "ROUND_COUNT", 10)),
        participants = listOf(
            seat("ta-a", "tarot-1", alice, 0), seat("ta-b", "tarot-1", bob, 1), seat("ta-c", "tarot-1", chloe, 2),
            seat("ta-d", "tarot-1", david, 3), seat("ta-e", "tarot-1", unseated, 4)
        ),
        teams = emptyList(),
        rounds = emptyList(),
        roundScores = emptyList(),
        beloteRounds = emptyList(),
        tarotRounds = listOf(
            TarotRoundEntity("tr-1", "tarot-1", 1, "ta-a", "GARDE", 2, 50, "ta-c", "ATTACK", "SIMPLE", "DOUBLE", "UNANNOUNCED_MADE"),
            TarotRoundEntity("tr-2", "tarot-1", 2, "ta-d", "PRISE", 0, 40, null, null, null, null, null)
        )
    )

    private fun roundTrip(tables: BackupTables): BackupTables =
        decodeBackupFile(tables.toBackupFile(createdAt = 99).encode()).toTables(::playerNameKey)

    private fun assertSameRows(expected: BackupTables, actual: BackupTables) {
        assertEquals(expected.players.toSet(), actual.players.toSet())
        assertEquals(expected.parties.toSet(), actual.parties.toSet())
        assertEquals(expected.participants.toSet(), actual.participants.toSet())
        assertEquals(expected.teams.toSet(), actual.teams.toSet())
        assertEquals(expected.rounds.toSet(), actual.rounds.toSet())
        assertEquals(expected.roundScores.toSet(), actual.roundScores.toSet())
        assertEquals(expected.beloteRounds.toSet(), actual.beloteRounds.toSet())
        assertEquals(expected.tarotRounds.toSet(), actual.tarotRounds.toSet())
    }

    @Test
    fun `a Papayoo Partie comes back unchanged`() = assertSameRows(papayoo, roundTrip(papayoo))

    @Test
    fun `a Belote Partie comes back unchanged`() = assertSameRows(belote, roundTrip(belote))

    @Test
    fun `a Jeu libre Partie comes back unchanged`() = assertSameRows(free, roundTrip(free))

    @Test
    fun `a Tarot Partie comes back unchanged`() = assertSameRows(tarot, roundTrip(tarot))

    @Test
    fun `every Jeu together, and a Joueur without Partie, come back unchanged`() {
        val all = BackupTables(
            players = (papayoo.players + belote.players + free.players + tarot.players).distinct(),
            parties = papayoo.parties + belote.parties + free.parties + tarot.parties,
            participants = papayoo.participants + belote.participants + free.participants + tarot.participants,
            teams = belote.teams,
            rounds = papayoo.rounds + free.rounds,
            roundScores = papayoo.roundScores + free.roundScores,
            beloteRounds = belote.beloteRounds,
            tarotRounds = tarot.tarotRounds
        )
        assertSameRows(all, roundTrip(all))
    }

    @Test
    fun `an empty database gives an empty file`() {
        val empty = BackupTables(emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList())
        val file = empty.toBackupFile(createdAt = 5)
        assertEquals(BackupFile(BACKUP_FORMAT, 5, emptyList(), emptyList()), file)
        assertSameRows(empty, roundTrip(empty))
    }

    @Test
    fun `the file names its format and date, and nests each Partie's rows in play order`() {
        val json = Json.parseToJsonElement(papayoo.toBackupFile(createdAt = 1_761_000_000_000).encode()).jsonObject

        assertEquals(2, json.getValue("format").jsonPrimitive.int)
        assertEquals(1_761_000_000_000, json.getValue("createdAt").jsonPrimitive.long)
        val party = json.getValue("parties").jsonArray.single().jsonObject
        assertEquals(listOf("pa-a", "pa-b", "pa-c"), party.getValue("participants").jsonArray.map { it.jsonObject.getValue("id").jsonPrimitive.content })
        val rounds = party.getValue("rounds").jsonArray
        assertEquals(listOf(1, 2), rounds.map { it.jsonObject.getValue("roundNumber").jsonPrimitive.int })
        assertEquals(3, rounds.first().jsonObject.getValue("scores").jsonArray.size)
    }

    @Test
    fun `a row of a missing Partie or Manche is refused`() {
        assertThrows(IllegalStateException::class.java) {
            papayoo.copy(parties = emptyList()).toBackupFile(createdAt = 0)
        }
        assertThrows(IllegalStateException::class.java) {
            papayoo.copy(roundScores = papayoo.roundScores + RoundScoreEntity("pr-9", "pa-a", 0)).toBackupFile(createdAt = 0)
        }
    }

    @Test
    fun `the Joueur's name key is recomputed on the way back`() {
        val tables = papayoo.toBackupFile(createdAt = 0).toTables { "key:$it" }
        assertTrue(tables.players.all { it.nameKey == "key:${it.name}" })
    }
}

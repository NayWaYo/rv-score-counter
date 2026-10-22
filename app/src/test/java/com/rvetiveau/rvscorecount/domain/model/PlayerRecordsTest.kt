package com.rvetiveau.rvscorecount.domain.model

import com.rvetiveau.rvscorecount.domain.usecase.assembleBeloteParty
import com.rvetiveau.rvscorecount.game.belote.BeloteEngine
import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlayerRecordsTest {

    // --- Papayoo ---

    private class PapayooParty(createdAt: Long, names: List<String>) {
        val party = Party(UUID.randomUUID(), "papayoo", PartyState.FINISHED, 0, createdAt)
        val participants = names.mapIndexed { index, name -> Participant(UUID.randomUUID(), party.id, name, index) }
        val rounds = mutableListOf<Round>()
        val scores = mutableListOf<RoundScore>()

        fun seat(name: String) = participants.single { it.name == name }.id

        /** One Manche: [points] by name, and the holder of the 7 when recorded. */
        fun round(vararg points: Pair<String, Int>, sevenHolder: String? = null) {
            val round = Round(UUID.randomUUID(), party.id, rounds.size + 1, CardSuit.HEART, sevenHolder?.let(::seat))
            rounds += round
            points.forEach { (name, value) -> scores += RoundScore(round.id, seat(name), value) }
        }

        fun play(name: String) = PapayooPlay(party, participants, rounds, scores, setOf(seat(name)))
    }

    @Test
    fun `papayoo counts wins with ties, the 7s where recorded, zeros, the oldest worst round and full rounds`() {
        val first = PapayooParty(createdAt = 10, listOf("Alice", "Bob", "Chloé")).apply {
            round("Alice" to 0, "Bob" to 100, "Chloé" to 150, sevenHolder = "Chloé")
            round("Alice" to 150, "Bob" to 50, "Chloé" to 50, sevenHolder = "Alice")
        }
        val second = PapayooParty(createdAt = 20, listOf("Alice", "Bob", "Chloé")).apply {
            round("Alice" to 150, "Bob" to 50, "Chloé" to 50)
            round("Alice" to 0, "Bob" to 250, "Chloé" to 0, sevenHolder = "Bob")
        }

        val records = checkNotNull(papayooRecords(listOf(second.play("Alice"), first.play("Alice")), fullRoundPoints = 250))

        // First: Alice 150 = Bob 150 < Chloé 200, a tied win. Second: Chloé wins with 50, Alice has 150.
        assertEquals(WinRecord(wins = 1, parties = 2), records.wins)
        assertEquals(4, records.roundsPlayed)
        assertEquals(1, records.sevensTaken)
        assertEquals(3, records.sevensRecorded)
        assertEquals(2, records.zeroRounds)
        assertEquals(WorstRound(150, first.party.id, 10), records.worstRound)
        assertEquals(0, records.fullRounds)
        assertEquals(0.5, records.zeroRate)
        assertEquals(1.0 / 3, records.sevenRate)

        val bob = checkNotNull(papayooRecords(listOf(first.play("Bob"), second.play("Bob")), fullRoundPoints = 250))
        assertEquals(1, bob.fullRounds)
        assertEquals(WorstRound(250, second.party.id, 20), bob.worstRound)
    }

    @Test
    fun `without any recorded 7 there is no 7 rate, and without any partie no record`() {
        val party = PapayooParty(createdAt = 10, listOf("Alice", "Bob", "Chloé")).apply {
            round("Alice" to 100, "Bob" to 100, "Chloé" to 50)
        }

        val records = checkNotNull(papayooRecords(listOf(party.play("Alice")), fullRoundPoints = 250))

        assertEquals(0, records.sevensTaken)
        assertNull(records.sevenRate)
        assertEquals(WinRecord(0, 1), records.wins)
        assertNull(papayooRecords(emptyList(), fullRoundPoints = 250))
    }

    // --- Belote ---

    private val engine = BeloteEngine()
    private val playerIds = mutableMapOf<String, UUID>()
    private fun playerId(name: String) = playerIds.getOrPut(name) { UUID.randomUUID() }

    /**
     * A one-Manche Partie: [first] and [second] are the two Équipes, their first player taking with
     * [takerTrickPoints] (made above 81, « dedans » below), or nobody recorded as taker.
     */
    private fun belote(
        first: Pair<String, String>,
        second: Pair<String, String>,
        takerTrickPoints: Int,
        takerRecorded: Boolean = true,
        seatOf: String = "Alice"
    ): BelotePlay {
        val party = Party(UUID.randomUUID(), "belote", PartyState.FINISHED, 0, 0, BeloteEndRule.RoundCount(1))
        val seats = listOf(first.first to TeamSide.FIRST, second.first to TeamSide.SECOND, first.second to TeamSide.FIRST, second.second to TeamSide.SECOND)
        val players = seats.mapIndexed { index, (name, side) -> Participant(UUID.randomUUID(), party.id, name, index, side) }
        val taker = players.first()
        val round = BeloteRound(
            UUID.randomUUID(), party.id, 1, BeloteRoundInput(TeamSide.FIRST, takerTrickPoints),
            takerPlayerId = taker.id.takeIf { takerRecorded }
        )
        val state = assembleBeloteParty(
            party,
            listOf(Team(party.id, TeamSide.FIRST, "Nous"), Team(party.id, TeamSide.SECOND, "Eux")),
            players,
            listOf(round),
            engine
        )
        return BelotePlay(
            state = state,
            seat = players.single { it.name == seatOf }.id,
            playerIdBySeat = players.associate { it.id to playerId(it.name) }
        )
    }

    private fun nameOf(id: UUID) = playerIds.entries.single { it.value == id }.key

    @Test
    fun `belote counts team wins, takes and made contracts, and ranks teammates by win rate together`() {
        val plays = listOf(
            belote("Alice" to "Bob", "Chloé" to "David", takerTrickPoints = 100),
            belote("Alice" to "Bob", "Chloé" to "David", takerTrickPoints = 50),
            belote("Alice" to "Chloé", "Bob" to "David", takerTrickPoints = 100)
        )

        val records = checkNotNull(beloteRecords(plays, ::nameOf))

        assertEquals(WinRecord(wins = 2, parties = 3), records.wins)
        assertEquals(3, records.takes)
        assertEquals(2, records.made)
        assertEquals(1.0, records.takesPerParty, 0.0)
        assertEquals(TeammateRecord(playerId("Chloé"), "Chloé", WinRecord(1, 1)), records.bestTeammate)
        assertEquals(TeammateRecord(playerId("Bob"), "Bob", WinRecord(1, 2)), records.worstTeammate)
    }

    @Test
    fun `on a tie of win rate the teammate with more parties together is the best, the other the worst`() {
        val plays = listOf(
            belote("Alice" to "Bob", "Chloé" to "David", takerTrickPoints = 100),
            belote("Alice" to "Bob", "Chloé" to "David", takerTrickPoints = 100),
            belote("Alice" to "Chloé", "Bob" to "David", takerTrickPoints = 100)
        )

        val records = checkNotNull(beloteRecords(plays, ::nameOf))

        assertEquals("Bob", records.bestTeammate?.name)
        assertEquals("Chloé", records.worstTeammate?.name)
    }

    @Test
    fun `a single teammate is only the best, and unrecorded takers give no contract rate`() {
        val records = checkNotNull(
            beloteRecords(listOf(belote("Alice" to "Bob", "Chloé" to "David", takerTrickPoints = 100, takerRecorded = false)), ::nameOf)
        )

        assertEquals("Bob", records.bestTeammate?.name)
        assertNull(records.worstTeammate)
        assertEquals(0, records.takes)
        assertNull(records.contractRate)
        assertNull(beloteRecords(emptyList(), ::nameOf))
    }

    @Test
    fun `a defender's record counts the parties the taking team lost as wins`() {
        val records = checkNotNull(
            beloteRecords(listOf(belote("Bob" to "Chloé", "Alice" to "David", takerTrickPoints = 50)), ::nameOf)
        )

        assertEquals(WinRecord(1, 1), records.wins)
        assertEquals(0, records.takes)
        assertEquals(0.0, records.takesPerParty, 0.0)
        assertEquals("David", records.bestTeammate?.name)
    }
}

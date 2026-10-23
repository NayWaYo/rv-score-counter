package com.rvetiveau.rvscorecount.game.tarot

import com.rvetiveau.rvscorecount.domain.model.Chelem
import com.rvetiveau.rvscorecount.domain.model.EndRule
import com.rvetiveau.rvscorecount.domain.model.Participant
import com.rvetiveau.rvscorecount.domain.model.Poignee
import com.rvetiveau.rvscorecount.domain.model.TarotContract
import com.rvetiveau.rvscorecount.domain.model.TarotContract.GARDE
import com.rvetiveau.rvscorecount.domain.model.TarotContract.GARDE_CONTRE
import com.rvetiveau.rvscorecount.domain.model.TarotContract.GARDE_SANS
import com.rvetiveau.rvscorecount.domain.model.TarotContract.PRISE
import com.rvetiveau.rvscorecount.domain.model.TarotInputError
import com.rvetiveau.rvscorecount.domain.model.TarotPartyStatus
import com.rvetiveau.rvscorecount.domain.model.TarotRound
import com.rvetiveau.rvscorecount.domain.model.TarotRoundInput
import com.rvetiveau.rvscorecount.domain.model.TarotSide
import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/** FFT scoring of `games/tarot.md`; every expected value is worked out by hand in its comment. */
class TarotEngineTest {

    private val engine = TarotEngine()
    private val partyId = UUID.randomUUID()
    private val alice = Participant(UUID.randomUUID(), partyId, "Alice", 0)
    private val bob = Participant(UUID.randomUUID(), partyId, "Bob", 1)
    private val chloe = Participant(UUID.randomUUID(), partyId, "Chloé", 2)
    private val david = Participant(UUID.randomUUID(), partyId, "David", 3)
    private val eve = Participant(UUID.randomUUID(), partyId, "Eve", 4)
    private val three = listOf(alice, bob, chloe)
    private val four = listOf(alice, bob, chloe, david)
    private val five = listOf(alice, bob, chloe, david, eve)

    /** Alice takes. */
    private fun input(
        contract: TarotContract = PRISE,
        bouts: Int = 2,
        points: Int = 41,
        partner: Participant? = null,
        petitAuBout: TarotSide? = null,
        attackPoignee: Poignee? = null,
        defensePoignee: Poignee? = null,
        chelem: Chelem? = null
    ) = TarotRoundInput(alice.id, contract, bouts, points, partner?.id, petitAuBout, attackPoignee, defensePoignee, chelem)

    private fun perDefender(input: TarotRoundInput, players: List<Participant> = four) =
        engine.scoreRound(input, players).perDefender

    @Test
    fun `metadata matches the tarot`() {
        assertEquals("tarot", engine.gameId)
        assertEquals(3, engine.minPlayers)
        assertEquals(5, engine.maxPlayers)
        assertEquals(5, engine.partnerPlayerCount)
        assertEquals(91, engine.totalPoints)
        assertEquals(3, engine.maxBouts)
    }

    @Test
    fun `the bouts set the points to make`() {
        assertEquals(listOf(56, 51, 41, 36), (0..3).map(engine::requiredPoints))
        assertThrows(IllegalArgumentException::class.java) { engine.requiredPoints(4) }
        assertThrows(IllegalArgumentException::class.java) { engine.requiredPoints(-1) }
    }

    @Test
    fun `a contract is made from its threshold, equality included, and down below`() {
        // Garde, 2 bouts, 50 points: 9 over 41, (25 + 9) × 2 = 68.
        engine.scoreRound(input(GARDE, bouts = 2, points = 50), four).let {
            assertEquals(41, it.requiredPoints)
            assertTrue(it.made)
            assertEquals(68, it.perDefender)
        }
        // Exactly 36 with 3 bouts: made by 0, 25.
        engine.scoreRound(input(PRISE, bouts = 3, points = 36), four).let {
            assertTrue(it.made)
            assertEquals(25, it.perDefender)
        }
        // Prise, 1 bout, 45 points: 6 under 51, −(25 + 6) = −31.
        engine.scoreRound(input(PRISE, bouts = 1, points = 45), four).let {
            assertFalse(it.made)
            assertEquals(-31, it.perDefender)
        }
        // One point short with 0 bouts: −26.
        assertEquals(-26, perDefender(input(PRISE, bouts = 0, points = 55)))
    }

    @Test
    fun `each contract multiplies the contract`() {
        // 0 bouts, 60 points: 4 over 56, 29 × multiplier.
        assertEquals(
            listOf(29, 58, 116, 174),
            listOf(PRISE, GARDE, GARDE_SANS, GARDE_CONTRE).map { perDefender(input(it, bouts = 0, points = 60)) }
        )
        // And a fall: 0 bouts, 50 points, −31 × multiplier.
        assertEquals(
            listOf(-31, -62, -124, -186),
            listOf(PRISE, GARDE, GARDE_SANS, GARDE_CONTRE).map { perDefender(input(it, bouts = 0, points = 50)) }
        )
    }

    @Test
    fun `the petit au bout is multiplied and goes to the side that led it, made or down`() {
        // Garde made by 0: 50, ± 10 × 2.
        assertEquals(70, perDefender(input(GARDE, bouts = 2, points = 41, petitAuBout = TarotSide.ATTACK)))
        assertEquals(30, perDefender(input(GARDE, bouts = 2, points = 41, petitAuBout = TarotSide.DEFENSE)))
        // Prise down by 1 with 1 bout: −26 + 10 for the attack's petit au bout.
        assertEquals(-16, perDefender(input(PRISE, bouts = 1, points = 50, petitAuBout = TarotSide.ATTACK)))
        // Garde contre made by 0: 150 − 60.
        assertEquals(90, perDefender(input(GARDE_CONTRE, bouts = 2, points = 41, petitAuBout = TarotSide.DEFENSE)))
    }

    @Test
    fun `handfuls are not multiplied and go to the side that wins the contract`() {
        // Garde made by 0: 50 + 20.
        assertEquals(70, perDefender(input(GARDE, attackPoignee = Poignee.SIMPLE)))
        // Prise made by 4 with 3 bouts: 29 + 30, though the defense showed it.
        assertEquals(59, perDefender(input(PRISE, bouts = 3, points = 40, defensePoignee = Poignee.DOUBLE)))
        // Prise down by 6 with 0 bouts: −31 − 20, though the attack showed it.
        assertEquals(-51, perDefender(input(PRISE, bouts = 0, points = 50, attackPoignee = Poignee.SIMPLE)))
        // Both sides, made by 0: 25 + 40 + 20.
        assertEquals(85, perDefender(input(PRISE, attackPoignee = Poignee.TRIPLE, defensePoignee = Poignee.SIMPLE)))
    }

    @Test
    fun `a chelem is not multiplied`() {
        // Garde sans, 3 bouts, 91 points: (25 + 55) × 4 = 320.
        val slam = input(GARDE_SANS, bouts = 3, points = 91)
        assertEquals(720, perDefender(slam.copy(chelem = Chelem.ANNOUNCED_MADE)))
        assertEquals(520, perDefender(slam.copy(chelem = Chelem.UNANNOUNCED_MADE)))
        // Prise, 3 bouts, 80 points: 69 − 200.
        assertEquals(-131, perDefender(input(PRISE, bouts = 3, points = 80, chelem = Chelem.ANNOUNCED_FAILED)))
        // Prise, 0 bouts, 0 points: −81 − 200.
        assertEquals(-281, perDefender(input(PRISE, bouts = 0, points = 0, chelem = Chelem.DEFENSE_MADE)))
    }

    @Test
    fun `every bonus adds up`() {
        // Garde, 2 bouts, 50 points: 68 + 20 (petit) + 30 + 20 (handfuls) + 200 (chelem) = 338.
        val all = input(
            GARDE, bouts = 2, points = 50, petitAuBout = TarotSide.ATTACK,
            attackPoignee = Poignee.DOUBLE, defensePoignee = Poignee.SIMPLE, chelem = Chelem.UNANNOUNCED_MADE
        )
        assertEquals(338, perDefender(all))
    }

    @Test
    fun `the taker wins S from each defender, the partner taking one share`() {
        // S = 68 (Garde, 2 bouts, 50 points).
        val garde = input(GARDE, bouts = 2, points = 50)
        assertEquals(
            mapOf(alice.id to 136, bob.id to -68, chloe.id to -68),
            engine.scoreRound(garde, three).points
        )
        assertEquals(
            mapOf(alice.id to 204, bob.id to -68, chloe.id to -68, david.id to -68),
            engine.scoreRound(garde, four).points
        )
        assertEquals(
            mapOf(alice.id to 136, bob.id to -68, chloe.id to 68, david.id to -68, eve.id to -68),
            engine.scoreRound(garde.copy(partnerId = chloe.id), five).points
        )
        assertEquals(
            mapOf(alice.id to 272, bob.id to -68, chloe.id to -68, david.id to -68, eve.id to -68),
            engine.scoreRound(garde, five).points
        )
    }

    @Test
    fun `a fall costs the taker and the partner the same shares`() {
        // S = −31 (Prise, 1 bout, 45 points), at 5 with a partner.
        assertEquals(
            mapOf(alice.id to -62, bob.id to -31, chloe.id to 31, david.id to 31, eve.id to 31),
            engine.scoreRound(input(PRISE, bouts = 1, points = 45, partner = bob), five).points
        )
    }

    @Test
    fun `every Manche adds up to zero`() {
        val sides = listOf(null, TarotSide.ATTACK, TarotSide.DEFENSE)
        val handfuls = listOf(null) + Poignee.entries
        val slams = listOf(null) + Chelem.entries
        listOf(three, four, five).forEach { players ->
            val partners = if (players.size == 5) listOf(null, bob, eve) else listOf(null)
            TarotContract.entries.forEach { contract ->
                (0..3).forEach { bouts ->
                    (0..91 step 7).forEach { points ->
                        partners.forEach { partner ->
                            val bonus = (bouts + points + contract.ordinal) % 3
                            val round = input(
                                contract, bouts, points, partner,
                                petitAuBout = sides[bonus],
                                attackPoignee = handfuls[(points / 7) % handfuls.size],
                                defensePoignee = handfuls[(points / 13) % handfuls.size],
                                chelem = slams[(points / 11) % slams.size]
                            )
                            assertEquals(round.toString(), 0, engine.scoreRound(round, players).points.values.sum())
                        }
                    }
                }
            }
        }
    }

    @Test
    fun `an impossible Manche is refused, and never scored`() {
        val valid = input()
        assertNull(engine.inputError(valid, four))
        assertNull(engine.inputError(valid.copy(partnerId = bob.id), five))
        assertNull(engine.inputError(valid, five))

        val refused = listOf(
            Triple(valid, listOf(alice, bob), TarotInputError.PLAYER_COUNT),
            Triple(valid, five + Participant(UUID.randomUUID(), partyId, "Fred", 5), TarotInputError.PLAYER_COUNT),
            Triple(valid.copy(takerId = UUID.randomUUID()), four, TarotInputError.UNKNOWN_TAKER),
            Triple(valid.copy(attackBouts = 4), four, TarotInputError.BOUTS_OUT_OF_RANGE),
            Triple(valid.copy(attackBouts = -1), four, TarotInputError.BOUTS_OUT_OF_RANGE),
            Triple(valid.copy(attackPoints = 92), four, TarotInputError.POINTS_OUT_OF_RANGE),
            Triple(valid.copy(attackPoints = -1), four, TarotInputError.POINTS_OUT_OF_RANGE),
            Triple(valid.copy(partnerId = bob.id), four, TarotInputError.PARTNER_NOT_ALLOWED),
            Triple(valid.copy(partnerId = bob.id), three, TarotInputError.PARTNER_NOT_ALLOWED),
            Triple(valid.copy(partnerId = alice.id), five, TarotInputError.PARTNER_IS_TAKER),
            Triple(valid.copy(partnerId = UUID.randomUUID()), five, TarotInputError.UNKNOWN_PARTNER)
        )
        refused.forEach { (round, players, error) ->
            assertEquals(error, engine.inputError(round, players))
            assertThrows(IllegalArgumentException::class.java) { engine.scoreRound(round, players) }
        }
    }

    @Test
    fun `scoring is deterministic and keeps no state`() {
        val round = input(GARDE_SANS, bouts = 1, points = 63, petitAuBout = TarotSide.DEFENSE, attackPoignee = Poignee.SIMPLE)
        val first = engine.scoreRound(round, four)
        engine.scoreRound(input(PRISE, bouts = 0, points = 0), four)
        assertEquals(first, engine.scoreRound(round, four))
        assertEquals(first, TarotEngine().scoreRound(round, four))
    }

    private fun rounds(vararg inputs: TarotRoundInput) = inputs.mapIndexed { index, input ->
        TarotRound(UUID.randomUUID(), partyId, index + 1, input)
    }

    @Test
    fun `standings put the highest total first, ties sharing a rank in table order`() {
        // Alice +75 then Bob takes and falls (Prise, 0 bouts, 50: −31): Alice 75 + 31 = 106,
        // Bob −25 − 93 = −118, Chloé and David −25 + 31 = 6 each.
        val played = rounds(
            input(PRISE, bouts = 2, points = 41),
            TarotRoundInput(bob.id, PRISE, attackBouts = 0, attackPoints = 50)
        )
        assertEquals(
            listOf(Triple("Alice", 106, 1), Triple("Chloé", 6, 2), Triple("David", 6, 2), Triple("Bob", -118, 4)),
            engine.standings(four, played).map { Triple(it.participant.name, it.total, it.rank) }
        )
        assertEquals(listOf(0, 0, 0, 0), engine.standings(four, emptyList()).map { it.total })
        assertEquals(listOf(1, 1, 1, 1), engine.standings(four, emptyList()).map { it.rank })
    }

    @Test
    fun `a Manche of someone outside the Partie is an inconsistency`() {
        val stranger = TarotRoundInput(eve.id, PRISE, attackBouts = 0, attackPoints = 60)
        assertThrows(IllegalArgumentException::class.java) { engine.standings(four, rounds(stranger)) }
    }

    @Test
    fun `the Partie ends after the planned Manches, by hand otherwise`() {
        val played = rounds(input(), input())
        assertEquals(TarotPartyStatus.InProgress, engine.partyStatus(four, played, endRule = null))
        assertEquals(TarotPartyStatus.InProgress, engine.partyStatus(four, played.take(1), EndRule.RoundCount(2)))
        assertEquals(TarotPartyStatus.Finished(listOf(alice)), engine.partyStatus(four, played, EndRule.RoundCount(2)))
        assertThrows(IllegalArgumentException::class.java) { engine.partyStatus(four, played, EndRule.TargetScore(500)) }
    }

    @Test
    fun `equal leaders all win`() {
        // Alice then Bob each make a Prise by 0 at 3: +50 −25 = 25 each, Chloé −50.
        val played = rounds(input(), TarotRoundInput(bob.id, PRISE, attackBouts = 2, attackPoints = 41))
        assertEquals(TarotPartyStatus.Finished(listOf(alice, bob)), engine.partyStatus(three, played, EndRule.RoundCount(2)))
    }
}

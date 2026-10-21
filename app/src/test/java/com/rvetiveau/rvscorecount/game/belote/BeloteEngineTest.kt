package com.rvetiveau.rvscorecount.game.belote

import com.rvetiveau.rvscorecount.domain.model.BeloteEndRule
import com.rvetiveau.rvscorecount.domain.model.BeloteOutcome
import com.rvetiveau.rvscorecount.domain.model.BelotePartyStatus
import com.rvetiveau.rvscorecount.domain.model.BeloteRoundInput
import com.rvetiveau.rvscorecount.domain.model.TeamPoints
import com.rvetiveau.rvscorecount.domain.model.TeamSide.FIRST
import com.rvetiveau.rvscorecount.domain.model.TeamSide.SECOND
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class BeloteEngineTest {

    private val engine = BeloteEngine()

    @Test
    fun `metadata matches belote rules`() {
        assertEquals("belote", engine.gameId)
        assertEquals(162, engine.totalTrickPoints)
    }

    @Test
    fun `the other team has the rest of the 162 trick points`() {
        assertEquals(62, engine.otherTeamTrickPoints(100))
        assertEquals(162, engine.otherTeamTrickPoints(0))
        assertEquals(0, engine.otherTeamTrickPoints(162))
    }

    // Fait, dedans, litige without belote

    @Test
    fun `taker with 82 trick points makes the contract and each team scores its tricks`() {
        val result = engine.scoreRound(BeloteRoundInput(taker = FIRST, takerTrickPoints = 82))

        assertEquals(BeloteOutcome.MADE, result.outcome)
        assertEquals(TeamPoints(82, 80), result.points)
        assertEquals(0, result.reserveAfter)
    }

    @Test
    fun `taker with 80 trick points is down and the defense scores 162`() {
        val result = engine.scoreRound(BeloteRoundInput(taker = FIRST, takerTrickPoints = 80))

        assertEquals(BeloteOutcome.DOWN, result.outcome)
        assertEquals(TeamPoints(0, 162), result.points)
    }

    @Test
    fun `81 to 81 is a litige and the taker's tricks go to the reserve`() {
        val result = engine.scoreRound(BeloteRoundInput(taker = FIRST, takerTrickPoints = 81))

        assertEquals(BeloteOutcome.DISPUTED, result.outcome)
        assertEquals(TeamPoints(0, 81), result.points)
        assertEquals(0, result.reserveWon)
        assertEquals(81, result.reserveAfter)
    }

    @Test
    fun `the second team can take`() {
        val result = engine.scoreRound(BeloteRoundInput(taker = SECOND, takerTrickPoints = 100))

        assertEquals(BeloteOutcome.MADE, result.outcome)
        assertEquals(TeamPoints(62, 100), result.points)
    }

    // Belote-rebelote

    @Test
    fun `taker with the belote makes the contract from 72 trick points`() {
        val result = engine.scoreRound(BeloteRoundInput(FIRST, takerTrickPoints = 72, beloteTeam = FIRST))

        assertEquals(BeloteOutcome.MADE, result.outcome)
        assertEquals(TeamPoints(92, 90), result.points)
    }

    @Test
    fun `taker with the belote and 71 trick points is in litige and keeps the belote`() {
        val result = engine.scoreRound(BeloteRoundInput(FIRST, takerTrickPoints = 71, beloteTeam = FIRST))

        assertEquals(BeloteOutcome.DISPUTED, result.outcome)
        assertEquals(TeamPoints(20, 91), result.points)
        assertEquals(71, result.reserveAfter)
    }

    @Test
    fun `when the defense has the belote the taker needs 92 trick points`() {
        val made = engine.scoreRound(BeloteRoundInput(FIRST, takerTrickPoints = 92, beloteTeam = SECOND))
        val disputed = engine.scoreRound(BeloteRoundInput(FIRST, takerTrickPoints = 91, beloteTeam = SECOND))

        assertEquals(BeloteOutcome.MADE, made.outcome)
        assertEquals(TeamPoints(92, 90), made.points)
        assertEquals(BeloteOutcome.DISPUTED, disputed.outcome)
    }

    @Test
    fun `a taker down keeps its belote`() {
        val result = engine.scoreRound(BeloteRoundInput(FIRST, takerTrickPoints = 60, beloteTeam = FIRST))

        assertEquals(BeloteOutcome.DOWN, result.outcome)
        assertEquals(TeamPoints(20, 162), result.points)
    }

    @Test
    fun `the defense adds its belote to the 162 when the taker is down`() {
        val result = engine.scoreRound(BeloteRoundInput(FIRST, takerTrickPoints = 60, beloteTeam = SECOND))

        assertEquals(TeamPoints(0, 182), result.points)
    }

    // Capot

    @Test
    fun `a capot taker scores 252`() {
        val result = engine.scoreRound(BeloteRoundInput(FIRST, takerTrickPoints = 162, capotTeam = FIRST))

        assertEquals(BeloteOutcome.CAPOT, result.outcome)
        assertEquals(TeamPoints(252, 0), result.points)
    }

    @Test
    fun `a capot team adds its belote and the other keeps its own`() {
        val takerBelote = engine.scoreRound(BeloteRoundInput(FIRST, 162, beloteTeam = FIRST, capotTeam = FIRST))
        val defenseBelote = engine.scoreRound(BeloteRoundInput(FIRST, 162, beloteTeam = SECOND, capotTeam = FIRST))

        assertEquals(TeamPoints(272, 0), takerBelote.points)
        assertEquals(TeamPoints(252, 20), defenseBelote.points)
    }

    @Test
    fun `a taker capot by the defense scores only its belote`() {
        val result = engine.scoreRound(BeloteRoundInput(FIRST, takerTrickPoints = 0, beloteTeam = FIRST, capotTeam = SECOND))

        assertEquals(BeloteOutcome.CAPOT, result.outcome)
        assertEquals(TeamPoints(20, 252), result.points)
    }

    @Test
    fun `162 trick points without a capot are scored as tricks`() {
        val result = engine.scoreRound(BeloteRoundInput(FIRST, takerTrickPoints = 162))

        assertEquals(BeloteOutcome.MADE, result.outcome)
        assertEquals(TeamPoints(162, 0), result.points)
    }

    // Litige reserve

    @Test
    fun `the reserve goes to the taker who makes the next contract`() {
        val results = engine.scoreParty(
            listOf(
                BeloteRoundInput(FIRST, takerTrickPoints = 81),
                BeloteRoundInput(SECOND, takerTrickPoints = 100)
            )
        )

        assertEquals(TeamPoints(62, 181), results[1].points)
        assertEquals(81, results[1].reserveWon)
        assertEquals(0, results[1].reserveAfter)
    }

    @Test
    fun `the reserve goes to the defense when the next taker is down`() {
        val results = engine.scoreParty(
            listOf(
                BeloteRoundInput(FIRST, takerTrickPoints = 81),
                BeloteRoundInput(FIRST, takerTrickPoints = 50)
            )
        )

        assertEquals(TeamPoints(0, 243), results[1].points)
    }

    @Test
    fun `the reserve goes to the capot team`() {
        val results = engine.scoreParty(
            listOf(
                BeloteRoundInput(FIRST, takerTrickPoints = 81),
                BeloteRoundInput(FIRST, takerTrickPoints = 0, capotTeam = SECOND)
            )
        )

        assertEquals(TeamPoints(0, 333), results[1].points)
    }

    @Test
    fun `consecutive litiges add up in the reserve`() {
        val results = engine.scoreParty(
            listOf(
                BeloteRoundInput(FIRST, takerTrickPoints = 81),
                BeloteRoundInput(SECOND, takerTrickPoints = 71, beloteTeam = SECOND),
                BeloteRoundInput(FIRST, takerTrickPoints = 100)
            )
        )

        assertEquals(81, results[0].reserveAfter)
        assertEquals(TeamPoints(91, 20), results[1].points)
        assertEquals(152, results[1].reserveAfter)
        assertEquals(TeamPoints(252, 62), results[2].points)
        assertEquals(152, results[2].reserveWon)
    }

    @Test
    fun `scoring a party of no round gives no result`() {
        assertEquals(emptyList<Any>(), engine.scoreParty(emptyList()))
    }

    @Test
    fun `scoring is deterministic`() {
        val rounds = listOf(BeloteRoundInput(FIRST, 81), BeloteRoundInput(SECOND, 120, beloteTeam = FIRST))

        assertEquals(engine.scoreParty(rounds), BeloteEngine().scoreParty(rounds))
        assertEquals(engine.scoreParty(rounds), engine.scoreParty(rounds))
    }

    // End of the party

    @Test
    fun `a party below the target score goes on`() {
        val results = engine.scoreParty(List(3) { BeloteRoundInput(FIRST, 100) })

        assertEquals(BelotePartyStatus.InProgress, engine.partyStatus(results, BeloteEndRule.TargetScore(1000)))
    }

    @Test
    fun `the team reaching the target score wins`() {
        val results = engine.scoreParty(List(10) { BeloteRoundInput(FIRST, 100) })

        assertEquals(BelotePartyStatus.Won(FIRST), engine.partyStatus(results, BeloteEndRule.TargetScore(1000)))
    }

    @Test
    fun `when both teams reach the target the higher one wins`() {
        val results = engine.scoreParty(listOf(BeloteRoundInput(SECOND, 100)))

        assertEquals(BelotePartyStatus.Won(SECOND), engine.partyStatus(results, BeloteEndRule.TargetScore(50)))
    }

    @Test
    fun `equal totals at the target need one more round`() {
        val results = engine.scoreParty(listOf(BeloteRoundInput(FIRST, 100), BeloteRoundInput(SECOND, 100)))

        assertEquals(BelotePartyStatus.InProgress, engine.partyStatus(results, BeloteEndRule.TargetScore(150)))
    }

    @Test
    fun `a party by round count ends after its last round`() {
        val rounds = List(9) { BeloteRoundInput(FIRST, 100) }
        val nine = engine.scoreParty(rounds)
        val ten = engine.scoreParty(rounds + BeloteRoundInput(SECOND, 120))

        assertEquals(BelotePartyStatus.InProgress, engine.partyStatus(nine, BeloteEndRule.RoundCount(10)))
        assertEquals(BelotePartyStatus.Won(FIRST), engine.partyStatus(ten, BeloteEndRule.RoundCount(10)))
    }

    @Test
    fun `equal totals after the last round need one more round`() {
        val tied = engine.scoreParty(listOf(BeloteRoundInput(FIRST, 100), BeloteRoundInput(SECOND, 100)))
        val decided = engine.scoreParty(
            listOf(BeloteRoundInput(FIRST, 100), BeloteRoundInput(SECOND, 100), BeloteRoundInput(SECOND, 90))
        )

        assertEquals(BelotePartyStatus.InProgress, engine.partyStatus(tied, BeloteEndRule.RoundCount(2)))
        assertEquals(BelotePartyStatus.Won(SECOND), engine.partyStatus(decided, BeloteEndRule.RoundCount(2)))
    }

    @Test
    fun `a reserve still pending at the end is lost`() {
        val results = engine.scoreParty(listOf(BeloteRoundInput(SECOND, 100), BeloteRoundInput(FIRST, 81)))

        assertEquals(81, results.last().reserveAfter)
        assertEquals(BelotePartyStatus.Won(SECOND), engine.partyStatus(results, BeloteEndRule.RoundCount(2)))
    }

    @Test
    fun `the default end rules are 1000 points and 10 rounds`() {
        assertEquals(1000, BeloteEndRule.DEFAULT_TARGET_SCORE)
        assertEquals(10, BeloteEndRule.DEFAULT_ROUND_COUNT)
    }

    // Invalid input

    @Test
    fun `trick points outside 0 to 162 are rejected`() {
        assertThrows(IllegalArgumentException::class.java) { engine.scoreRound(BeloteRoundInput(FIRST, -1)) }
        assertThrows(IllegalArgumentException::class.java) { engine.scoreRound(BeloteRoundInput(FIRST, 163)) }
        assertThrows(IllegalArgumentException::class.java) { engine.otherTeamTrickPoints(163) }
    }

    @Test
    fun `a capot contradicted by the trick points is rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            engine.scoreRound(BeloteRoundInput(FIRST, takerTrickPoints = 150, capotTeam = FIRST))
        }
        assertThrows(IllegalArgumentException::class.java) {
            engine.scoreRound(BeloteRoundInput(FIRST, takerTrickPoints = 10, capotTeam = SECOND))
        }
    }

    @Test
    fun `a negative reserve is rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            engine.scoreRound(BeloteRoundInput(FIRST, 100), pendingReserve = -1)
        }
    }

    @Test
    fun `a non positive end rule is rejected`() {
        assertThrows(IllegalArgumentException::class.java) { BeloteEndRule.TargetScore(0) }
        assertThrows(IllegalArgumentException::class.java) { BeloteEndRule.RoundCount(0) }
    }
}

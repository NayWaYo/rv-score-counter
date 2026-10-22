package com.rvetiveau.rvscorecount.data.entity

import com.rvetiveau.rvscorecount.domain.model.BeloteEndRule
import com.rvetiveau.rvscorecount.domain.model.BeloteRound
import com.rvetiveau.rvscorecount.domain.model.BeloteRoundInput
import com.rvetiveau.rvscorecount.domain.model.Participant
import com.rvetiveau.rvscorecount.domain.model.Party
import com.rvetiveau.rvscorecount.domain.model.PartyState
import com.rvetiveau.rvscorecount.domain.model.Team
import com.rvetiveau.rvscorecount.domain.model.TeamSide
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test
import java.util.UUID

/** Room mappings added for the Belote (schema v3), both ways, and their refusal of invalid rows. */
class BeloteEntityMappingTest {

    private val partyId = UUID.randomUUID()

    private fun aParty(endRule: BeloteEndRule?) = Party(
        id = partyId,
        gameId = "belote",
        status = PartyState.IN_PROGRESS,
        currentDealerIndex = 1,
        createdAt = 42L,
        endRule = endRule
    )

    private fun aPartyEntity(endRuleType: String?, endRuleValue: Int?) = PartyEntity(
        id = partyId.toString(),
        gameId = "belote",
        status = "IN_PROGRESS",
        currentDealerIndex = 0,
        createdAt = 0L,
        endRuleType = endRuleType,
        endRuleValue = endRuleValue
    )

    @Test
    fun `a party keeps its end rule both ways`() {
        listOf(BeloteEndRule.TargetScore(1000), BeloteEndRule.RoundCount(10), null).forEach { rule ->
            assertEquals(aParty(rule), aParty(rule).toEntity().toDomain())
        }
    }

    @Test
    fun `end rules are stored by type name and value`() {
        val target = aParty(BeloteEndRule.TargetScore(501)).toEntity()
        val rounds = aParty(BeloteEndRule.RoundCount(12)).toEntity()
        val none = aParty(null).toEntity()

        assertEquals("TARGET_SCORE" to 501, target.endRuleType to target.endRuleValue)
        assertEquals("ROUND_COUNT" to 12, rounds.endRuleType to rounds.endRuleValue)
        assertNull(none.endRuleType)
        assertNull(none.endRuleValue)
    }

    @Test
    fun `an incomplete, unknown or non positive end rule fails instead of being fixed`() {
        assertThrows(IllegalStateException::class.java) { aPartyEntity("TARGET_SCORE", null).toDomain() }
        assertThrows(IllegalStateException::class.java) { aPartyEntity(null, 1000).toDomain() }
        assertThrows(IllegalStateException::class.java) { aPartyEntity("POINTS", 1000).toDomain() }
        assertThrows(IllegalArgumentException::class.java) { aPartyEntity("ROUND_COUNT", 0).toDomain() }
    }

    @Test
    fun `a participant keeps its team both ways`() {
        listOf(TeamSide.FIRST, TeamSide.SECOND, null).forEach { side ->
            val participant = Participant(UUID.randomUUID(), partyId, "Alice", 0, side)
            assertEquals(participant, participant.toEntity(playerId = "p1").toDomain())
        }
    }

    @Test
    fun `an unknown participant team fails`() {
        val entity = ParticipantEntity(UUID.randomUUID().toString(), partyId.toString(), "Alice", 0, teamSide = "THIRD", playerId = "p1")

        assertThrows(IllegalArgumentException::class.java) { entity.toDomain() }
    }

    @Test
    fun `a team round trips and an unknown side fails`() {
        val team = Team(partyId, TeamSide.SECOND, "Eux")

        assertEquals(team, team.toEntity().toDomain())
        assertEquals("SECOND", team.toEntity().side)
        assertThrows(IllegalArgumentException::class.java) { TeamEntity(partyId.toString(), "THIRD", "X").toDomain() }
    }

    @Test
    fun `a belote round keeps what was entered both ways`() {
        val rounds = listOf(
            BeloteRoundInput(TeamSide.FIRST, 100),
            BeloteRoundInput(TeamSide.SECOND, 71, beloteTeam = TeamSide.SECOND),
            BeloteRoundInput(TeamSide.FIRST, 0, beloteTeam = TeamSide.FIRST, capotTeam = TeamSide.SECOND)
        ).mapIndexed { index, input -> BeloteRound(UUID.randomUUID(), partyId, index + 1, input) }

        rounds.forEach { assertEquals(it, it.toEntity().toDomain()) }
    }

    @Test
    fun `a belote round with an unknown side fails`() {
        val valid = BeloteRound(UUID.randomUUID(), partyId, 1, BeloteRoundInput(TeamSide.FIRST, 100)).toEntity()

        assertThrows(IllegalArgumentException::class.java) { valid.copy(takerSide = "THIRD").toDomain() }
        assertThrows(IllegalArgumentException::class.java) { valid.copy(beloteSide = "BOTH").toDomain() }
        assertThrows(IllegalArgumentException::class.java) { valid.copy(capotSide = "").toDomain() }
    }
}

package com.rvetiveau.rvscorecount.data.entity

import com.rvetiveau.rvscorecount.domain.model.Chelem
import com.rvetiveau.rvscorecount.domain.model.Poignee
import com.rvetiveau.rvscorecount.domain.model.TarotContract
import com.rvetiveau.rvscorecount.domain.model.TarotRound
import com.rvetiveau.rvscorecount.domain.model.TarotRoundInput
import com.rvetiveau.rvscorecount.domain.model.TarotSide
import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

/** Room mapping of a Manche of Tarot (schema v7), both ways, and its refusal of invalid rows. */
class TarotEntityMappingTest {

    private val full = TarotRound(
        id = UUID.randomUUID(),
        partyId = UUID.randomUUID(),
        roundNumber = 3,
        input = TarotRoundInput(
            takerId = UUID.randomUUID(),
            contract = TarotContract.GARDE_SANS,
            attackBouts = 2,
            attackPoints = 47,
            partnerId = UUID.randomUUID(),
            petitAuBout = TarotSide.DEFENSE,
            attackPoignee = Poignee.DOUBLE,
            defensePoignee = Poignee.SIMPLE,
            chelem = Chelem.ANNOUNCED_FAILED
        )
    )

    @Test
    fun `a Manche with every value goes and comes back unchanged, enums by name`() {
        val entity = full.toEntity()
        assertEquals("GARDE_SANS", entity.contract)
        assertEquals("DEFENSE", entity.petitAuBout)
        assertEquals("DOUBLE", entity.attackPoignee)
        assertEquals("ANNOUNCED_FAILED", entity.chelem)
        assertEquals(full, entity.toDomain())
    }

    @Test
    fun `a Manche without partner nor bonus goes and comes back unchanged`() {
        val bare = full.copy(input = TarotRoundInput(full.input.takerId, TarotContract.PRISE, attackBouts = 0, attackPoints = 60))
        val entity = bare.toEntity()
        assertEquals(
            listOf(null, null, null, null, null),
            listOf(entity.partnerId, entity.petitAuBout, entity.attackPoignee, entity.defensePoignee, entity.chelem)
        )
        assertEquals(bare, entity.toDomain())
    }

    @Test
    fun `an unknown value or an invalid id fails explicitly`() {
        val entity = full.toEntity()
        listOf(
            entity.copy(contract = "POUSSE"),
            entity.copy(petitAuBout = "BOTH"),
            entity.copy(attackPoignee = "QUADRUPLE"),
            entity.copy(defensePoignee = ""),
            entity.copy(chelem = "MAYBE"),
            entity.copy(takerId = "alice"),
            entity.copy(partnerId = "bob")
        ).forEach { invalid -> assertThrows(IllegalArgumentException::class.java) { invalid.toDomain() } }
    }
}

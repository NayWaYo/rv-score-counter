package com.rvetiveau.rvscorecount.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.rvetiveau.rvscorecount.domain.model.Chelem
import com.rvetiveau.rvscorecount.domain.model.Poignee
import com.rvetiveau.rvscorecount.domain.model.TarotContract
import com.rvetiveau.rvscorecount.domain.model.TarotRound
import com.rvetiveau.rvscorecount.domain.model.TarotRoundInput
import com.rvetiveau.rvscorecount.domain.model.TarotSide
import java.util.UUID

/**
 * What was entered for a Manche of Tarot (schema v7, AD-10); enum values are stored by name, the
 * taker and partner are Participant ids. Points per player are recomputed by the engine, never stored.
 */
@Entity(
    tableName = "tarot_rounds",
    foreignKeys = [ForeignKey(
        entity = PartyEntity::class,
        parentColumns = ["id"],
        childColumns = ["partyId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [
        Index("partyId"),
        Index(value = ["partyId", "roundNumber"], unique = true)
    ]
)
data class TarotRoundEntity(
    @PrimaryKey val id: String,
    val partyId: String,
    val roundNumber: Int,
    val takerId: String,
    val contract: String,
    val attackBouts: Int,
    val attackPoints: Int,
    val partnerId: String?,
    val petitAuBout: String?,
    val attackPoignee: String?,
    val defensePoignee: String?,
    val chelem: String?
)

fun TarotRoundEntity.toDomain() = TarotRound(
    id = UUID.fromString(id),
    partyId = UUID.fromString(partyId),
    roundNumber = roundNumber,
    input = TarotRoundInput(
        takerId = UUID.fromString(takerId),
        contract = TarotContract.valueOf(contract),
        attackBouts = attackBouts,
        attackPoints = attackPoints,
        partnerId = partnerId?.let(UUID::fromString),
        petitAuBout = petitAuBout?.let(TarotSide::valueOf),
        attackPoignee = attackPoignee?.let(Poignee::valueOf),
        defensePoignee = defensePoignee?.let(Poignee::valueOf),
        chelem = chelem?.let(Chelem::valueOf)
    )
)

fun TarotRound.toEntity() = TarotRoundEntity(
    id = id.toString(),
    partyId = partyId.toString(),
    roundNumber = roundNumber,
    takerId = input.takerId.toString(),
    contract = input.contract.name,
    attackBouts = input.attackBouts,
    attackPoints = input.attackPoints,
    partnerId = input.partnerId?.toString(),
    petitAuBout = input.petitAuBout?.name,
    attackPoignee = input.attackPoignee?.name,
    defensePoignee = input.defensePoignee?.name,
    chelem = input.chelem?.name
)

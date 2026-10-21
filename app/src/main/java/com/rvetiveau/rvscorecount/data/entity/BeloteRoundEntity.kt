package com.rvetiveau.rvscorecount.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.rvetiveau.rvscorecount.domain.model.BeloteRound
import com.rvetiveau.rvscorecount.domain.model.BeloteRoundInput
import com.rvetiveau.rvscorecount.domain.model.TeamSide
import java.util.UUID

/**
 * What was entered for a Manche of Belote (schema v3); the sides are [TeamSide] names. Points and
 * litige reserve are recomputed by the engine, never stored.
 */
@Entity(
    tableName = "belote_rounds",
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
data class BeloteRoundEntity(
    @PrimaryKey val id: String,
    val partyId: String,
    val roundNumber: Int,
    val takerSide: String,
    val takerTrickPoints: Int,
    val beloteSide: String?,
    val capotSide: String?
)

fun BeloteRoundEntity.toDomain() = BeloteRound(
    id = UUID.fromString(id),
    partyId = UUID.fromString(partyId),
    roundNumber = roundNumber,
    input = BeloteRoundInput(
        taker = TeamSide.valueOf(takerSide),
        takerTrickPoints = takerTrickPoints,
        beloteTeam = beloteSide?.let(TeamSide::valueOf),
        capotTeam = capotSide?.let(TeamSide::valueOf)
    )
)

fun BeloteRound.toEntity() = BeloteRoundEntity(
    id = id.toString(),
    partyId = partyId.toString(),
    roundNumber = roundNumber,
    takerSide = input.taker.name,
    takerTrickPoints = input.takerTrickPoints,
    beloteSide = input.beloteTeam?.name,
    capotSide = input.capotTeam?.name
)

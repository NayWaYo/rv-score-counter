package com.rvetiveau.rvscorecount.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import com.rvetiveau.rvscorecount.domain.model.Team
import com.rvetiveau.rvscorecount.domain.model.TeamSide
import java.util.UUID

/** An Équipe of a team game (schema v3): one per [side] and per Partie. */
@Entity(
    tableName = "teams",
    primaryKeys = ["partyId", "side"],
    foreignKeys = [ForeignKey(
        entity = PartyEntity::class,
        parentColumns = ["id"],
        childColumns = ["partyId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class TeamEntity(
    val partyId: String,
    val side: String,
    val name: String
)

fun TeamEntity.toDomain() = Team(
    partyId = UUID.fromString(partyId),
    side = TeamSide.valueOf(side),
    name = name
)

fun Team.toEntity() = TeamEntity(
    partyId = partyId.toString(),
    side = side.name,
    name = name
)

package com.rvetiveau.rvscorecount.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.rvetiveau.rvscorecount.domain.model.Participant
import com.rvetiveau.rvscorecount.domain.model.TeamSide
import java.util.UUID

@Entity(
    tableName = "participants",
    foreignKeys = [
        ForeignKey(
            entity = PartyEntity::class,
            parentColumns = ["id"],
            childColumns = ["partyId"],
            onDelete = ForeignKey.CASCADE
        ),
        // A Joueur outlives their Parties: deleting one keeps them (Story 10.1).
        ForeignKey(
            entity = PlayerEntity::class,
            parentColumns = ["id"],
            childColumns = ["playerId"]
        )
    ],
    indices = [Index("partyId"), Index("playerId")]
)
data class ParticipantEntity(
    @PrimaryKey val id: String,
    val partyId: String,
    val name: String,
    val listPosition: Int,
    /** Équipe in a team game (schema v3), a [TeamSide] name; null for the Papayoo. */
    @ColumnInfo(defaultValue = "NULL") val teamSide: String? = null,
    /** The Joueur this seat belongs to (schema v5). */
    val playerId: String
)

fun ParticipantEntity.toDomain() = Participant(
    id = UUID.fromString(id),
    partyId = UUID.fromString(partyId),
    name = name,
    listPosition = listPosition,
    teamSide = teamSide?.let(TeamSide::valueOf)
)

fun Participant.toEntity(playerId: String) = ParticipantEntity(
    id = id.toString(),
    partyId = partyId.toString(),
    name = name,
    listPosition = listPosition,
    teamSide = teamSide?.name,
    playerId = playerId
)

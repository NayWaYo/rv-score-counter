package com.rvetiveau.rvscorecount.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.rvetiveau.rvscorecount.domain.model.Participant
import java.util.UUID

@Entity(
    tableName = "participants",
    foreignKeys = [ForeignKey(
        entity = PartyEntity::class,
        parentColumns = ["id"],
        childColumns = ["partyId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("partyId")]
)
data class ParticipantEntity(
    @PrimaryKey val id: String,
    val partyId: String,
    val name: String,
    val listPosition: Int
)

fun ParticipantEntity.toDomain() = Participant(
    id = UUID.fromString(id),
    partyId = UUID.fromString(partyId),
    name = name,
    listPosition = listPosition
)

fun Participant.toEntity() = ParticipantEntity(
    id = id.toString(),
    partyId = partyId.toString(),
    name = name,
    listPosition = listPosition
)

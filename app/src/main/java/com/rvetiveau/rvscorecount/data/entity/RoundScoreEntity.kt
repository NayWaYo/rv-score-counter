package com.rvetiveau.rvscorecount.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import com.rvetiveau.rvscorecount.domain.model.RoundScore
import java.util.UUID

@Entity(
    tableName = "round_scores",
    primaryKeys = ["roundId", "participantId"],
    foreignKeys = [
        ForeignKey(
            entity = RoundEntity::class,
            parentColumns = ["id"], childColumns = ["roundId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ParticipantEntity::class,
            parentColumns = ["id"], childColumns = ["participantId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("roundId"), Index("participantId")]
)
data class RoundScoreEntity(
    val roundId: String,
    val participantId: String,
    val points: Int
)

fun RoundScoreEntity.toDomain() = RoundScore(
    roundId = UUID.fromString(roundId),
    participantId = UUID.fromString(participantId),
    points = points
)

fun RoundScore.toEntity() = RoundScoreEntity(
    roundId = roundId.toString(),
    participantId = participantId.toString(),
    points = points
)

package com.rvetiveau.rvscorecount.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.rvetiveau.rvscorecount.domain.model.CardSuit
import com.rvetiveau.rvscorecount.domain.model.Round
import java.util.UUID

@Entity(
    tableName = "rounds",
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
data class RoundEntity(
    @PrimaryKey val id: String,
    val partyId: String,
    val roundNumber: Int,
    val suit: String,
    /** Participant who took the 7 of [suit] (schema v2); null for rounds recorded before. */
    @ColumnInfo(defaultValue = "NULL") val sevenHolderId: String? = null
)

fun RoundEntity.toDomain() = Round(
    id = UUID.fromString(id),
    partyId = UUID.fromString(partyId),
    roundNumber = roundNumber,
    suit = CardSuit.valueOf(suit),
    sevenHolderId = sevenHolderId?.let(UUID::fromString)
)

fun Round.toEntity() = RoundEntity(
    id = id.toString(),
    partyId = partyId.toString(),
    roundNumber = roundNumber,
    suit = suit.name,
    sevenHolderId = sevenHolderId?.toString()
)

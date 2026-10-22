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
    /** The 7's suit of a Manche of Papayoo; null for a Manche of Jeu libre (schema v6, AD-9). */
    val suit: String?,
    /** Participant who took the 7 of [suit] (schema v2); null for rounds recorded before. */
    @ColumnInfo(defaultValue = "NULL") val sevenHolderId: String? = null
)

fun RoundEntity.toDomain() = Round(
    id = UUID.fromString(id),
    partyId = UUID.fromString(partyId),
    roundNumber = roundNumber,
    suit = CardSuit.valueOf(checkNotNull(suit) { "Round $id of party $partyId has no suit: it is not a Papayoo round" }),
    sevenHolderId = sevenHolderId?.let(UUID::fromString)
)

fun Round.toEntity() = RoundEntity(
    id = id.toString(),
    partyId = partyId.toString(),
    roundNumber = roundNumber,
    suit = suit.name,
    sevenHolderId = sevenHolderId?.toString()
)

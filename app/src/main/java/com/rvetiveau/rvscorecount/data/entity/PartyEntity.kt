package com.rvetiveau.rvscorecount.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.rvetiveau.rvscorecount.domain.model.Party
import com.rvetiveau.rvscorecount.domain.model.PartyState
import java.util.UUID

@Entity(tableName = "parties")
data class PartyEntity(
    @PrimaryKey val id: String,
    val gameId: String,
    val status: String,
    val currentDealerIndex: Int,
    val createdAt: Long
)

fun PartyEntity.toDomain() = Party(
    id = UUID.fromString(id),
    gameId = gameId,
    status = PartyState.valueOf(status),
    currentDealerIndex = currentDealerIndex,
    createdAt = createdAt
)

fun Party.toEntity() = PartyEntity(
    id = id.toString(),
    gameId = gameId,
    status = status.name,
    currentDealerIndex = currentDealerIndex,
    createdAt = createdAt
)

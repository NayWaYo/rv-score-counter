package com.rvetiveau.rvscorecount.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A Joueur followed from one Partie to the next (Story 10.1). [nameKey] is
 * [com.rvetiveau.rvscorecount.domain.model.playerNameKey] of [name]: unique, so one name is one player.
 */
@Entity(
    tableName = "players",
    indices = [Index(value = ["nameKey"], unique = true)]
)
data class PlayerEntity(
    @PrimaryKey val id: String,
    val name: String,
    val nameKey: String
)

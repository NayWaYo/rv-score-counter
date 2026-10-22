package com.rvetiveau.rvscorecount.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.rvetiveau.rvscorecount.data.entity.PlayerEntity
import com.rvetiveau.rvscorecount.domain.model.playerNameKey
import java.util.UUID
import kotlinx.coroutines.flow.Flow

@Dao
interface PlayerDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(player: PlayerEntity)

    @Query("SELECT * FROM players WHERE nameKey = :nameKey")
    suspend fun getByNameKey(nameKey: String): PlayerEntity?

    @Query("SELECT * FROM players")
    suspend fun getAll(): List<PlayerEntity>

    /** Every Joueur, with the creation time of their latest Partie; null when they have none left. */
    @Query(
        """
        SELECT players.name AS name, MAX(parties.createdAt) AS lastPlayedAt
        FROM players
        LEFT JOIN participants ON participants.playerId = players.id
        LEFT JOIN parties ON parties.id = participants.partyId
        GROUP BY players.id
        """
    )
    fun observeKnownNames(): Flow<List<KnownNameRow>>

    /** Every Joueur with their FINISHED Parties and the creation time of their latest Partie of any status. */
    @Query(
        """
        SELECT players.id AS id, players.name AS name,
            COUNT(DISTINCT CASE WHEN parties.status = 'FINISHED' THEN parties.id END) AS finishedParties,
            MAX(parties.createdAt) AS lastPlayedAt
        FROM players
        LEFT JOIN participants ON participants.playerId = players.id
        LEFT JOIN parties ON parties.id = participants.partyId
        GROUP BY players.id
        """
    )
    fun observeSummaries(): Flow<List<PlayerSummaryRow>>

    /** Number of rows changed: 0 when no Joueur has [id]. */
    @Query("UPDATE players SET name = :name, nameKey = :nameKey WHERE id = :id")
    suspend fun rename(id: String, name: String, nameKey: String): Int
}

data class KnownNameRow(
    val name: String,
    val lastPlayedAt: Long?
)

data class PlayerSummaryRow(
    val id: String,
    val name: String,
    val finishedParties: Int,
    val lastPlayedAt: Long?
)

/**
 * The id of the Joueur named [name], created when no Joueur has its key yet. Call it inside the
 * transaction that inserts the Participant, so the Joueur and the Participant commit together.
 */
suspend fun PlayerDao.playerIdFor(name: String): String {
    val key = playerNameKey(name)
    getByNameKey(key)?.let { return it.id }
    val player = PlayerEntity(id = UUID.randomUUID().toString(), name = name.trim(), nameKey = key)
    insert(player)
    return player.id
}

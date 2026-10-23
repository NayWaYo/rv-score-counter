package com.rvetiveau.rvscorecount.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.rvetiveau.rvscorecount.data.entity.PartyEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PartyDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(party: PartyEntity)

    @Update
    suspend fun update(party: PartyEntity)

    @Query("SELECT * FROM parties")
    suspend fun getAll(): List<PartyEntity>

    @Query("SELECT COUNT(*) FROM parties")
    fun observeCount(): Flow<Int>

    @Query("SELECT * FROM parties WHERE id = :id")
    suspend fun getById(id: String): PartyEntity?

    @Query("SELECT * FROM parties WHERE id = :id")
    fun observeById(id: String): Flow<PartyEntity?>

    @Query("SELECT * FROM parties WHERE status = :status LIMIT 1")
    suspend fun getPartyInProgress(status: String = "IN_PROGRESS"): PartyEntity?

    @Query("SELECT * FROM parties WHERE status = :status ORDER BY createdAt DESC")
    fun observeByStatus(status: String): Flow<List<PartyEntity>>

    /** Participants, rounds and round scores follow through the CASCADE foreign keys. */
    @Query("DELETE FROM parties WHERE id = :id")
    suspend fun deleteById(id: String)

    /** One row per exact game name of the Jeu libre (Story 11.2), with the creation time of its latest Partie. */
    @Query(
        "SELECT gameName AS name, MAX(createdAt) AS lastPlayedAt FROM parties " +
            "WHERE gameId = :gameId AND gameName IS NOT NULL GROUP BY gameName"
    )
    fun observeGameNames(gameId: String): Flow<List<KnownNameRow>>
}

package com.rvetiveau.rvscorecount.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.rvetiveau.rvscorecount.data.entity.ParticipantEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ParticipantDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(participant: ParticipantEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAll(participants: List<ParticipantEntity>)

    /** Opens a seat at [fromPosition] for an insertion (FR-4); only listPosition changes. */
    @Query("UPDATE participants SET listPosition = listPosition + 1 WHERE partyId = :partyId AND listPosition >= :fromPosition")
    suspend fun shiftPositionsFrom(partyId: String, fromPosition: Int)

    @Query("SELECT * FROM participants WHERE partyId = :partyId ORDER BY listPosition ASC")
    suspend fun getByPartyId(partyId: String): List<ParticipantEntity>

    @Query("SELECT * FROM participants WHERE partyId = :partyId ORDER BY listPosition ASC")
    fun observeByPartyId(partyId: String): Flow<List<ParticipantEntity>>

    /** One row per exact name across every Partie, with the creation time of its latest Partie. */
    @Query(
        """
        SELECT participants.name AS name, MAX(parties.createdAt) AS lastPlayedAt
        FROM participants INNER JOIN parties ON parties.id = participants.partyId
        GROUP BY participants.name
        """
    )
    fun observeKnownNames(): Flow<List<KnownNameRow>>
}

data class KnownNameRow(
    val name: String,
    val lastPlayedAt: Long
)

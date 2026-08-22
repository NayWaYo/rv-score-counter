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

    @Query("SELECT * FROM participants WHERE partyId = :partyId ORDER BY listPosition ASC")
    suspend fun getByPartyId(partyId: String): List<ParticipantEntity>

    @Query("SELECT * FROM participants WHERE partyId = :partyId ORDER BY listPosition ASC")
    fun observeByPartyId(partyId: String): Flow<List<ParticipantEntity>>
}

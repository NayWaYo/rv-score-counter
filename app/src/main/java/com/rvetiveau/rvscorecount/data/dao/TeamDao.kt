package com.rvetiveau.rvscorecount.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.rvetiveau.rvscorecount.data.entity.TeamEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TeamDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAll(teams: List<TeamEntity>)

    @Query("SELECT * FROM teams WHERE partyId = :partyId")
    suspend fun getByPartyId(partyId: String): List<TeamEntity>

    @Query("SELECT * FROM teams WHERE partyId = :partyId")
    fun observeByPartyId(partyId: String): Flow<List<TeamEntity>>

    @Query("SELECT * FROM teams")
    suspend fun getAll(): List<TeamEntity>
}

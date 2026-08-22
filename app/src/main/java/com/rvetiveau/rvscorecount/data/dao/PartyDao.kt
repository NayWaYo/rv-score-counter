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
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(party: PartyEntity)

    @Update
    suspend fun update(party: PartyEntity)

    @Query("SELECT * FROM parties WHERE id = :id")
    suspend fun getById(id: String): PartyEntity?

    @Query("SELECT * FROM parties WHERE id = :id")
    fun observeById(id: String): Flow<PartyEntity?>

    @Query("SELECT * FROM parties WHERE status = :status LIMIT 1")
    suspend fun getPartyInProgress(status: String = "IN_PROGRESS"): PartyEntity?
}

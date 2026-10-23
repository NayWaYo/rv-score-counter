package com.rvetiveau.rvscorecount.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.rvetiveau.rvscorecount.data.entity.RoundEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RoundDao {
    // ABORT : une manche validée n'est jamais écrasée par une insertion ; seule la correction
    // explicite (updateSevenHolder) la modifie
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(round: RoundEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAll(rounds: List<RoundEntity>)

    @Query("SELECT * FROM rounds WHERE partyId = :partyId ORDER BY roundNumber ASC")
    suspend fun getByPartyId(partyId: String): List<RoundEntity>

    @Query("SELECT * FROM rounds WHERE id = :id")
    suspend fun getById(id: String): RoundEntity?

    /** Returns the number of rows changed: 0 when the round does not exist. */
    @Query("UPDATE rounds SET sevenHolderId = :sevenHolderId WHERE id = :id")
    suspend fun updateSevenHolder(id: String, sevenHolderId: String?): Int

    @Query("SELECT * FROM rounds WHERE partyId = :partyId ORDER BY roundNumber ASC")
    fun observeByPartyId(partyId: String): Flow<List<RoundEntity>>

    @Query("SELECT * FROM rounds")
    suspend fun getAll(): List<RoundEntity>
}

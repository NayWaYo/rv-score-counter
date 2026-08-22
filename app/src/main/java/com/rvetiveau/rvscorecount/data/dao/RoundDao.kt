package com.rvetiveau.rvscorecount.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.rvetiveau.rvscorecount.data.entity.RoundEntity

@Dao
interface RoundDao {
    // ABORT — immuabilité FR-9 : aucune manche validée ne peut être écrasée
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(round: RoundEntity)

    @Query("SELECT * FROM rounds WHERE partyId = :partyId ORDER BY roundNumber ASC")
    suspend fun getByPartyId(partyId: String): List<RoundEntity>
}

package com.rvetiveau.rvscorecount.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.rvetiveau.rvscorecount.data.entity.RoundScoreEntity

@Dao
interface RoundScoreDao {
    // ABORT — immuabilité FR-9 : aucun score de manche validée ne peut être écrasé
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(score: RoundScoreEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAll(scores: List<RoundScoreEntity>)

    @Query("SELECT * FROM round_scores WHERE roundId = :roundId")
    suspend fun getByRoundId(roundId: String): List<RoundScoreEntity>
}

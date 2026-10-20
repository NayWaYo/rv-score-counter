package com.rvetiveau.rvscorecount.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.rvetiveau.rvscorecount.data.entity.RoundScoreEntity

@Dao
interface RoundScoreDao {
    // ABORT : un score validé n'est jamais écrasé par une insertion ; seule la correction
    // explicite (updatePoints) le modifie
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(score: RoundScoreEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAll(scores: List<RoundScoreEntity>)

    @Query("SELECT * FROM round_scores WHERE roundId = :roundId")
    suspend fun getByRoundId(roundId: String): List<RoundScoreEntity>

    /** Returns the number of rows changed: 0 when the participant has no score in this round. */
    @Query("UPDATE round_scores SET points = :points WHERE roundId = :roundId AND participantId = :participantId")
    suspend fun updatePoints(roundId: String, participantId: String, points: Int): Int
}

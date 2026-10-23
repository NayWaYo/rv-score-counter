package com.rvetiveau.rvscorecount.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.rvetiveau.rvscorecount.data.entity.BeloteRoundEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BeloteRoundDao {
    // ABORT : une manche validée n'est jamais écrasée par une insertion ; seule la correction
    // explicite (updateInput) la modifie
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(round: BeloteRoundEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAll(rounds: List<BeloteRoundEntity>)

    @Query("SELECT * FROM belote_rounds WHERE id = :id")
    suspend fun getById(id: String): BeloteRoundEntity?

    @Query("SELECT * FROM belote_rounds WHERE partyId = :partyId ORDER BY roundNumber ASC")
    suspend fun getByPartyId(partyId: String): List<BeloteRoundEntity>

    @Query("SELECT * FROM belote_rounds WHERE partyId = :partyId ORDER BY roundNumber ASC")
    fun observeByPartyId(partyId: String): Flow<List<BeloteRoundEntity>>

    @Query(
        "UPDATE belote_rounds SET takerSide = :takerSide, takerTrickPoints = :takerTrickPoints, " +
            "beloteSide = :beloteSide, capotSide = :capotSide, takerPlayerId = :takerPlayerId WHERE id = :id"
    )
    suspend fun updateInput(
        id: String,
        takerSide: String,
        takerTrickPoints: Int,
        beloteSide: String?,
        capotSide: String?,
        takerPlayerId: String?
    )

    @Query("SELECT * FROM belote_rounds")
    suspend fun getAll(): List<BeloteRoundEntity>
}

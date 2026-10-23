package com.rvetiveau.rvscorecount.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.rvetiveau.rvscorecount.data.entity.TarotRoundEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TarotRoundDao {
    // ABORT : une manche validée n'est jamais écrasée par une insertion ; seule la correction
    // explicite (update) la modifie
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(round: TarotRoundEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAll(rounds: List<TarotRoundEntity>)

    @Update
    suspend fun update(round: TarotRoundEntity)

    @Query("SELECT * FROM tarot_rounds WHERE id = :id")
    suspend fun getById(id: String): TarotRoundEntity?

    @Query("SELECT * FROM tarot_rounds WHERE partyId = :partyId ORDER BY roundNumber ASC")
    suspend fun getByPartyId(partyId: String): List<TarotRoundEntity>

    @Query("SELECT * FROM tarot_rounds WHERE partyId = :partyId ORDER BY roundNumber ASC")
    fun observeByPartyId(partyId: String): Flow<List<TarotRoundEntity>>

    @Query("SELECT * FROM tarot_rounds")
    suspend fun getAll(): List<TarotRoundEntity>
}

package com.rvetiveau.rvscorecount.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.rvetiveau.rvscorecount.data.dao.ParticipantDao
import com.rvetiveau.rvscorecount.data.dao.PartyDao
import com.rvetiveau.rvscorecount.data.dao.RoundDao
import com.rvetiveau.rvscorecount.data.dao.RoundScoreDao
import com.rvetiveau.rvscorecount.data.entity.ParticipantEntity
import com.rvetiveau.rvscorecount.data.entity.PartyEntity
import com.rvetiveau.rvscorecount.data.entity.RoundEntity
import com.rvetiveau.rvscorecount.data.entity.RoundScoreEntity

@Database(
    entities = [
        PartyEntity::class,
        ParticipantEntity::class,
        RoundEntity::class,
        RoundScoreEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun partyDao(): PartyDao
    abstract fun participantDao(): ParticipantDao
    abstract fun roundDao(): RoundDao
    abstract fun roundScoreDao(): RoundScoreDao
}

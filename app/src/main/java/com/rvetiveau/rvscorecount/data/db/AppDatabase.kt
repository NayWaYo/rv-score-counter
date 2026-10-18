package com.rvetiveau.rvscorecount.data.db

import androidx.room.AutoMigration
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
    version = 2,
    exportSchema = true,
    autoMigrations = [
        // v2 : colonne rounds.sevenHolderId (qui a eu le 7 de la couleur du dé), nulle pour les manches existantes.
        AutoMigration(from = 1, to = 2)
    ]
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun partyDao(): PartyDao
    abstract fun participantDao(): ParticipantDao
    abstract fun roundDao(): RoundDao
    abstract fun roundScoreDao(): RoundScoreDao
}

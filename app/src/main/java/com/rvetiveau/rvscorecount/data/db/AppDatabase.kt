package com.rvetiveau.rvscorecount.data.db

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.rvetiveau.rvscorecount.data.dao.BeloteRoundDao
import com.rvetiveau.rvscorecount.data.dao.ParticipantDao
import com.rvetiveau.rvscorecount.data.dao.PartyDao
import com.rvetiveau.rvscorecount.data.dao.RoundDao
import com.rvetiveau.rvscorecount.data.dao.RoundScoreDao
import com.rvetiveau.rvscorecount.data.dao.TeamDao
import com.rvetiveau.rvscorecount.data.entity.BeloteRoundEntity
import com.rvetiveau.rvscorecount.data.entity.ParticipantEntity
import com.rvetiveau.rvscorecount.data.entity.PartyEntity
import com.rvetiveau.rvscorecount.data.entity.RoundEntity
import com.rvetiveau.rvscorecount.data.entity.RoundScoreEntity
import com.rvetiveau.rvscorecount.data.entity.TeamEntity

@Database(
    entities = [
        PartyEntity::class,
        ParticipantEntity::class,
        RoundEntity::class,
        RoundScoreEntity::class,
        TeamEntity::class,
        BeloteRoundEntity::class
    ],
    version = 3,
    exportSchema = true,
    autoMigrations = [
        // v2 : colonne rounds.sevenHolderId (qui a eu le 7 de la couleur du dé), nulle pour les manches existantes.
        AutoMigration(from = 1, to = 2),
        // v3 (Belote) : tables teams et belote_rounds, colonnes parties.endRuleType/endRuleValue et
        // participants.teamSide, nulles pour les parties de Papayoo existantes.
        AutoMigration(from = 2, to = 3)
    ]
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun partyDao(): PartyDao
    abstract fun participantDao(): ParticipantDao
    abstract fun roundDao(): RoundDao
    abstract fun roundScoreDao(): RoundScoreDao
    abstract fun teamDao(): TeamDao
    abstract fun beloteRoundDao(): BeloteRoundDao
}

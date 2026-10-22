package com.rvetiveau.rvscorecount.data.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.rvetiveau.rvscorecount.domain.model.playerNameKey
import java.util.UUID

/**
 * v5 (Story 10.1) : one Joueur per distinct name of the stored Parties, every Participant linked to
 * theirs. The name key is computed in Kotlin, since SQLite's lower() only knows ASCII, and each
 * Joueur keeps the spelling of their latest Partie. `participants` is rebuilt to gain a non-null
 * `playerId` with its foreign key; Room turns foreign keys on only once migrations are done, so
 * dropping the old table leaves `round_scores`, which references it, untouched.
 */
val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `players` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `nameKey` TEXT NOT NULL, PRIMARY KEY(`id`))")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_players_nameKey` ON `players` (`nameKey`)")

        val playerIdByKey = mutableMapOf<String, String>()
        val playerIdByParticipant = mutableMapOf<String, String>()
        db.query(
            "SELECT participants.id, participants.name FROM participants " +
                "INNER JOIN parties ON parties.id = participants.partyId " +
                "ORDER BY parties.createdAt DESC"
        ).use { cursor ->
            while (cursor.moveToNext()) {
                val participantId = cursor.getString(0)
                val name = cursor.getString(1)
                val key = playerNameKey(name)
                val playerId = playerIdByKey.getOrPut(key) {
                    UUID.randomUUID().toString().also { id ->
                        db.execSQL("INSERT INTO `players` (`id`, `name`, `nameKey`) VALUES (?, ?, ?)", arrayOf(id, name.trim(), key))
                    }
                }
                playerIdByParticipant[participantId] = playerId
            }
        }

        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `_new_participants` (`id` TEXT NOT NULL, `partyId` TEXT NOT NULL, " +
                "`name` TEXT NOT NULL, `listPosition` INTEGER NOT NULL, `teamSide` TEXT DEFAULT NULL, " +
                "`playerId` TEXT NOT NULL, PRIMARY KEY(`id`), " +
                "FOREIGN KEY(`partyId`) REFERENCES `parties`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , " +
                "FOREIGN KEY(`playerId`) REFERENCES `players`(`id`) ON UPDATE NO ACTION ON DELETE NO ACTION )"
        )
        playerIdByParticipant.forEach { (participantId, playerId) ->
            db.execSQL(
                "INSERT INTO `_new_participants` (`id`, `partyId`, `name`, `listPosition`, `teamSide`, `playerId`) " +
                    "SELECT `id`, `partyId`, `name`, `listPosition`, `teamSide`, ? FROM `participants` WHERE `id` = ?",
                arrayOf(playerId, participantId)
            )
        }
        db.execSQL("DROP TABLE `participants`")
        db.execSQL("ALTER TABLE `_new_participants` RENAME TO `participants`")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_participants_partyId` ON `participants` (`partyId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_participants_playerId` ON `participants` (`playerId`)")
    }
}

/** Hand-written migrations, on top of the auto-migrations of [AppDatabase]: the app and its tests open the base with them. */
val MANUAL_MIGRATIONS: Array<Migration> = arrayOf(MIGRATION_4_5)

package com.rvetiveau.rvscorecount.data.db

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

/**
 * Schema migrations through the production path: a real file of an older version, written with the
 * SQL of the exported `schemas/.../<version>.json`, is opened by Room, which runs the auto-migrations
 * up to the current version and validates the result. v2 added rounds.sevenHolderId, v3 the Belote
 * (teams, belote_rounds, end rule, participant team), v4 the Belote taker player. Existing parties,
 * rounds and scores must survive untouched.
 */
class AppDatabaseMigrationTest {

    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext

    @Before
    @After
    fun deleteDatabase() {
        context.deleteDatabase(DB_NAME)
    }

    @Test
    fun openingAVersion1DatabaseMigratesItAndKeepsItsRounds() = runTest {
        SQLiteDatabase.openOrCreateDatabase(context.getDatabasePath(DB_NAME), null).use { v1 ->
            V1_SCHEMA.forEach(v1::execSQL)
            v1.execSQL("INSERT INTO parties (id, gameId, status, currentDealerIndex, createdAt) VALUES ('p', 'papayoo', 'FINISHED', 0, 42)")
            v1.execSQL("INSERT INTO participants (id, partyId, name, listPosition) VALUES ('a', 'p', 'Alice', 0)")
            v1.execSQL("INSERT INTO rounds (id, partyId, roundNumber, suit) VALUES ('r', 'p', 1, 'HEART')")
            v1.execSQL("INSERT INTO round_scores (roundId, participantId, points) VALUES ('r', 'a', 250)")
            v1.version = 1
        }

        val database = Room.databaseBuilder(context, AppDatabase::class.java, DB_NAME).build()
        try {
            val round = database.roundDao().getByPartyId("p").single()
            assertEquals(1, round.roundNumber)
            assertEquals("HEART", round.suit)
            assertNull(round.sevenHolderId)
            assertEquals(250, database.roundScoreDao().getByRoundId("r").single().points)
            assertEquals("Alice", database.participantDao().getByPartyId("p").single().name)
            // ... and continues to v3 (Belote) without touching them.
            assertNull(database.partyDao().getById("p")?.endRuleType)
            assertNull(database.participantDao().getByPartyId("p").single().teamSide)
        } finally {
            database.close()
        }
    }

    @Test
    fun openingAVersion2DatabaseAddsTheBeloteSchemaAndKeepsThePapayooParties() = runTest {
        SQLiteDatabase.openOrCreateDatabase(context.getDatabasePath(DB_NAME), null).use { v2 ->
            V2_SCHEMA.forEach(v2::execSQL)
            v2.execSQL("INSERT INTO parties (id, gameId, status, currentDealerIndex, createdAt) VALUES ('p', 'papayoo', 'IN_PROGRESS', 2, 42)")
            v2.execSQL("INSERT INTO participants (id, partyId, name, listPosition) VALUES ('a', 'p', 'Alice', 0)")
            v2.execSQL("INSERT INTO rounds (id, partyId, roundNumber, suit, sevenHolderId) VALUES ('r', 'p', 1, 'CLUB', 'a')")
            v2.execSQL("INSERT INTO round_scores (roundId, participantId, points) VALUES ('r', 'a', 250)")
            v2.version = 2
        }

        val database = Room.databaseBuilder(context, AppDatabase::class.java, DB_NAME).build()
        try {
            val party = checkNotNull(database.partyDao().getById("p"))
            assertEquals("IN_PROGRESS", party.status)
            assertEquals(2, party.currentDealerIndex)
            assertNull(party.endRuleType)
            assertNull(party.endRuleValue)
            val participant = database.participantDao().getByPartyId("p").single()
            assertEquals("Alice", participant.name)
            assertNull(participant.teamSide)
            assertEquals("a", database.roundDao().getByPartyId("p").single().sevenHolderId)
            assertEquals(250, database.roundScoreDao().getByRoundId("r").single().points)
            assertEquals(emptyList<Any>(), database.teamDao().getByPartyId("p"))
            assertEquals(emptyList<Any>(), database.beloteRoundDao().getByPartyId("p"))
        } finally {
            database.close()
        }
    }

    @Test
    fun openingAVersion3DatabaseAddsTheTakerPlayerAndKeepsTheBeloteRounds() = runTest {
        SQLiteDatabase.openOrCreateDatabase(context.getDatabasePath(DB_NAME), null).use { v3 ->
            V3_SCHEMA.forEach(v3::execSQL)
            v3.execSQL("INSERT INTO parties (id, gameId, status, currentDealerIndex, createdAt, endRuleType, endRuleValue) VALUES ('p', 'belote', 'IN_PROGRESS', 1, 42, 'TARGET_SCORE', 1000)")
            v3.execSQL("INSERT INTO teams (partyId, side, name) VALUES ('p', 'FIRST', 'Nous'), ('p', 'SECOND', 'Eux')")
            v3.execSQL("INSERT INTO participants (id, partyId, name, listPosition, teamSide) VALUES ('a', 'p', 'Alice', 0, 'FIRST')")
            v3.execSQL("INSERT INTO belote_rounds (id, partyId, roundNumber, takerSide, takerTrickPoints, beloteSide, capotSide) VALUES ('r', 'p', 1, 'FIRST', 81, 'SECOND', NULL)")
            v3.version = 3
        }

        val database = Room.databaseBuilder(context, AppDatabase::class.java, DB_NAME).build()
        try {
            val round = database.beloteRoundDao().getByPartyId("p").single()
            assertEquals("FIRST", round.takerSide)
            assertEquals(81, round.takerTrickPoints)
            assertEquals("SECOND", round.beloteSide)
            assertNull(round.takerPlayerId)
            assertEquals(listOf("Nous", "Eux"), database.teamDao().getByPartyId("p").sortedBy { it.side }.map { it.name })
            assertEquals("TARGET_SCORE", database.partyDao().getById("p")?.endRuleType)
        } finally {
            database.close()
        }
    }

    private companion object {
        const val DB_NAME = "migration-test.db"

        /** Copied from app/schemas/com.rvetiveau.rvscorecount.data.db.AppDatabase/3.json. */
        val V3_SCHEMA = listOf(
            "CREATE TABLE IF NOT EXISTS `parties` (`id` TEXT NOT NULL, `gameId` TEXT NOT NULL, `status` TEXT NOT NULL, `currentDealerIndex` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, `endRuleType` TEXT DEFAULT NULL, `endRuleValue` INTEGER DEFAULT NULL, PRIMARY KEY(`id`))",
            "CREATE TABLE IF NOT EXISTS `participants` (`id` TEXT NOT NULL, `partyId` TEXT NOT NULL, `name` TEXT NOT NULL, `listPosition` INTEGER NOT NULL, `teamSide` TEXT DEFAULT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`partyId`) REFERENCES `parties`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
            "CREATE INDEX IF NOT EXISTS `index_participants_partyId` ON `participants` (`partyId`)",
            "CREATE TABLE IF NOT EXISTS `rounds` (`id` TEXT NOT NULL, `partyId` TEXT NOT NULL, `roundNumber` INTEGER NOT NULL, `suit` TEXT NOT NULL, `sevenHolderId` TEXT DEFAULT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`partyId`) REFERENCES `parties`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
            "CREATE INDEX IF NOT EXISTS `index_rounds_partyId` ON `rounds` (`partyId`)",
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_rounds_partyId_roundNumber` ON `rounds` (`partyId`, `roundNumber`)",
            "CREATE TABLE IF NOT EXISTS `round_scores` (`roundId` TEXT NOT NULL, `participantId` TEXT NOT NULL, `points` INTEGER NOT NULL, PRIMARY KEY(`roundId`, `participantId`), FOREIGN KEY(`roundId`) REFERENCES `rounds`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , FOREIGN KEY(`participantId`) REFERENCES `participants`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
            "CREATE INDEX IF NOT EXISTS `index_round_scores_roundId` ON `round_scores` (`roundId`)",
            "CREATE INDEX IF NOT EXISTS `index_round_scores_participantId` ON `round_scores` (`participantId`)",
            "CREATE TABLE IF NOT EXISTS `teams` (`partyId` TEXT NOT NULL, `side` TEXT NOT NULL, `name` TEXT NOT NULL, PRIMARY KEY(`partyId`, `side`), FOREIGN KEY(`partyId`) REFERENCES `parties`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
            "CREATE TABLE IF NOT EXISTS `belote_rounds` (`id` TEXT NOT NULL, `partyId` TEXT NOT NULL, `roundNumber` INTEGER NOT NULL, `takerSide` TEXT NOT NULL, `takerTrickPoints` INTEGER NOT NULL, `beloteSide` TEXT, `capotSide` TEXT, PRIMARY KEY(`id`), FOREIGN KEY(`partyId`) REFERENCES `parties`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
            "CREATE INDEX IF NOT EXISTS `index_belote_rounds_partyId` ON `belote_rounds` (`partyId`)",
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_belote_rounds_partyId_roundNumber` ON `belote_rounds` (`partyId`, `roundNumber`)",
            "CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)",
            "INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '7e4da602d34cfafe32837c50e33f7380')"
        )

        /** Copied from app/schemas/com.rvetiveau.rvscorecount.data.db.AppDatabase/2.json. */
        val V2_SCHEMA = listOf(
            "CREATE TABLE IF NOT EXISTS `parties` (`id` TEXT NOT NULL, `gameId` TEXT NOT NULL, `status` TEXT NOT NULL, `currentDealerIndex` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, PRIMARY KEY(`id`))",
            "CREATE TABLE IF NOT EXISTS `participants` (`id` TEXT NOT NULL, `partyId` TEXT NOT NULL, `name` TEXT NOT NULL, `listPosition` INTEGER NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`partyId`) REFERENCES `parties`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
            "CREATE INDEX IF NOT EXISTS `index_participants_partyId` ON `participants` (`partyId`)",
            "CREATE TABLE IF NOT EXISTS `rounds` (`id` TEXT NOT NULL, `partyId` TEXT NOT NULL, `roundNumber` INTEGER NOT NULL, `suit` TEXT NOT NULL, `sevenHolderId` TEXT DEFAULT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`partyId`) REFERENCES `parties`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
            "CREATE INDEX IF NOT EXISTS `index_rounds_partyId` ON `rounds` (`partyId`)",
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_rounds_partyId_roundNumber` ON `rounds` (`partyId`, `roundNumber`)",
            "CREATE TABLE IF NOT EXISTS `round_scores` (`roundId` TEXT NOT NULL, `participantId` TEXT NOT NULL, `points` INTEGER NOT NULL, PRIMARY KEY(`roundId`, `participantId`), FOREIGN KEY(`roundId`) REFERENCES `rounds`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , FOREIGN KEY(`participantId`) REFERENCES `participants`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
            "CREATE INDEX IF NOT EXISTS `index_round_scores_roundId` ON `round_scores` (`roundId`)",
            "CREATE INDEX IF NOT EXISTS `index_round_scores_participantId` ON `round_scores` (`participantId`)",
            "CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)",
            "INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '063676c4b1b6ac3d454ae50e76fc6143')"
        )

        /** Copied from app/schemas/com.rvetiveau.rvscorecount.data.db.AppDatabase/1.json. */
        val V1_SCHEMA = listOf(
            "CREATE TABLE IF NOT EXISTS `parties` (`id` TEXT NOT NULL, `gameId` TEXT NOT NULL, `status` TEXT NOT NULL, `currentDealerIndex` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, PRIMARY KEY(`id`))",
            "CREATE TABLE IF NOT EXISTS `participants` (`id` TEXT NOT NULL, `partyId` TEXT NOT NULL, `name` TEXT NOT NULL, `listPosition` INTEGER NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`partyId`) REFERENCES `parties`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
            "CREATE INDEX IF NOT EXISTS `index_participants_partyId` ON `participants` (`partyId`)",
            "CREATE TABLE IF NOT EXISTS `rounds` (`id` TEXT NOT NULL, `partyId` TEXT NOT NULL, `roundNumber` INTEGER NOT NULL, `suit` TEXT NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`partyId`) REFERENCES `parties`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
            "CREATE INDEX IF NOT EXISTS `index_rounds_partyId` ON `rounds` (`partyId`)",
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_rounds_partyId_roundNumber` ON `rounds` (`partyId`, `roundNumber`)",
            "CREATE TABLE IF NOT EXISTS `round_scores` (`roundId` TEXT NOT NULL, `participantId` TEXT NOT NULL, `points` INTEGER NOT NULL, PRIMARY KEY(`roundId`, `participantId`), FOREIGN KEY(`roundId`) REFERENCES `rounds`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , FOREIGN KEY(`participantId`) REFERENCES `participants`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
            "CREATE INDEX IF NOT EXISTS `index_round_scores_roundId` ON `round_scores` (`roundId`)",
            "CREATE INDEX IF NOT EXISTS `index_round_scores_participantId` ON `round_scores` (`participantId`)",
            "CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)",
            "INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '054171b738e57b89801fc68e9e2860a8')"
        )
    }
}

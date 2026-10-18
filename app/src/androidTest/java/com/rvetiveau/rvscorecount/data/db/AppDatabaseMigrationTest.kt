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
 * Schema v1 -> v2 (rounds.sevenHolderId) through the production path: a real v1 file, written with
 * the SQL of the exported `schemas/.../1.json`, is opened by Room, which runs the auto-migration and
 * validates the result against v2. Existing parties, rounds and scores must survive untouched.
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
        } finally {
            database.close()
        }
    }

    private companion object {
        const val DB_NAME = "migration-test.db"

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

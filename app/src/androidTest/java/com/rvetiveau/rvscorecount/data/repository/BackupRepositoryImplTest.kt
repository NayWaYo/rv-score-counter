package com.rvetiveau.rvscorecount.data.repository

import android.net.Uri
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.rvetiveau.rvscorecount.data.backup.BACKUP_FORMAT
import com.rvetiveau.rvscorecount.data.backup.decodeBackupFile
import com.rvetiveau.rvscorecount.data.backup.toTables
import com.rvetiveau.rvscorecount.data.db.AppDatabase
import com.rvetiveau.rvscorecount.data.entity.BeloteRoundEntity
import com.rvetiveau.rvscorecount.data.entity.ParticipantEntity
import com.rvetiveau.rvscorecount.data.entity.PartyEntity
import com.rvetiveau.rvscorecount.data.entity.PlayerEntity
import com.rvetiveau.rvscorecount.data.entity.RoundEntity
import com.rvetiveau.rvscorecount.data.entity.RoundScoreEntity
import com.rvetiveau.rvscorecount.data.entity.TeamEntity
import com.rvetiveau.rvscorecount.domain.model.BackupContent
import com.rvetiveau.rvscorecount.domain.model.playerNameKey
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** The backup (Story 12.1) holds every row of a real database, written to the chosen document. */
class BackupRepositoryImplTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var database: AppDatabase
    private lateinit var repository: BackupRepositoryImpl
    private lateinit var file: File

    private val players = listOf(
        PlayerEntity("p-a", "Alice", "alice"),
        PlayerEntity("p-b", "Bob", "bob"),
        PlayerEntity("p-e", "Eve", "eve")
    )
    private val parties = listOf(
        PartyEntity("papayoo", "papayoo", "IN_PROGRESS", 1, 1_000),
        PartyEntity("belote", "belote", "FINISHED", 0, 2_000, "TARGET_SCORE", 1000),
        PartyEntity("free", "free", "FINISHED", 0, 3_000, gameName = "Skyjo", winDirection = "LOWEST_WINS")
    )
    private val participants = listOf(
        ParticipantEntity("pa-a", "papayoo", "Alice", 0, playerId = "p-a"),
        ParticipantEntity("pa-b", "papayoo", "Bob", 1, playerId = "p-b"),
        ParticipantEntity("be-a", "belote", "Alice", 0, "FIRST", "p-a"),
        ParticipantEntity("be-b", "belote", "Bob", 1, "SECOND", "p-b"),
        ParticipantEntity("fr-a", "free", "Alice", 0, playerId = "p-a"),
        ParticipantEntity("fr-b", "free", "Bob", 1, playerId = "p-b")
    )
    private val teams = listOf(TeamEntity("belote", "FIRST", "Nous"), TeamEntity("belote", "SECOND", "Eux"))
    private val rounds = listOf(
        RoundEntity("pr-1", "papayoo", 1, "CLUB", "pa-b"),
        RoundEntity("fr-1", "free", 1, suit = null)
    )
    private val roundScores = listOf(
        RoundScoreEntity("pr-1", "pa-a", 50), RoundScoreEntity("pr-1", "pa-b", 200),
        RoundScoreEntity("fr-1", "fr-a", -5), RoundScoreEntity("fr-1", "fr-b", 12)
    )
    private val beloteRounds = listOf(BeloteRoundEntity("br-1", "belote", 1, "FIRST", 120, "FIRST", null, "p-a"))

    @Before
    fun setUp() = runTest {
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        repository = BackupRepositoryImpl(context, database)
        file = File(context.cacheDir, "backup-test.json").apply { delete() }

        players.forEach { database.playerDao().insert(it) }
        parties.forEach { database.partyDao().insert(it) }
        database.participantDao().insertAll(participants)
        database.teamDao().insertAll(teams)
        rounds.forEach { database.roundDao().insert(it) }
        database.roundScoreDao().insertAll(roundScores)
        beloteRounds.forEach { database.beloteRoundDao().insert(it) }
    }

    @After
    fun tearDown() {
        database.close()
        file.delete()
    }

    @Test
    fun countsEveryJoueurAndEveryPartie() = runTest {
        assertEquals(BackupContent(players = 3, parties = 3), repository.observeContent().first())
    }

    @Test
    fun writesEveryRowOfEveryTableToTheChosenDocument() = runTest {
        val before = System.currentTimeMillis()
        repository.export(Uri.fromFile(file).toString())

        val backup = decodeBackupFile(file.readText())
        assertEquals(BACKUP_FORMAT, backup.format)
        assertTrue(backup.createdAt >= before)
        val tables = backup.toTables(::playerNameKey)
        assertEquals(players.toSet(), tables.players.toSet())
        assertEquals(parties.toSet(), tables.parties.toSet())
        assertEquals(participants.toSet(), tables.participants.toSet())
        assertEquals(teams.toSet(), tables.teams.toSet())
        assertEquals(rounds.toSet(), tables.rounds.toSet())
        assertEquals(roundScores.toSet(), tables.roundScores.toSet())
        assertEquals(beloteRounds.toSet(), tables.beloteRounds.toSet())
    }

    @Test
    fun exportingAgainToTheSameDocumentReplacesItsContent() = runTest {
        file.writeText("x".repeat(100_000))

        repository.export(Uri.fromFile(file).toString())

        assertEquals(3, decodeBackupFile(file.readText()).parties.size)
    }
}

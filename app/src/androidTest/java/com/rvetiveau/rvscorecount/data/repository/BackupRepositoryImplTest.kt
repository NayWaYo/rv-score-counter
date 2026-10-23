package com.rvetiveau.rvscorecount.data.repository

import android.net.Uri
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.rvetiveau.rvscorecount.data.backup.BACKUP_FORMAT
import com.rvetiveau.rvscorecount.data.backup.BackupTables
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
import com.rvetiveau.rvscorecount.domain.model.BackupReading
import com.rvetiveau.rvscorecount.domain.model.playerNameKey
import java.io.File
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * The backup (Story 12.1) holds every row of a real database, written to the chosen document; its
 * restore (Story 12.2) replaces every row in one transaction, or nothing at all.
 */
class BackupRepositoryImplTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var database: AppDatabase
    private lateinit var repository: BackupRepositoryImpl
    private lateinit var file: File
    private val uri get() = Uri.fromFile(file).toString()

    private fun id() = UUID.randomUUID().toString()

    private val alice = PlayerEntity(id(), "Alice", "alice")
    private val bob = PlayerEntity(id(), "Bob", "bob")
    private val eve = PlayerEntity(id(), "Eve", "eve")
    private val papayoo = PartyEntity(id(), "papayoo", "IN_PROGRESS", 1, 1_000)
    private val belote = PartyEntity(id(), "belote", "FINISHED", 0, 2_000, "TARGET_SCORE", 1000)
    private val free = PartyEntity(id(), "free", "FINISHED", 0, 3_000, gameName = "Skyjo", winDirection = "LOWEST_WINS")
    private val papayooAlice = ParticipantEntity(id(), papayoo.id, "Alice", 0, playerId = alice.id)
    private val papayooBob = ParticipantEntity(id(), papayoo.id, "Bob", 1, playerId = bob.id)
    private val freeAlice = ParticipantEntity(id(), free.id, "Alice", 0, playerId = alice.id)
    private val freeBob = ParticipantEntity(id(), free.id, "Bob", 1, playerId = bob.id)
    private val papayooRound = RoundEntity(id(), papayoo.id, 1, "CLUB", papayooBob.id)
    private val freeRound = RoundEntity(id(), free.id, 1, suit = null)

    /** Every Jeu, and a Joueur without Partie. */
    private val original = BackupTables(
        players = listOf(alice, bob, eve),
        parties = listOf(papayoo, belote, free),
        participants = listOf(
            papayooAlice, papayooBob,
            ParticipantEntity(id(), belote.id, "Alice", 0, "FIRST", alice.id),
            ParticipantEntity(id(), belote.id, "Bob", 1, "SECOND", bob.id),
            freeAlice, freeBob
        ),
        teams = listOf(TeamEntity(belote.id, "FIRST", "Nous"), TeamEntity(belote.id, "SECOND", "Eux")),
        rounds = listOf(papayooRound, freeRound),
        roundScores = listOf(
            RoundScoreEntity(papayooRound.id, papayooAlice.id, 50), RoundScoreEntity(papayooRound.id, papayooBob.id, 200),
            RoundScoreEntity(freeRound.id, freeAlice.id, -5), RoundScoreEntity(freeRound.id, freeBob.id, 12)
        ),
        beloteRounds = listOf(BeloteRoundEntity(id(), belote.id, 1, "FIRST", 120, "FIRST", null, alice.id))
    )

    @Before
    fun setUp() = runTest {
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        repository = BackupRepositoryImpl(context, database)
        file = File(context.cacheDir, "backup-test.json").apply { delete() }
        repository.replaceTables(original)
    }

    @After
    fun tearDown() {
        database.close()
        file.delete()
    }

    private fun assertSameRows(expected: BackupTables, actual: BackupTables) {
        assertEquals(expected.players.toSet(), actual.players.toSet())
        assertEquals(expected.parties.toSet(), actual.parties.toSet())
        assertEquals(expected.participants.toSet(), actual.participants.toSet())
        assertEquals(expected.teams.toSet(), actual.teams.toSet())
        assertEquals(expected.rounds.toSet(), actual.rounds.toSet())
        assertEquals(expected.roundScores.toSet(), actual.roundScores.toSet())
        assertEquals(expected.beloteRounds.toSet(), actual.beloteRounds.toSet())
    }

    /** A second database, replaced by [original]'s backup. */
    private val other = BackupTables(
        players = listOf(PlayerEntity(id(), "Zoé", "zoé")),
        parties = listOf(PartyEntity(id(), "papayoo", "FINISHED", 0, 9_000)),
        participants = emptyList(), teams = emptyList(), rounds = emptyList(), roundScores = emptyList(), beloteRounds = emptyList()
    )

    @Test
    fun countsEveryJoueurAndEveryPartie() = runTest {
        assertEquals(BackupContent(players = 3, parties = 3), repository.observeContent().first())
    }

    @Test
    fun writesEveryRowOfEveryTableToTheChosenDocument() = runTest {
        val before = System.currentTimeMillis()
        repository.export(uri)

        val backup = decodeBackupFile(file.readText())
        assertEquals(BACKUP_FORMAT, backup.format)
        assertTrue(backup.createdAt >= before)
        assertSameRows(original, backup.toTables(::playerNameKey))
    }

    @Test
    fun exportingAgainToTheSameDocumentReplacesItsContent() = runTest {
        file.writeText("x".repeat(100_000))

        repository.export(uri)

        assertEquals(3, decodeBackupFile(file.readText()).parties.size)
    }

    @Test
    fun readingABackupChangesNothing() = runTest {
        repository.export(uri)
        repository.replaceTables(other)

        val reading = repository.read(uri)

        assertTrue(reading is BackupReading.Valid)
        assertEquals(BackupContent(players = 3, parties = 3), (reading as BackupReading.Valid).content)
        assertSameRows(other, repository.readTables())
    }

    @Test
    fun restoringReplacesEveryRowWithTheBackup() = runTest {
        repository.export(uri)
        repository.replaceTables(other)

        assertTrue(repository.restore(uri) is BackupReading.Valid)

        assertSameRows(original, repository.readTables())
        assertEquals(BackupContent(players = 3, parties = 3), repository.observeContent().first())
    }

    @Test
    fun aRefusedFileChangesNothing() = runTest {
        file.writeText("""{"format": 2}""")
        assertEquals(BackupReading.NewerFormat, repository.restore(uri))
        file.writeText("pas une sauvegarde")
        assertEquals(BackupReading.NotABackup, repository.restore(uri))
        assertEquals(BackupReading.Unreadable, repository.restore(Uri.fromFile(File(context.cacheDir, "missing.json")).toString()))

        assertSameRows(original, repository.readTables())
    }

    @Test
    fun aFailedWriteRollsEverythingBack() = runTest {
        val broken = other.copy(participants = listOf(ParticipantEntity(id(), other.parties[0].id, "Zoé", 0, playerId = id())))

        assertTrue(runCatching { repository.replaceTables(broken) }.isFailure)

        assertSameRows(original, repository.readTables())
    }
}

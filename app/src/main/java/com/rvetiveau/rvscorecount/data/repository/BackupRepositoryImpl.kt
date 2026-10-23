package com.rvetiveau.rvscorecount.data.repository

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.rvetiveau.rvscorecount.data.backup.BackupFileReading
import com.rvetiveau.rvscorecount.data.backup.BackupTables
import com.rvetiveau.rvscorecount.data.backup.encode
import com.rvetiveau.rvscorecount.data.backup.readBackupFile
import com.rvetiveau.rvscorecount.data.backup.toBackupFile
import com.rvetiveau.rvscorecount.data.db.AppDatabase
import com.rvetiveau.rvscorecount.domain.model.BackupContent
import com.rvetiveau.rvscorecount.domain.model.BackupReading
import com.rvetiveau.rvscorecount.domain.repository.BackupRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext

/**
 * Backup of every table to a JSON file (Story 12.1) and its restore (Story 12.2), both through the
 * Storage Access Framework.
 */
class BackupRepositoryImpl @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val appDatabase: AppDatabase
) : BackupRepository {
    override fun observeContent(): Flow<BackupContent> =
        combine(appDatabase.playerDao().observeCount(), appDatabase.partyDao().observeCount(), ::BackupContent)

    override suspend fun export(destination: String) {
        val text = readTables().toBackupFile(createdAt = System.currentTimeMillis()).encode()
        withContext(Dispatchers.IO) {
            // "wt" truncates: a file picked again must not keep the end of a longer backup.
            val output = context.contentResolver.openOutputStream(Uri.parse(destination), "wt")
                ?: throw IOException("Cannot open $destination")
            output.use { it.write(text.toByteArray(Charsets.UTF_8)) }
        }
    }

    override suspend fun read(source: String): BackupReading = readSource(source).toDomain()

    override suspend fun restore(source: String): BackupReading {
        val reading = readSource(source)
        if (reading is SourceReading.File && reading.reading is BackupFileReading.Valid) {
            replaceTables(reading.reading.tables)
        }
        return reading.toDomain()
    }

    /** Parties go first, taking their rows through CASCADE; then Joueurs, whose seats are gone. */
    internal suspend fun replaceTables(tables: BackupTables) {
        appDatabase.withTransaction {
            appDatabase.partyDao().deleteAll()
            appDatabase.playerDao().deleteAll()
            appDatabase.playerDao().insertAll(tables.players)
            appDatabase.partyDao().insertAll(tables.parties)
            appDatabase.participantDao().insertAll(tables.participants)
            appDatabase.teamDao().insertAll(tables.teams)
            appDatabase.roundDao().insertAll(tables.rounds)
            appDatabase.roundScoreDao().insertAll(tables.roundScores)
            appDatabase.beloteRoundDao().insertAll(tables.beloteRounds)
            appDatabase.tarotRoundDao().insertAll(tables.tarotRounds)
        }
    }

    private suspend fun readSource(source: String): SourceReading = withContext(Dispatchers.IO) {
        val text = try {
            context.contentResolver.openInputStream(Uri.parse(source))?.use { it.readBytes().toString(Charsets.UTF_8) }
        } catch (_: IOException) {
            null
        } catch (_: SecurityException) {
            null
        }
        if (text == null) SourceReading.Unreadable else SourceReading.File(readBackupFile(text))
    }

    private sealed interface SourceReading {
        data object Unreadable : SourceReading
        data class File(val reading: BackupFileReading) : SourceReading
    }

    private fun SourceReading.toDomain(): BackupReading = when (this) {
        SourceReading.Unreadable -> BackupReading.Unreadable
        is SourceReading.File -> when (reading) {
            is BackupFileReading.Valid -> BackupReading.Valid(
                createdAt = reading.file.createdAt,
                content = BackupContent(players = reading.file.players.size, parties = reading.file.parties.size)
            )
            BackupFileReading.NotABackup -> BackupReading.NotABackup
            BackupFileReading.NewerFormat -> BackupReading.NewerFormat
            BackupFileReading.Damaged -> BackupReading.Damaged
        }
    }

    /** One transaction, so a Partie and its Manches are read in the same state. */
    internal suspend fun readTables(): BackupTables = appDatabase.withTransaction {
        BackupTables(
            players = appDatabase.playerDao().getAll(),
            parties = appDatabase.partyDao().getAll(),
            participants = appDatabase.participantDao().getAll(),
            teams = appDatabase.teamDao().getAll(),
            rounds = appDatabase.roundDao().getAll(),
            roundScores = appDatabase.roundScoreDao().getAll(),
            beloteRounds = appDatabase.beloteRoundDao().getAll(),
            tarotRounds = appDatabase.tarotRoundDao().getAll()
        )
    }
}

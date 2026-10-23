package com.rvetiveau.rvscorecount.data.repository

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.rvetiveau.rvscorecount.data.backup.BackupTables
import com.rvetiveau.rvscorecount.data.backup.encode
import com.rvetiveau.rvscorecount.data.backup.toBackupFile
import com.rvetiveau.rvscorecount.data.db.AppDatabase
import com.rvetiveau.rvscorecount.domain.model.BackupContent
import com.rvetiveau.rvscorecount.domain.repository.BackupRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext

/** Backup of every table to a JSON file (Story 12.1), written through the Storage Access Framework. */
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

    /** One transaction, so a Partie and its Manches are read in the same state. */
    internal suspend fun readTables(): BackupTables = appDatabase.withTransaction {
        BackupTables(
            players = appDatabase.playerDao().getAll(),
            parties = appDatabase.partyDao().getAll(),
            participants = appDatabase.participantDao().getAll(),
            teams = appDatabase.teamDao().getAll(),
            rounds = appDatabase.roundDao().getAll(),
            roundScores = appDatabase.roundScoreDao().getAll(),
            beloteRounds = appDatabase.beloteRoundDao().getAll()
        )
    }
}

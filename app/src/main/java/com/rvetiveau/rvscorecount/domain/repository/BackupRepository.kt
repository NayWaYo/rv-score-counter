package com.rvetiveau.rvscorecount.domain.repository

import com.rvetiveau.rvscorecount.domain.model.BackupContent
import com.rvetiveau.rvscorecount.domain.model.BackupReading
import kotlinx.coroutines.flow.Flow

/** Saves the whole database to a file the user chose, and puts it back (Epic 12). */
interface BackupRepository {
    fun observeContent(): Flow<BackupContent>

    /**
     * Writes every Joueur and Partie to the document [destination], the content URI the system file
     * picker returned. Throws when it cannot be written.
     */
    suspend fun export(destination: String)

    /** Reads and checks the whole document [source] without changing anything. */
    suspend fun read(source: String): BackupReading

    /**
     * Reads and checks [source] again, then, when it is [BackupReading.Valid], replaces every Joueur and
     * Partie with its content in one transaction. Throws when the write fails, leaving the data unchanged.
     */
    suspend fun restore(source: String): BackupReading
}

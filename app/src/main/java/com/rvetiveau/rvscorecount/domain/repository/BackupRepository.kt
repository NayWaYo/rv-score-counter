package com.rvetiveau.rvscorecount.domain.repository

import com.rvetiveau.rvscorecount.domain.model.BackupContent
import kotlinx.coroutines.flow.Flow

/** Saves the whole database to a file the user chose (Epic 12). */
interface BackupRepository {
    fun observeContent(): Flow<BackupContent>

    /**
     * Writes every Joueur and Partie to the document [destination], the content URI the system file
     * picker returned. Throws when it cannot be written.
     */
    suspend fun export(destination: String)
}

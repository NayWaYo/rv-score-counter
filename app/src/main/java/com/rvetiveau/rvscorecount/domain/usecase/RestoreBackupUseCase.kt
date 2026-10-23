package com.rvetiveau.rvscorecount.domain.usecase

import com.rvetiveau.rvscorecount.domain.model.BackupReading
import com.rvetiveau.rvscorecount.domain.repository.BackupRepository
import javax.inject.Inject

/** Replaces every Joueur and Partie with the backup [source] (Story 12.2); throws when the write fails. */
class RestoreBackupUseCase @Inject constructor(
    private val backupRepository: BackupRepository
) {
    suspend operator fun invoke(source: String): BackupReading = backupRepository.restore(source)
}

package com.rvetiveau.rvscorecount.domain.usecase

import com.rvetiveau.rvscorecount.domain.model.BackupReading
import com.rvetiveau.rvscorecount.domain.repository.BackupRepository
import javax.inject.Inject

/** Checks the backup [source] before the restore is confirmed (Story 12.2). */
class ReadBackupUseCase @Inject constructor(
    private val backupRepository: BackupRepository
) {
    suspend operator fun invoke(source: String): BackupReading = backupRepository.read(source)
}

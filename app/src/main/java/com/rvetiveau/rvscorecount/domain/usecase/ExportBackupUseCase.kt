package com.rvetiveau.rvscorecount.domain.usecase

import com.rvetiveau.rvscorecount.domain.repository.BackupRepository
import javax.inject.Inject

/** Saves every Joueur and Partie to the document [destination] the user chose (Story 12.1); throws when it fails. */
class ExportBackupUseCase @Inject constructor(
    private val backupRepository: BackupRepository
) {
    suspend operator fun invoke(destination: String) = backupRepository.export(destination)
}

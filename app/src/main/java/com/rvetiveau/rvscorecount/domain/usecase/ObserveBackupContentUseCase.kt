package com.rvetiveau.rvscorecount.domain.usecase

import com.rvetiveau.rvscorecount.domain.model.BackupContent
import com.rvetiveau.rvscorecount.domain.repository.BackupRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/** What the « Sauvegarde » screen announces (Story 12.1). */
class ObserveBackupContentUseCase @Inject constructor(
    private val backupRepository: BackupRepository
) {
    operator fun invoke(): Flow<BackupContent> = backupRepository.observeContent()
}

package com.rvetiveau.rvscorecount.domain.usecase

import com.rvetiveau.rvscorecount.domain.model.Party
import com.rvetiveau.rvscorecount.domain.repository.PartyRepository
import javax.inject.Inject

/**
 * Finds the Party to resume at app launch (FR-16, AD-6). A repository failure
 * must propagate — it is not equivalent to "no party in progress".
 */
class ResumePartyUseCase @Inject constructor(
    private val partyRepository: PartyRepository
) {
    suspend operator fun invoke(): Party? = partyRepository.getPartyInProgress()
}

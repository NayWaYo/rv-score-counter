package com.rvetiveau.rvscorecount.domain.usecase

import com.rvetiveau.rvscorecount.domain.model.distinctGameNames
import com.rvetiveau.rvscorecount.domain.repository.FreeGameRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Game names suggested when creating a Partie of Jeu libre (Story 11.2): once each, the most recent first. */
class ObserveFreeGameNamesUseCase @Inject constructor(
    private val freeGameRepository: FreeGameRepository
) {
    operator fun invoke(): Flow<List<String>> = freeGameRepository.observeGameNames().map { it.distinctGameNames() }
}

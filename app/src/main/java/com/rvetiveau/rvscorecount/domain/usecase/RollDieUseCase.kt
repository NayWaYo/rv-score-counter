package com.rvetiveau.rvscorecount.domain.usecase

import com.rvetiveau.rvscorecount.domain.model.CardSuit
import javax.inject.Inject
import kotlin.random.Random

/** Uniform draw among the four suits (FR-5); a caller-supplied [Random] keeps this testable. */
open class RollDieUseCase @Inject constructor() {
    open operator fun invoke(random: Random = Random.Default): CardSuit {
        val index = random.nextInt(CardSuit.entries.size)
        return CardSuit.entries[index]
    }
}

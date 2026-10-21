package com.rvetiveau.rvscorecount.domain.usecase

import com.rvetiveau.rvscorecount.domain.engine.BeloteRules
import com.rvetiveau.rvscorecount.domain.model.BelotePartyStatus
import com.rvetiveau.rvscorecount.domain.model.BeloteRoundInput
import com.rvetiveau.rvscorecount.domain.repository.BeloteRepository
import com.rvetiveau.rvscorecount.domain.repository.PartyRepository
import java.util.UUID
import javax.inject.Inject

/**
 * Corrects what was entered for the validated Manche [roundNumber] of a Partie of Belote in
 * progress; the following Manches are rescored by the engine. When the correction reaches the end
 * rule, the Partie is FINISHED in the same transaction. The Donneur does not move.
 */
class CorrectBeloteRoundUseCase @Inject constructor(
    private val partyRepository: PartyRepository,
    private val beloteRepository: BeloteRepository,
    private val rules: BeloteRules
) {
    /** [takerPlayerId] is required, also for a Manche played before it was recorded (Story 8.3). */
    suspend operator fun invoke(
        partyId: UUID,
        roundNumber: Int,
        input: BeloteRoundInput,
        takerPlayerId: UUID
    ): Result<BelotePartyStatus> =
        runBeloteRoundWrite {
            val state = loadPartyInProgress(partyId, partyRepository, beloteRepository, rules)
            requireTaker(state, input, takerPlayerId)
            val round = state.rounds.firstOrNull { it.roundNumber == roundNumber }
                ?: throw InvalidBeloteRoundException("Party $partyId has no round $roundNumber")
            val inputs = state.rounds.map { if (it.roundNumber == roundNumber) input else it.input }
            val status = statusAfter(inputs, state.endRule, rules)
            persist {
                beloteRepository.correctRound(
                    round.copy(input = input, takerPlayerId = takerPlayerId),
                    state.party.copy(status = status.toPartyState())
                )
            }
            status
        }
}

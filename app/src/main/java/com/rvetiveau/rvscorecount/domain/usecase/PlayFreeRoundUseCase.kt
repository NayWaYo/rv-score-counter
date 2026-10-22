package com.rvetiveau.rvscorecount.domain.usecase

import com.rvetiveau.rvscorecount.domain.engine.FreeGameRules
import com.rvetiveau.rvscorecount.domain.model.FreePartyState
import com.rvetiveau.rvscorecount.domain.model.FreePartyStatus
import com.rvetiveau.rvscorecount.domain.model.FreeRound
import com.rvetiveau.rvscorecount.domain.model.PartyState
import com.rvetiveau.rvscorecount.domain.repository.FreeGameRepository
import com.rvetiveau.rvscorecount.domain.repository.PartyRepository
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.CancellationException

sealed class FreeRoundException(message: String, cause: Throwable? = null) : Exception(message, cause)

class InvalidFreeRoundException(message: String, cause: Throwable? = null) : FreeRoundException(message, cause)

class FreeRoundPersistenceException(cause: Throwable) : FreeRoundException("Jeu libre round persistence failed", cause)

/**
 * Validates the next Manche of a Partie of Jeu libre in progress (Story 11.3): the Manche, the next
 * Donneur and, when the Manche reaches the end rule, the FINISHED status are written in one
 * transaction. Returns the Partie's status after the Manche.
 */
class PlayFreeRoundUseCase @Inject constructor(
    private val partyRepository: PartyRepository,
    private val freeGameRepository: FreeGameRepository,
    private val rules: FreeGameRules
) {
    /** [scores]: one score per Participant of the Partie, negative included. */
    suspend operator fun invoke(partyId: UUID, scores: Map<UUID, Int>): Result<FreePartyStatus> = runFreeRoundWrite {
        val state = loadFreePartyInProgress(partyId, partyRepository, freeGameRepository, rules)
        requireValidRound(state, scores, rules)
        val round = FreeRound(UUID.randomUUID(), partyId, state.rounds.size + 1, scores)
        val status = statusAfter(state, state.rounds + round, rules)
        val updatedParty = state.party.copy(
            currentDealerIndex = (state.party.currentDealerIndex + 1) % state.participants.size,
            status = status.toPartyState()
        )
        persistFreeRound { freeGameRepository.saveRound(round, updatedParty) }
        status
    }
}

/**
 * Corrects the scores of the validated Manche [roundNumber] of a Partie of Jeu libre in progress
 * (Story 11.3). When the correction reaches the end rule, the Partie is FINISHED in the same
 * transaction. The Donneur does not move.
 */
class CorrectFreeRoundUseCase @Inject constructor(
    private val partyRepository: PartyRepository,
    private val freeGameRepository: FreeGameRepository,
    private val rules: FreeGameRules
) {
    suspend operator fun invoke(partyId: UUID, roundNumber: Int, scores: Map<UUID, Int>): Result<FreePartyStatus> =
        runFreeRoundWrite {
            val state = loadFreePartyInProgress(partyId, partyRepository, freeGameRepository, rules)
            requireValidRound(state, scores, rules)
            val round = state.rounds.firstOrNull { it.roundNumber == roundNumber }
                ?: throw InvalidFreeRoundException("Party $partyId has no round $roundNumber")
            val corrected = round.copy(scores = scores)
            val status = statusAfter(state, state.rounds.map { if (it.id == round.id) corrected else it }, rules)
            persistFreeRound { freeGameRepository.correctRound(corrected, state.party.copy(status = status.toPartyState())) }
            status
        }
}

private suspend fun loadFreePartyInProgress(
    partyId: UUID,
    partyRepository: PartyRepository,
    freeGameRepository: FreeGameRepository,
    rules: FreeGameRules
): FreePartyState = try {
    loadFreeParty(partyId, partyRepository, freeGameRepository, rules)
        ?.takeIf { it.party.status == PartyState.IN_PROGRESS }
        ?: throw InvalidFreeRoundException("Party $partyId is not a Jeu libre party in progress")
} catch (incoherent: IllegalStateException) {
    throw InvalidFreeRoundException("Party $partyId is not a coherent Jeu libre party", incoherent)
}

private fun requireValidRound(state: FreePartyState, scores: Map<UUID, Int>, rules: FreeGameRules) {
    if (!rules.isValidRound(scores, state.participants)) {
        throw InvalidFreeRoundException("A Manche of Jeu libre needs one score within ${rules.roundScores} per Participant")
    }
}

/** The engine's status once [rounds] are played; totals beyond [Int] make the Manche invalid. */
private fun statusAfter(state: FreePartyState, rounds: List<FreeRound>, rules: FreeGameRules): FreePartyStatus = try {
    rules.partyStatus(state.participants, rounds, state.winDirection, state.party.endRule)
} catch (overflow: ArithmeticException) {
    throw InvalidFreeRoundException("The totals of the Partie overflow", overflow)
}

private fun FreePartyStatus.toPartyState(): PartyState = when (this) {
    FreePartyStatus.InProgress -> PartyState.IN_PROGRESS
    is FreePartyStatus.Finished -> PartyState.FINISHED
}

private suspend fun persistFreeRound(write: suspend () -> Unit) {
    try {
        write()
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (failure: Throwable) {
        throw FreeRoundPersistenceException(failure)
    }
}

private suspend fun <T> runFreeRoundWrite(block: suspend () -> T): Result<T> = try {
    Result.success(block())
} catch (cancellation: CancellationException) {
    throw cancellation
} catch (failure: Throwable) {
    Result.failure(failure)
}

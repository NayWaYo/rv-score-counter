package com.rvetiveau.rvscorecount.domain.usecase

import com.rvetiveau.rvscorecount.domain.engine.TarotRules
import com.rvetiveau.rvscorecount.domain.model.PartyState
import com.rvetiveau.rvscorecount.domain.model.TarotPartyState
import com.rvetiveau.rvscorecount.domain.model.TarotPartyStatus
import com.rvetiveau.rvscorecount.domain.model.TarotRound
import com.rvetiveau.rvscorecount.domain.model.TarotRoundInput
import com.rvetiveau.rvscorecount.domain.repository.PartyRepository
import com.rvetiveau.rvscorecount.domain.repository.TarotRepository
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.CancellationException

sealed class TarotRoundException(message: String, cause: Throwable? = null) : Exception(message, cause)

class InvalidTarotRoundException(message: String, cause: Throwable? = null) : TarotRoundException(message, cause)

class TarotRoundPersistenceException(cause: Throwable) : TarotRoundException("Tarot round persistence failed", cause)

/**
 * Validates the next Manche of a Partie of Tarot in progress (Story 13.3): the Manche, the next
 * Donneur and, after the last Manche planned, the FINISHED status are written in one transaction.
 * Returns the Partie's status after the Manche.
 */
class PlayTarotRoundUseCase @Inject constructor(
    private val partyRepository: PartyRepository,
    private val tarotRepository: TarotRepository,
    private val rules: TarotRules
) {
    suspend operator fun invoke(partyId: UUID, input: TarotRoundInput): Result<TarotPartyStatus> = runTarotRoundWrite {
        val state = loadTarotPartyInProgress(partyId, partyRepository, tarotRepository, rules)
        requireValidInput(state, input, rules)
        val round = TarotRound(UUID.randomUUID(), partyId, state.rounds.size + 1, input)
        val status = rules.partyStatus(state.participants, state.rounds + round, state.party.endRule)
        val updatedParty = state.party.copy(
            currentDealerIndex = (state.party.currentDealerIndex + 1) % state.participants.size,
            status = status.toPartyState()
        )
        persistTarotRound { tarotRepository.saveRound(round, updatedParty) }
        status
    }
}

/**
 * Corrects what was entered for the validated Manche [roundNumber] of a Partie of Tarot in progress
 * (Story 13.3). When the correction reaches the end, the Partie is FINISHED in the same transaction.
 * The Donneur does not move.
 */
class CorrectTarotRoundUseCase @Inject constructor(
    private val partyRepository: PartyRepository,
    private val tarotRepository: TarotRepository,
    private val rules: TarotRules
) {
    suspend operator fun invoke(partyId: UUID, roundNumber: Int, input: TarotRoundInput): Result<TarotPartyStatus> =
        runTarotRoundWrite {
            val state = loadTarotPartyInProgress(partyId, partyRepository, tarotRepository, rules)
            requireValidInput(state, input, rules)
            val round = state.rounds.firstOrNull { it.roundNumber == roundNumber }
                ?: throw InvalidTarotRoundException("Party $partyId has no round $roundNumber")
            val corrected = round.copy(input = input)
            val rounds = state.rounds.map { if (it.id == round.id) corrected else it }
            val status = rules.partyStatus(state.participants, rounds, state.party.endRule)
            persistTarotRound { tarotRepository.correctRound(corrected, state.party.copy(status = status.toPartyState())) }
            status
        }
}

private suspend fun loadTarotPartyInProgress(
    partyId: UUID,
    partyRepository: PartyRepository,
    tarotRepository: TarotRepository,
    rules: TarotRules
): TarotPartyState = try {
    loadTarotParty(partyId, partyRepository, tarotRepository, rules)
        ?.takeIf { it.party.status == PartyState.IN_PROGRESS }
        ?: throw InvalidTarotRoundException("Party $partyId is not a Tarot party in progress")
} catch (incoherent: IllegalStateException) {
    throw InvalidTarotRoundException("Party $partyId is not a coherent Tarot party", incoherent)
}

private fun requireValidInput(state: TarotPartyState, input: TarotRoundInput, rules: TarotRules) {
    rules.inputError(input, state.participants)?.let { error -> throw InvalidTarotRoundException("Impossible Manche: $error") }
}

private fun TarotPartyStatus.toPartyState(): PartyState = when (this) {
    TarotPartyStatus.InProgress -> PartyState.IN_PROGRESS
    is TarotPartyStatus.Finished -> PartyState.FINISHED
}

private suspend fun persistTarotRound(write: suspend () -> Unit) {
    try {
        write()
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (failure: Throwable) {
        throw TarotRoundPersistenceException(failure)
    }
}

private suspend fun <T> runTarotRoundWrite(block: suspend () -> T): Result<T> = try {
    Result.success(block())
} catch (cancellation: CancellationException) {
    throw cancellation
} catch (failure: Throwable) {
    Result.failure(failure)
}

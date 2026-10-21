package com.rvetiveau.rvscorecount.domain.usecase

import com.rvetiveau.rvscorecount.domain.engine.BeloteRules
import com.rvetiveau.rvscorecount.domain.model.BeloteEndRule
import com.rvetiveau.rvscorecount.domain.model.BelotePartyStatus
import com.rvetiveau.rvscorecount.domain.model.BeloteRound
import com.rvetiveau.rvscorecount.domain.model.BeloteRoundInput
import com.rvetiveau.rvscorecount.domain.model.PartyState
import com.rvetiveau.rvscorecount.domain.repository.BeloteRepository
import com.rvetiveau.rvscorecount.domain.repository.PartyRepository
import kotlinx.coroutines.CancellationException
import java.util.UUID
import javax.inject.Inject

sealed class BeloteRoundException(message: String, cause: Throwable? = null) : Exception(message, cause)

class InvalidBeloteRoundException(message: String, cause: Throwable? = null) : BeloteRoundException(message, cause)

class BeloteRoundPersistenceException(cause: Throwable) : BeloteRoundException("Belote round persistence failed", cause)

/**
 * Validates the next Manche of a Partie of Belote in progress: the Manche, the next Donneur and,
 * when the Manche reaches the end rule, the FINISHED status are written in one transaction.
 * Returns the Partie's status after the Manche.
 */
class PlayBeloteRoundUseCase @Inject constructor(
    private val partyRepository: PartyRepository,
    private val beloteRepository: BeloteRepository,
    private val rules: BeloteRules
) {
    suspend operator fun invoke(partyId: UUID, input: BeloteRoundInput): Result<BelotePartyStatus> =
        runBeloteRoundWrite {
            val state = loadPartyInProgress(partyId, partyRepository, beloteRepository, rules)
            val status = statusAfter(state.rounds.map { it.input } + input, state.endRule, rules)
            val round = BeloteRound(UUID.randomUUID(), partyId, state.rounds.size + 1, input)
            val updatedParty = state.party.copy(
                currentDealerIndex = (state.party.currentDealerIndex + 1) % state.players.size,
                status = status.toPartyState()
            )
            persist { beloteRepository.saveRound(round, updatedParty) }
            status
        }
}

internal suspend fun loadPartyInProgress(
    partyId: UUID,
    partyRepository: PartyRepository,
    beloteRepository: BeloteRepository,
    rules: BeloteRules
) = try {
    loadBeloteParty(partyId, partyRepository, beloteRepository, rules)
        ?.takeIf { it.party.status == PartyState.IN_PROGRESS }
        ?: throw InvalidBeloteRoundException("Party $partyId is not a Belote party in progress")
} catch (incoherent: IllegalStateException) {
    throw InvalidBeloteRoundException("Party $partyId is not a coherent Belote party", incoherent)
}

/** The engine's status once [inputs] are played; an input it refuses is an invalid Manche. */
internal fun statusAfter(
    inputs: List<BeloteRoundInput>,
    endRule: BeloteEndRule,
    rules: BeloteRules
): BelotePartyStatus = try {
    rules.partyStatus(rules.scoreParty(inputs), endRule)
} catch (refused: IllegalArgumentException) {
    throw InvalidBeloteRoundException("The engine refused a Belote round", refused)
}

internal fun BelotePartyStatus.toPartyState(): PartyState = when (this) {
    BelotePartyStatus.InProgress -> PartyState.IN_PROGRESS
    is BelotePartyStatus.Won -> PartyState.FINISHED
}

internal suspend fun persist(write: suspend () -> Unit) {
    try {
        write()
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (failure: Throwable) {
        throw BeloteRoundPersistenceException(failure)
    }
}

internal suspend fun <T> runBeloteRoundWrite(block: suspend () -> T): Result<T> = try {
    Result.success(block())
} catch (cancellation: CancellationException) {
    throw cancellation
} catch (failure: Throwable) {
    Result.failure(failure)
}

package com.rvetiveau.rvscorecount.domain.usecase

import com.rvetiveau.rvscorecount.domain.model.PartyState
import com.rvetiveau.rvscorecount.domain.repository.PartyRepository
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.CancellationException

sealed class ClosePartyException(message: String, cause: Throwable? = null) : Exception(message, cause)

class InvalidPartyClosureException(message: String) : ClosePartyException(message)

class PartyClosurePersistenceException(cause: Throwable) :
    ClosePartyException("Party closure persistence failed", cause)

/**
 * Clôt une Partie en cours (FR-12) : IN_PROGRESS -> FINISHED. Une Partie FINISHED ne reçoit plus
 * de Manche ni de Participant et n'est plus reprise au lancement.
 */
class ClosePartyUseCase @Inject constructor(
    private val partyRepository: PartyRepository
) {
    suspend operator fun invoke(partyId: UUID): Result<Unit> {
        return try {
            val party = partyRepository.getPartyById(partyId)
                ?: throw InvalidPartyClosureException("Party $partyId not found")
            if (party.status != PartyState.IN_PROGRESS) {
                throw InvalidPartyClosureException("Party $partyId is ${party.status}, not IN_PROGRESS")
            }
            try {
                partyRepository.updateParty(party.copy(status = PartyState.FINISHED))
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (failure: Throwable) {
                throw PartyClosurePersistenceException(failure)
            }
            Result.success(Unit)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }
}

package com.rvetiveau.rvscorecount.domain.usecase

import com.rvetiveau.rvscorecount.domain.model.PartyState
import com.rvetiveau.rvscorecount.domain.repository.PartyRepository
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.CancellationException

sealed class DeletePartyException(message: String, cause: Throwable? = null) : Exception(message, cause)

class InvalidPartyDeletionException(message: String) : DeletePartyException(message)

class PartyDeletionPersistenceException(cause: Throwable) :
    DeletePartyException("Party deletion persistence failed", cause)

/**
 * Suppression définitive d'une Partie de l'Historique (Story 6.1). Seule une Partie FINISHED peut
 * être supprimée : une Partie en cours se termine d'abord par « Fin de partie ».
 */
class DeletePartyUseCase @Inject constructor(
    private val partyRepository: PartyRepository
) {
    suspend operator fun invoke(partyId: UUID): Result<Unit> {
        return try {
            val party = partyRepository.getPartyById(partyId)
                ?: throw InvalidPartyDeletionException("Party $partyId not found")
            if (party.status != PartyState.FINISHED) {
                throw InvalidPartyDeletionException("Party $partyId is ${party.status}, not FINISHED")
            }
            try {
                partyRepository.deleteParty(partyId)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (failure: Throwable) {
                throw PartyDeletionPersistenceException(failure)
            }
            Result.success(Unit)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }
}

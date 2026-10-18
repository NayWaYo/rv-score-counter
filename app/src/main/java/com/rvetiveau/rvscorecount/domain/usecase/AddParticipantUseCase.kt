package com.rvetiveau.rvscorecount.domain.usecase

import com.rvetiveau.rvscorecount.domain.engine.GameEngine
import com.rvetiveau.rvscorecount.domain.model.Participant
import com.rvetiveau.rvscorecount.domain.model.PartyState
import com.rvetiveau.rvscorecount.domain.repository.PartyRepository
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.CancellationException

sealed class AddParticipantException(message: String, cause: Throwable? = null) : Exception(message, cause)

class InvalidParticipantAdditionException(message: String) : AddParticipantException(message)

class ParticipantPersistenceException(cause: Throwable) :
    AddParticipantException("Participant persistence failed", cause)

/**
 * Index du prochain Donneur après l'insertion d'un Participant à [insertPosition] (PRD FR-4) :
 * inséré juste après le prochain Donneur, le nouveau Participant le devient ; sinon le prochain
 * Donneur reste le même Participant, décalé d'un rang s'il était à ou après la place d'insertion.
 */
fun nextDealerIndexAfterInsertion(currentDealerIndex: Int, insertPosition: Int): Int = when {
    insertPosition == currentDealerIndex + 1 -> insertPosition
    insertPosition <= currentDealerIndex -> currentDealerIndex + 1
    else -> currentDealerIndex
}

/**
 * Ajoute un Participant en cours de Partie à la place choisie (Story 4.2). Valide la Partie et le
 * nombre maximal du moteur, puis délègue au repository l'insertion, le décalage des places et la
 * mise à jour du Donneur en une seule transaction.
 */
class AddParticipantUseCase @Inject constructor(
    private val partyRepository: PartyRepository,
    private val engines: Map<String, @JvmSuppressWildcards GameEngine>
) {
    suspend operator fun invoke(partyId: UUID, name: String, position: Int): Result<Participant> {
        return try {
            val trimmed = name.trim()
            if (trimmed.isBlank()) throw InvalidParticipantAdditionException("Participant name must not be blank")

            val party = partyRepository.getPartyById(partyId)
                ?: throw InvalidParticipantAdditionException("Party $partyId not found")
            if (party.status != PartyState.IN_PROGRESS) {
                throw InvalidParticipantAdditionException("Party $partyId is ${party.status}, not IN_PROGRESS")
            }
            val engine = engines[party.gameId]?.takeIf { it.gameId == party.gameId }
                ?: throw InvalidParticipantAdditionException("No GameEngine registered for gameId '${party.gameId}'")

            val participants = partyRepository.getParticipantsByPartyId(partyId)
            if (participants.map { it.listPosition }.sorted() != participants.indices.toList()) {
                throw InvalidParticipantAdditionException("Participant positions are not continuous")
            }
            if (participants.size >= engine.maxPlayers) {
                throw InvalidParticipantAdditionException("Party already has ${engine.maxPlayers} participants")
            }
            if (position !in 0..participants.size) {
                throw InvalidParticipantAdditionException("Position $position is outside [0, ${participants.size}]")
            }
            if (party.currentDealerIndex !in participants.indices) {
                throw InvalidParticipantAdditionException(
                    "currentDealerIndex ${party.currentDealerIndex} is out of bounds for ${participants.size} participants"
                )
            }

            val participant = Participant(
                id = UUID.randomUUID(),
                partyId = partyId,
                name = trimmed,
                listPosition = position
            )
            val updatedParty = party.copy(
                currentDealerIndex = nextDealerIndexAfterInsertion(party.currentDealerIndex, position)
            )
            try {
                partyRepository.addParticipant(participant, updatedParty)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (failure: Throwable) {
                throw ParticipantPersistenceException(failure)
            }
            Result.success(participant)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }
}

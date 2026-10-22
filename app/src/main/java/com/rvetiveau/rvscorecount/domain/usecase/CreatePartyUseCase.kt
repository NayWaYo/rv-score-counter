package com.rvetiveau.rvscorecount.domain.usecase

import com.rvetiveau.rvscorecount.domain.engine.GameEngine
import com.rvetiveau.rvscorecount.domain.model.Participant
import com.rvetiveau.rvscorecount.domain.model.ParticipantCreationData
import com.rvetiveau.rvscorecount.domain.model.Party
import com.rvetiveau.rvscorecount.domain.model.PartyState
import com.rvetiveau.rvscorecount.domain.model.duplicatePlayerName
import com.rvetiveau.rvscorecount.domain.repository.PartyRepository
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.CancellationException

sealed class CreatePartyException(message: String, cause: Throwable? = null) : Exception(message, cause)

class InvalidPartyConfigurationException(message: String) : CreatePartyException(message)

class PartyCreationPersistenceException(cause: Throwable) :
    CreatePartyException("Party aggregate persistence failed", cause)

/**
 * Creates a Party and its Participants as a single atomic aggregate (AC 1-3, 8-9). Validates
 * against the requested Game's registered engine, decides the SETUP -> IN_PROGRESS transition
 * here in the domain, and delegates the write to a single repository transaction; it never
 * loops over independent participant writes.
 */
class CreatePartyUseCase @Inject constructor(
    private val partyRepository: PartyRepository,
    private val engines: Map<String, @JvmSuppressWildcards GameEngine>
) {
    suspend operator fun invoke(gameId: String, participants: List<ParticipantCreationData>): Result<UUID> {
        return try {
            val engine = engines[gameId]
                ?: throw InvalidPartyConfigurationException("No GameEngine registered for gameId '$gameId'")
            if (engine.gameId != gameId) {
                throw InvalidPartyConfigurationException(
                    "Engine registry mismatch: key '$gameId' does not match engine.gameId '${engine.gameId}'"
                )
            }

            val normalized = participants.map { it.copy(name = it.name.trim()) }
            if (normalized.size !in engine.minPlayers..engine.maxPlayers) {
                throw InvalidPartyConfigurationException(
                    "Participant count ${normalized.size} is out of bounds " +
                        "[${engine.minPlayers}, ${engine.maxPlayers}] for gameId '$gameId'"
                )
            }
            if (normalized.any { it.name.isBlank() }) {
                throw InvalidPartyConfigurationException("Participant names must not be blank")
            }
            duplicatePlayerName(normalized.map { it.name })?.let { name ->
                throw InvalidPartyConfigurationException("'$name' is already in the party")
            }
            if (normalized.map { it.id }.toSet().size != normalized.size) {
                throw InvalidPartyConfigurationException("Participant ids must be unique")
            }

            val partyId = UUID.randomUUID()
            val createdAt = System.currentTimeMillis()
            // SETUP -> IN_PROGRESS is decided here, in the domain, before the single atomic write (AC 1).
            val setupParty = Party(
                id = partyId,
                gameId = gameId,
                status = PartyState.SETUP,
                currentDealerIndex = 0,
                createdAt = createdAt
            )
            val party = setupParty.copy(status = PartyState.IN_PROGRESS)

            val entities = normalized.mapIndexed { index, data ->
                Participant(id = data.id, partyId = partyId, name = data.name, listPosition = index)
            }

            try {
                partyRepository.createPartyWithParticipants(party, entities)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (failure: Throwable) {
                throw PartyCreationPersistenceException(failure)
            }
            Result.success(partyId)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    /**
     * Relance rapide (FR-14) : nouvelle Partie du même Jeu avec les mêmes prénoms dans le même
     * ordre, sous de nouveaux identifiants, via la même validation et la même écriture atomique.
     * Seule une Partie FINISHED sert de référence.
     */
    suspend fun fromParty(referencePartyId: UUID): Result<UUID> {
        val (gameId, participants) = try {
            val reference = partyRepository.getPartyById(referencePartyId)
                ?: throw InvalidPartyConfigurationException("Reference party $referencePartyId not found")
            if (reference.status != PartyState.FINISHED) {
                throw InvalidPartyConfigurationException(
                    "Reference party $referencePartyId is ${reference.status}, not FINISHED"
                )
            }
            reference.gameId to partyRepository.getParticipantsByPartyId(referencePartyId)
                .sortedBy { it.listPosition }
                .map { ParticipantCreationData(id = UUID.randomUUID(), name = it.name) }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (t: Throwable) {
            return Result.failure(t)
        }
        return invoke(gameId = gameId, participants = participants)
    }
}

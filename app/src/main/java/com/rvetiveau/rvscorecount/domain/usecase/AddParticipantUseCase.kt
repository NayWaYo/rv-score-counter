package com.rvetiveau.rvscorecount.domain.usecase

import com.rvetiveau.rvscorecount.domain.model.Participant
import com.rvetiveau.rvscorecount.domain.repository.PartyRepository
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.CancellationException

/**
 * Adds a single Participant to an existing Party (AC 8-9). Unitary business action, reusable
 * as-is by the future in-progress add-participant flow (Story 4.2); it does not anticipate the
 * position picker or dealer rotation rules owned by that story.
 */
class AddParticipantUseCase @Inject constructor(
    private val partyRepository: PartyRepository
) {
    suspend operator fun invoke(partyId: UUID, name: String): Result<Participant> {
        return try {
            val trimmed = name.trim()
            require(trimmed.isNotBlank()) { "Participant name must not be blank" }

            val listPosition = partyRepository.getParticipantsByPartyId(partyId).size
            val participant = Participant(
                id = UUID.randomUUID(),
                partyId = partyId,
                name = trimmed,
                listPosition = listPosition
            )
            partyRepository.addParticipant(participant)
            Result.success(participant)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }
}

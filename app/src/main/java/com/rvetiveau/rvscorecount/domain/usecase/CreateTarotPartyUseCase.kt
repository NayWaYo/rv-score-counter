package com.rvetiveau.rvscorecount.domain.usecase

import com.rvetiveau.rvscorecount.domain.engine.TarotRules
import com.rvetiveau.rvscorecount.domain.model.EndRule
import com.rvetiveau.rvscorecount.domain.model.Participant
import com.rvetiveau.rvscorecount.domain.model.ParticipantCreationData
import com.rvetiveau.rvscorecount.domain.model.Party
import com.rvetiveau.rvscorecount.domain.model.PartyState
import com.rvetiveau.rvscorecount.domain.model.duplicatePlayerName
import com.rvetiveau.rvscorecount.domain.repository.PartyRepository
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.CancellationException

/** What a Partie of Tarot is created with (Story 13.3): its end, by hand when null, and its seats in table order. */
data class TarotPartyDraft(
    val endRule: EndRule.RoundCount?,
    val participants: List<ParticipantCreationData>
)

/**
 * Creates a Partie of Tarot (AD-10) with its Participants in one write, IN_PROGRESS, the first seat
 * dealing. An invalid draft fails with [InvalidPartyConfigurationException].
 */
class CreateTarotPartyUseCase @Inject constructor(
    private val partyRepository: PartyRepository,
    private val rules: TarotRules
) {
    suspend operator fun invoke(draft: TarotPartyDraft): Result<UUID> = try {
        draft.endRule?.let { endRule ->
            if (endRule.rounds > MAX_ROUND_COUNT) {
                throw InvalidPartyConfigurationException("A Partie of Tarot has at most $MAX_ROUND_COUNT Manches")
            }
        }
        val seats = draft.participants.map { it.copy(name = it.name.trim()) }
        if (seats.size !in rules.minPlayers..rules.maxPlayers) {
            throw InvalidPartyConfigurationException("The Tarot needs ${rules.minPlayers} to ${rules.maxPlayers} players")
        }
        if (seats.any { it.name.isEmpty() }) throw InvalidPartyConfigurationException("Participant names must not be blank")
        duplicatePlayerName(seats.map { it.name })?.let { throw InvalidPartyConfigurationException("'$it' is already in the party") }
        if (seats.map { it.id }.toSet().size != seats.size) throw InvalidPartyConfigurationException("Participant ids must be unique")

        val party = Party(
            id = UUID.randomUUID(),
            gameId = rules.gameId,
            status = PartyState.IN_PROGRESS,
            currentDealerIndex = 0,
            createdAt = System.currentTimeMillis(),
            endRule = draft.endRule
        )
        try {
            partyRepository.createPartyWithParticipants(
                party,
                seats.mapIndexed { index, seat -> Participant(seat.id, party.id, seat.name, index) }
            )
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (failure: Throwable) {
            throw PartyCreationPersistenceException(failure)
        }
        Result.success(party.id)
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (failure: Throwable) {
        Result.failure(failure)
    }

    /** « Rejouer »: the same Joueurs in the same order and the same end as the FINISHED Partie [referencePartyId]. */
    suspend fun fromParty(referencePartyId: UUID): Result<UUID> {
        val draft = try {
            val reference = partyRepository.getPartyById(referencePartyId)
                ?.takeIf { it.gameId == rules.gameId && it.status == PartyState.FINISHED }
                ?: throw InvalidPartyConfigurationException("Party $referencePartyId is not a finished Tarot party")
            TarotPartyDraft(
                endRule = when (val endRule = reference.endRule) {
                    null -> null
                    is EndRule.RoundCount -> endRule
                    is EndRule.TargetScore -> throw InvalidPartyConfigurationException("A Partie of Tarot has no target score")
                },
                participants = partyRepository.getParticipantsByPartyId(referencePartyId)
                    .sortedBy { it.listPosition }
                    .map { ParticipantCreationData(UUID.randomUUID(), it.name) }
            )
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (failure: Throwable) {
            return Result.failure(failure)
        }
        return invoke(draft)
    }

    companion object {
        const val MAX_ROUND_COUNT = 99
    }
}

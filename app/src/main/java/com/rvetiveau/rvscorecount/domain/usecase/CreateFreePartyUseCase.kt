package com.rvetiveau.rvscorecount.domain.usecase

import com.rvetiveau.rvscorecount.domain.engine.FreeGameRules
import com.rvetiveau.rvscorecount.domain.model.EndRule
import com.rvetiveau.rvscorecount.domain.model.Participant
import com.rvetiveau.rvscorecount.domain.model.ParticipantCreationData
import com.rvetiveau.rvscorecount.domain.model.Party
import com.rvetiveau.rvscorecount.domain.model.PartyState
import com.rvetiveau.rvscorecount.domain.model.WinDirection
import com.rvetiveau.rvscorecount.domain.model.duplicatePlayerName
import com.rvetiveau.rvscorecount.domain.repository.PartyRepository
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.CancellationException

/** What a Partie of Jeu libre is created with (Story 11.2): its game, its rules and its seats in table order. */
data class FreePartyDraft(
    val gameName: String,
    val winDirection: WinDirection,
    val endRule: EndRule?,
    val participants: List<ParticipantCreationData>
)

/**
 * Creates a Partie of Jeu libre (AD-9) with its rules and Participants in one write, IN_PROGRESS,
 * the first seat dealing. An invalid draft fails with [InvalidPartyConfigurationException].
 */
class CreateFreePartyUseCase @Inject constructor(
    private val partyRepository: PartyRepository,
    private val rules: FreeGameRules
) {
    suspend operator fun invoke(draft: FreePartyDraft): Result<UUID> = try {
        val gameName = draft.gameName.trim()
        if (gameName.isEmpty() || gameName.length > MAX_GAME_NAME_LENGTH) {
            throw InvalidPartyConfigurationException("The game name must have 1 to $MAX_GAME_NAME_LENGTH characters")
        }
        val endValue = when (val endRule = draft.endRule) {
            is EndRule.TargetScore -> endRule.points
            is EndRule.RoundCount -> endRule.rounds
            null -> null
        }
        if (endValue != null && endValue > MAX_END_VALUE) {
            throw InvalidPartyConfigurationException("The end value must be at most $MAX_END_VALUE")
        }
        val seats = draft.participants.map { it.copy(name = it.name.trim()) }
        if (seats.size !in rules.minPlayers..rules.maxPlayers) {
            throw InvalidPartyConfigurationException("A Jeu libre needs ${rules.minPlayers} to ${rules.maxPlayers} players")
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
            endRule = draft.endRule,
            gameName = gameName,
            winDirection = draft.winDirection
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

    companion object {
        const val MAX_GAME_NAME_LENGTH = 30
        const val MAX_END_VALUE = 9_999
    }
}

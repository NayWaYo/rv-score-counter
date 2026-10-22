package com.rvetiveau.rvscorecount.domain.usecase

import com.rvetiveau.rvscorecount.domain.engine.GameIds
import com.rvetiveau.rvscorecount.domain.model.BeloteEndRule
import com.rvetiveau.rvscorecount.domain.model.Participant
import com.rvetiveau.rvscorecount.domain.model.Party
import com.rvetiveau.rvscorecount.domain.model.PartyState
import com.rvetiveau.rvscorecount.domain.model.Team
import com.rvetiveau.rvscorecount.domain.model.TeamSide
import com.rvetiveau.rvscorecount.domain.model.duplicatePlayerName
import com.rvetiveau.rvscorecount.domain.repository.BeloteRepository
import com.rvetiveau.rvscorecount.domain.repository.PartyRepository
import kotlinx.coroutines.CancellationException
import java.util.UUID
import javax.inject.Inject

/** An Équipe as entered at creation: its name and its two players. */
data class BeloteTeamDraft(val name: String, val playerNames: List<String>)

/**
 * Creates a Partie of Belote (Partie, two Équipes, four players) in one transaction, IN_PROGRESS,
 * the first player of the first Équipe dealing. Table order alternates the Équipes: A1, B1, A2, B2.
 */
class CreateBelotePartyUseCase @Inject constructor(
    private val partyRepository: PartyRepository,
    private val beloteRepository: BeloteRepository
) {
    /** [teams]: the first Équipe, then the second. */
    suspend operator fun invoke(teams: List<BeloteTeamDraft>, endRule: BeloteEndRule): Result<UUID> = try {
        val normalized = teams.map { team ->
            BeloteTeamDraft(team.name.trim(), team.playerNames.map(String::trim))
        }
        if (normalized.size != TeamSide.entries.size || normalized.any { it.playerNames.size != PLAYERS_PER_TEAM }) {
            throw InvalidPartyConfigurationException("A Belote party has two teams of two players")
        }
        if (normalized.any { team -> team.name.isBlank() || team.playerNames.any(String::isBlank) }) {
            throw InvalidPartyConfigurationException("Team and player names must not be blank")
        }
        duplicatePlayerName(normalized.flatMap { it.playerNames })?.let { name ->
            throw InvalidPartyConfigurationException("'$name' is already in the party")
        }

        val party = Party(
            id = UUID.randomUUID(),
            gameId = GameIds.BELOTE,
            status = PartyState.IN_PROGRESS,
            currentDealerIndex = 0,
            createdAt = System.currentTimeMillis(),
            endRule = endRule
        )
        val sides = TeamSide.entries.zip(normalized)
        val players = (0 until PLAYERS_PER_TEAM).flatMap { seat ->
            sides.map { (side, team) -> side to team.playerNames[seat] }
        }.mapIndexed { position, (side, name) ->
            Participant(UUID.randomUUID(), party.id, name, position, side)
        }
        try {
            beloteRepository.createParty(party, sides.map { (side, team) -> Team(party.id, side, team.name) }, players)
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

    /** « Rejouer » : same Équipes, players, seats and end rule as the FINISHED Partie [referencePartyId]. */
    suspend fun fromParty(referencePartyId: UUID): Result<UUID> {
        val (teams, endRule) = try {
            val reference = partyRepository.getPartyById(referencePartyId)
                ?.takeIf { it.gameId == GameIds.BELOTE && it.status == PartyState.FINISHED }
                ?: throw InvalidPartyConfigurationException("Party $referencePartyId is not a finished Belote party")
            val players = partyRepository.getParticipantsByPartyId(referencePartyId).sortedBy { it.listPosition }
            val teams = beloteRepository.getTeams(referencePartyId).sortedBy { it.side }.map { team ->
                BeloteTeamDraft(team.name, players.filter { it.teamSide == team.side }.map { it.name })
            }
            teams to checkNotNull(reference.endRule) { "Belote party $referencePartyId has no end rule" }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (failure: Throwable) {
            return Result.failure(failure)
        }
        return invoke(teams, endRule)
    }

    private companion object {
        const val PLAYERS_PER_TEAM = 2
    }
}

package com.rvetiveau.rvscorecount.domain.usecase

import com.rvetiveau.rvscorecount.domain.engine.GameEngine
import com.rvetiveau.rvscorecount.domain.engine.RoundValidation
import com.rvetiveau.rvscorecount.domain.model.CardSuit
import com.rvetiveau.rvscorecount.domain.model.PartyState
import com.rvetiveau.rvscorecount.domain.model.Round
import com.rvetiveau.rvscorecount.domain.model.RoundScore
import com.rvetiveau.rvscorecount.domain.model.Standing
import com.rvetiveau.rvscorecount.domain.model.computeStandings
import com.rvetiveau.rvscorecount.domain.repository.PartyRepository
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.CancellationException

sealed interface ValidateRoundOutcome {
    data class Validated(
        val round: Round,
        val nextDealerIndex: Int,
        val standings: List<Standing>
    ) : ValidateRoundOutcome

    /** `difference` = somme saisie - total attendu par le moteur ; rien n'est écrit. */
    data class SumMismatch(val difference: Int) : ValidateRoundOutcome
}

sealed class ValidateRoundException(message: String, cause: Throwable? = null) : Exception(message, cause)

class InvalidRoundException(message: String) : ValidateRoundException(message)

class RoundPersistenceException(cause: Throwable) :
    ValidateRoundException("Round persistence failed", cause)

/**
 * Valide et enregistre une Manche (FR-8, FR-9). Le contrôle de somme appartient au moteur de la
 * Partie ; le Round, ses RoundScores et la rotation du Donneur sont écrits en une seule
 * transaction, avec le Classement recalculé à partir des Manches déjà enregistrées.
 */
class ValidateRoundUseCase @Inject constructor(
    private val partyRepository: PartyRepository,
    private val engines: Map<String, @JvmSuppressWildcards GameEngine>
) {
    suspend operator fun invoke(
        partyId: UUID,
        suit: CardSuit,
        scores: Map<UUID, Int>,
        sevenHolderId: UUID? = null
    ): Result<ValidateRoundOutcome> {
        return try {
            val party = partyRepository.getPartyById(partyId)
                ?: throw InvalidRoundException("Party $partyId not found")
            if (party.status != PartyState.IN_PROGRESS) {
                throw InvalidRoundException("Party $partyId is ${party.status}, not IN_PROGRESS")
            }
            val engine = engines[party.gameId]?.takeIf { it.gameId == party.gameId }
                ?: throw InvalidRoundException("No GameEngine registered for gameId '${party.gameId}'")

            val participants = partyRepository.getParticipantsByPartyId(partyId)
            if (scores.keys != participants.mapTo(mutableSetOf()) { it.id }) {
                throw InvalidRoundException("Scores must cover exactly the party's participants")
            }
            if (scores.values.any { it !in 0..engine.totalPointsPerRound }) {
                throw InvalidRoundException("Scores must be within [0, ${engine.totalPointsPerRound}]")
            }
            if (party.currentDealerIndex !in participants.indices) {
                throw InvalidRoundException(
                    "currentDealerIndex ${party.currentDealerIndex} is out of bounds for ${participants.size} participants"
                )
            }
            checkSevenHolder(engine, scores, sevenHolderId)

            val validation = engine.validateRound(scores)
            if (validation is RoundValidation.Error) {
                return Result.success(ValidateRoundOutcome.SumMismatch(validation.difference))
            }

            val previousRounds = partyRepository.getRoundsByPartyId(partyId)
            val round = Round(
                id = UUID.randomUUID(),
                partyId = partyId,
                roundNumber = (previousRounds.maxOfOrNull { it.roundNumber } ?: 0) + 1,
                suit = suit,
                sevenHolderId = sevenHolderId
            )
            val nextDealerIndex = (party.currentDealerIndex + 1) % participants.size
            // Calculé avant l'écriture : une fois la Manche enregistrée, plus rien ne doit échouer,
            // sinon l'utilisateur pourrait la valider une seconde fois.
            val newScores = scores.map { (participantId, points) ->
                RoundScore(roundId = round.id, participantId = participantId, points = points)
            }
            val previousScores = previousRounds.flatMap { partyRepository.getScoresByRoundId(it.id) }
            val standings = computeStandings(participants, previousScores + newScores)

            try {
                partyRepository.saveRound(round, scores, party.copy(currentDealerIndex = nextDealerIndex))
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (failure: Throwable) {
                throw RoundPersistenceException(failure)
            }
            Result.success(ValidateRoundOutcome.Validated(round, nextDealerIndex, standings))
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }
}

/** A game with a suit seven requires its holder, who scored at least its points this round. */
internal fun checkSevenHolder(engine: GameEngine, scores: Map<UUID, Int>, sevenHolderId: UUID?) {
    val sevenPoints = engine.suitSevenPoints
    if (sevenPoints == null) {
        if (sevenHolderId != null) throw InvalidRoundException("Game '${engine.gameId}' has no suit seven to record")
        return
    }
    val holderScore = sevenHolderId?.let { scores[it] }
        ?: throw InvalidRoundException("The 7 holder must be one of the round's participants")
    if (holderScore < sevenPoints) {
        throw InvalidRoundException("The 7 holder scored $holderScore, less than the 7's $sevenPoints points")
    }
}

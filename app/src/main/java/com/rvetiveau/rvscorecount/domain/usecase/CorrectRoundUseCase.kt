package com.rvetiveau.rvscorecount.domain.usecase

import com.rvetiveau.rvscorecount.domain.engine.GameEngine
import com.rvetiveau.rvscorecount.domain.engine.RoundValidation
import com.rvetiveau.rvscorecount.domain.model.PartyState
import com.rvetiveau.rvscorecount.domain.model.Round
import com.rvetiveau.rvscorecount.domain.repository.PartyRepository
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.CancellationException

sealed interface CorrectRoundOutcome {
    data class Corrected(val round: Round) : CorrectRoundOutcome

    /** `difference` = somme saisie - total attendu par le moteur ; rien n'est écrit. */
    data class SumMismatch(val difference: Int) : CorrectRoundOutcome
}

/**
 * Corrige une Manche déjà validée d'une Partie en cours : mêmes contrôles qu'à la validation
 * (somme du moteur, détenteur du 7), sur les seuls Participants qui l'ont jouée. Le numéro, la
 * couleur et le Donneur ne changent pas ; scores et détenteur du 7 sont réécrits ensemble.
 */
class CorrectRoundUseCase @Inject constructor(
    private val partyRepository: PartyRepository,
    private val engines: Map<String, @JvmSuppressWildcards GameEngine>
) {
    suspend operator fun invoke(
        partyId: UUID,
        roundNumber: Int,
        scores: Map<UUID, Int>,
        sevenHolderId: UUID? = null
    ): Result<CorrectRoundOutcome> {
        return try {
            val party = partyRepository.getPartyById(partyId)
                ?: throw InvalidRoundException("Party $partyId not found")
            if (party.status != PartyState.IN_PROGRESS) {
                throw InvalidRoundException("Party $partyId is ${party.status}, not IN_PROGRESS")
            }
            val engine = engines[party.gameId]?.takeIf { it.gameId == party.gameId }
                ?: throw InvalidRoundException("No GameEngine registered for gameId '${party.gameId}'")

            val round = partyRepository.getRoundsByPartyId(partyId).firstOrNull { it.roundNumber == roundNumber }
                ?: throw InvalidRoundException("Party $partyId has no round $roundNumber")
            val playedBy = partyRepository.getScoresByRoundId(round.id).mapTo(mutableSetOf()) { it.participantId }
            if (scores.keys != playedBy) {
                throw InvalidRoundException("Scores must cover exactly the participants who played round $roundNumber")
            }
            if (scores.values.any { it !in 0..engine.totalPointsPerRound }) {
                throw InvalidRoundException("Scores must be within [0, ${engine.totalPointsPerRound}]")
            }
            checkSevenHolder(engine, scores, sevenHolderId)

            val validation = engine.validateRound(scores)
            if (validation is RoundValidation.Error) {
                return Result.success(CorrectRoundOutcome.SumMismatch(validation.difference))
            }

            val corrected = round.copy(sevenHolderId = sevenHolderId)
            try {
                partyRepository.correctRound(corrected, scores)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (failure: Throwable) {
                throw RoundPersistenceException(failure)
            }
            Result.success(CorrectRoundOutcome.Corrected(corrected))
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }
}

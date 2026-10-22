package com.rvetiveau.rvscorecount.domain.usecase

import com.rvetiveau.rvscorecount.domain.model.playerNameKey
import com.rvetiveau.rvscorecount.domain.repository.PlayerRepository
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.first

sealed interface RenamePlayerOutcome {
    data object Renamed : RenamePlayerOutcome
    data object BlankName : RenamePlayerOutcome
    /** Another Joueur, [takenBy], already has this name, case and spaces ignored. */
    data class NameTaken(val takenBy: String) : RenamePlayerOutcome
    data object UnknownPlayer : RenamePlayerOutcome
}

/**
 * Renames a Joueur everywhere (Story 10.2): their own name may change case or spaces, but one name
 * is one Joueur (Story 10.1), so a name another Joueur has is refused.
 */
class RenamePlayerUseCase @Inject constructor(
    private val playerRepository: PlayerRepository
) {
    suspend operator fun invoke(playerId: UUID, name: String): RenamePlayerOutcome {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return RenamePlayerOutcome.BlankName
        val players = playerRepository.observePlayers().first()
        if (players.none { it.id == playerId }) return RenamePlayerOutcome.UnknownPlayer
        players.firstOrNull { it.id != playerId && playerNameKey(it.name) == playerNameKey(trimmed) }
            ?.let { return RenamePlayerOutcome.NameTaken(it.name) }
        return if (playerRepository.renamePlayer(playerId, trimmed)) RenamePlayerOutcome.Renamed else RenamePlayerOutcome.UnknownPlayer
    }
}

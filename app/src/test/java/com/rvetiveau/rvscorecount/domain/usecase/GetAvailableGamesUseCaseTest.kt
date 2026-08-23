package com.rvetiveau.rvscorecount.domain.usecase

import com.rvetiveau.rvscorecount.domain.engine.GameEngine
import com.rvetiveau.rvscorecount.domain.engine.RoundValidation
import com.rvetiveau.rvscorecount.domain.model.ParticipantId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class GetAvailableGamesUseCaseTest {

    private class FakeGameEngine(override val gameId: String) : GameEngine {
        override val minPlayers: Int = 2
        override val maxPlayers: Int = 8
        override val totalPointsPerRound: Int = 100
        override fun precomputeLastScore(entered: Map<ParticipantId, Int>): Int = 0
        override fun validateRound(scores: Map<ParticipantId, Int>): RoundValidation = RoundValidation.Success
    }

    @Test
    fun `returns the Papayoo engine when it is the only one registered`() {
        val papayoo = FakeGameEngine("papayoo")
        val useCase = GetAvailableGamesUseCase(mapOf("papayoo" to papayoo))

        assertEquals(listOf(papayoo), useCase())
    }

    @Test
    fun `returns engines ordered deterministically by gameId`() {
        val zebra = FakeGameEngine("zebra")
        val alpha = FakeGameEngine("alpha")
        val useCase = GetAvailableGamesUseCase(mapOf("zebra" to zebra, "alpha" to alpha))

        assertEquals(listOf(alpha, zebra), useCase())
    }

    @Test
    fun `returns an empty list when the registry is empty`() {
        val useCase = GetAvailableGamesUseCase(emptyMap())

        assertTrue(useCase().isEmpty())
    }

    @Test
    fun `fails fast when a registry key does not match the engine's own gameId`() {
        val mismatched = FakeGameEngine("other")
        val useCase = GetAvailableGamesUseCase(mapOf("papayoo" to mismatched))

        assertThrows(IllegalStateException::class.java) { useCase() }
    }
}

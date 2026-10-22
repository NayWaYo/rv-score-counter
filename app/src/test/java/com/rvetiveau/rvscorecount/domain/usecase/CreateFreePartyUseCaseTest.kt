package com.rvetiveau.rvscorecount.domain.usecase

import com.rvetiveau.rvscorecount.domain.model.EndRule
import com.rvetiveau.rvscorecount.domain.model.Participant
import com.rvetiveau.rvscorecount.domain.model.ParticipantCreationData
import com.rvetiveau.rvscorecount.domain.model.Party
import com.rvetiveau.rvscorecount.domain.model.PartyState
import com.rvetiveau.rvscorecount.domain.model.PlayedGameName
import com.rvetiveau.rvscorecount.domain.model.WinDirection
import com.rvetiveau.rvscorecount.domain.model.distinctGameNames
import com.rvetiveau.rvscorecount.game.free.FreeGameEngine
import java.io.IOException
import java.util.UUID
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CreateFreePartyUseCaseTest {

    /** Records the single aggregate write, or fails it with [failure]. */
    private class RecordingStore(private val failure: Throwable? = null) : PartyRepositoryAdapter() {
        var party: Party? = null
        var participants: List<Participant> = emptyList()

        override suspend fun createPartyWithParticipants(party: Party, participants: List<Participant>) {
            failure?.let { throw it }
            this.party = party
            this.participants = participants
        }
    }

    private fun seats(vararg names: String) = names.map { ParticipantCreationData(UUID.randomUUID(), it) }

    private fun draft(
        gameName: String = " Skyjo ",
        endRule: EndRule? = EndRule.TargetScore(100),
        participants: List<ParticipantCreationData> = seats("Alice", " Bob ")
    ) = FreePartyDraft(gameName, WinDirection.LOWEST_WINS, endRule, participants)

    @Test
    fun `creates the party in progress with its rules and its seats in order, the first dealing`() = runTest {
        val store = RecordingStore()

        val partyId = CreateFreePartyUseCase(store, FreeGameEngine())(draft()).getOrThrow()

        val party = checkNotNull(store.party)
        assertEquals(partyId, party.id)
        assertEquals("free", party.gameId)
        assertEquals(PartyState.IN_PROGRESS, party.status)
        assertEquals(0, party.currentDealerIndex)
        assertEquals("Skyjo", party.gameName)
        assertEquals(WinDirection.LOWEST_WINS, party.winDirection)
        assertEquals(EndRule.TargetScore(100), party.endRule)
        assertEquals(listOf("Alice" to 0, "Bob" to 1), store.participants.map { it.name to it.listPosition })
    }

    @Test
    fun `an end by hand has no rule`() = runTest {
        val store = RecordingStore()

        CreateFreePartyUseCase(store, FreeGameEngine())(draft(endRule = null)).getOrThrow()

        assertNull(store.party?.endRule)
    }

    @Test
    fun `an invalid draft is refused without writing`() = runTest {
        val invalid = listOf(
            draft(gameName = "  "),
            draft(gameName = "x".repeat(31)),
            draft(participants = seats("Alice")),
            draft(participants = seats(*Array(11) { "P$it" })),
            draft(participants = seats("Alice", "alice")),
            draft(participants = seats("Alice", " ")),
            draft(endRule = EndRule.RoundCount(10_000))
        )

        invalid.forEach { draft ->
            val store = RecordingStore()
            val result = CreateFreePartyUseCase(store, FreeGameEngine())(draft)
            assertTrue(result.exceptionOrNull() is InvalidPartyConfigurationException)
            assertNull(store.party)
        }
    }

    @Test
    fun `a storage failure is a persistence failure`() = runTest {
        val result = CreateFreePartyUseCase(RecordingStore(IOException("disk")), FreeGameEngine())(draft())

        assertTrue(result.exceptionOrNull() is PartyCreationPersistenceException)
    }

    @Test
    fun `game names are suggested once each, the most recently played first`() {
        val played = listOf(
            PlayedGameName("Uno", 10),
            PlayedGameName("skyjo", 5),
            PlayedGameName(" Skyjo", 30),
            PlayedGameName("Yams", 20)
        )

        assertEquals(listOf("Skyjo", "Yams", "Uno"), played.distinctGameNames())
    }
}

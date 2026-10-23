package com.rvetiveau.rvscorecount.domain.usecase

import com.rvetiveau.rvscorecount.game.tarot.TarotEngine
import com.rvetiveau.rvscorecount.game.free.FreeGameEngine
import com.rvetiveau.rvscorecount.domain.engine.Game
import com.rvetiveau.rvscorecount.domain.model.EndRule
import com.rvetiveau.rvscorecount.domain.model.BelotePartyStatus
import com.rvetiveau.rvscorecount.domain.model.BeloteRound
import com.rvetiveau.rvscorecount.domain.model.BeloteRoundInput
import com.rvetiveau.rvscorecount.domain.model.PartyState
import com.rvetiveau.rvscorecount.domain.model.TeamPoints
import com.rvetiveau.rvscorecount.domain.model.TeamScore
import com.rvetiveau.rvscorecount.domain.model.TeamSide.FIRST
import com.rvetiveau.rvscorecount.domain.model.TeamSide.SECOND
import com.rvetiveau.rvscorecount.game.belote.BeloteEngine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.util.UUID

/** Belote use cases (Story 7.3) over the real engine and an in-memory store. */
class BeloteUseCasesTest {

    private val store = FakeBeloteStore()
    private val rules = BeloteEngine()
    private val create = CreateBelotePartyUseCase(store, store)
    private val playRound = PlayBeloteRoundUseCase(store, store, rules)
    private val correctRound = CorrectBeloteRoundUseCase(store, store, rules)

    /** Plays or corrects with the first player of the taking Équipe as taker (Story 8.3). */
    private suspend fun play(partyId: UUID, input: BeloteRoundInput) =
        playRound(partyId, input, store.takerOf(partyId, input.taker))

    private suspend fun correct(partyId: UUID, roundNumber: Int, input: BeloteRoundInput) =
        correctRound(partyId, roundNumber, input, store.takerOf(partyId, input.taker))
    private val observe = ObserveBelotePartyUseCase(store, store, rules)

    private val drafts = listOf(
        BeloteTeamDraft(" Nous ", listOf("Alice", " Chloé")),
        BeloteTeamDraft("Eux", listOf("Bob", "David "))
    )

    // Catalogue

    private class FakeGame(override val gameId: String) : Game

    @Test
    fun `the catalogue lists the Papayoo then the Belote, whatever the registration order`() {
        val catalogue = GetGameCatalogUseCase(mapOf("belote" to FakeGame("belote"), "papayoo" to FakeGame("papayoo")))

        assertEquals(listOf("papayoo", "belote"), catalogue())
    }

    @Test
    fun `the catalogue fails on a key mismatch or a Jeu missing from the order`() {
        assertThrows(IllegalStateException::class.java) { GetGameCatalogUseCase(mapOf("belote" to FakeGame("papayoo")))() }
        assertThrows(IllegalStateException::class.java) { GetGameCatalogUseCase(mapOf("poker" to FakeGame("poker")))() }
    }

    // Creation

    @Test
    fun `creates the party with alternating seats, trimmed names and the first player dealing`() = runTest {
        val partyId = create(drafts, EndRule.RoundCount(10)).getOrThrow()

        val party = checkNotNull(store.getPartyById(partyId))
        assertEquals("belote", party.gameId)
        assertEquals(PartyState.IN_PROGRESS, party.status)
        assertEquals(0, party.currentDealerIndex)
        assertEquals(EndRule.RoundCount(10), party.endRule)
        assertEquals(listOf("Nous", "Eux"), store.getTeams(partyId).map { it.name })
        val players = store.getParticipantsByPartyId(partyId)
        assertEquals(listOf("Alice", "Bob", "Chloé", "David"), players.map { it.name })
        assertEquals(listOf(FIRST, SECOND, FIRST, SECOND), players.map { it.teamSide })
    }

    @Test
    fun `refuses blank or repeated names or teams that are not two pairs`() = runTest {
        val blankPlayer = drafts.mapIndexed { i, team -> if (i == 0) team.copy(playerNames = listOf("Alice", " ")) else team }
        val blankTeam = drafts.mapIndexed { i, team -> if (i == 1) team.copy(name = "") else team }
        val threePlayers = drafts.mapIndexed { i, team -> if (i == 0) team.copy(playerNames = team.playerNames + "Eve") else team }
        val samePlayerTwice = drafts.mapIndexed { i, team -> if (i == 1) team.copy(playerNames = listOf("ALICE", "Bob")) else team }

        listOf(blankPlayer, blankTeam, threePlayers, samePlayerTwice, drafts.take(1)).forEach { teams ->
            assertTrue(create(teams, EndRule.TargetScore(1000)).exceptionOrNull() is InvalidPartyConfigurationException)
        }
        assertTrue(store.parties.value.isEmpty())
    }

    @Test
    fun `a storage failure is reported as a persistence failure`() = runTest {
        store.writeFailure = IOException("disk")

        assertTrue(create(drafts, EndRule.TargetScore(1000)).exceptionOrNull() is PartyCreationPersistenceException)
    }

    @Test
    fun `replay copies the teams, the seats and the end rule of a finished party`() = runTest {
        val reference = store.seedParty(EndRule.RoundCount(4), status = PartyState.FINISHED)

        val partyId = create.fromParty(reference.id).getOrThrow()

        assertEquals(EndRule.RoundCount(4), store.getPartyById(partyId)?.endRule)
        assertEquals(listOf("Nous", "Eux"), store.getTeams(partyId).map { it.name })
        assertEquals(listOf("Alice", "Bob", "Chloé", "David"), store.getParticipantsByPartyId(partyId).map { it.name })
    }

    @Test
    fun `replay refuses a party in progress`() = runTest {
        val reference = store.seedParty()

        assertTrue(create.fromParty(reference.id).exceptionOrNull() is InvalidPartyConfigurationException)
    }

    // Playing a round

    @Test
    fun `a round is saved with the next dealer, who wraps around the table`() = runTest {
        val party = store.seedParty(dealerIndex = 3)

        val status = play(party.id, BeloteRoundInput(FIRST, 100)).getOrThrow()

        assertEquals(BelotePartyStatus.InProgress, status)
        assertEquals(listOf(1), store.getRounds(party.id).map { it.roundNumber })
        assertEquals(0, store.getPartyById(party.id)?.currentDealerIndex)
    }

    @Test
    fun `the round reaching the target finishes the party in the same write`() = runTest {
        val party = store.seedParty(EndRule.TargetScore(200))
        play(party.id, BeloteRoundInput(SECOND, 120)).getOrThrow()

        val status = play(party.id, BeloteRoundInput(SECOND, 100)).getOrThrow()

        assertEquals(BelotePartyStatus.Won(SECOND), status)
        assertEquals(PartyState.FINISHED, store.getPartyById(party.id)?.status)
    }

    @Test
    fun `an input the engine refuses is an invalid round and nothing is saved`() = runTest {
        val party = store.seedParty()

        val result = play(party.id, BeloteRoundInput(FIRST, 150, capotTeam = FIRST))

        assertTrue(result.exceptionOrNull() is InvalidBeloteRoundException)
        assertTrue(store.getRounds(party.id).isEmpty())
    }

    @Test
    fun `no round can be played in a finished party`() = runTest {
        val party = store.seedParty(status = PartyState.FINISHED)

        assertTrue(play(party.id, BeloteRoundInput(FIRST, 100)).exceptionOrNull() is InvalidBeloteRoundException)
    }

    @Test
    fun `a storage failure on a round is a persistence failure`() = runTest {
        val party = store.seedParty()
        store.writeFailure = IOException("disk")

        assertTrue(play(party.id, BeloteRoundInput(FIRST, 100)).exceptionOrNull() is BeloteRoundPersistenceException)
    }

    // Correcting a round

    @Test
    fun `a correction replaces the input, keeps the dealer and rescores the next rounds`() = runTest {
        val party = store.seedParty()
        play(party.id, BeloteRoundInput(FIRST, 100)).getOrThrow()
        play(party.id, BeloteRoundInput(SECOND, 100)).getOrThrow()

        correct(party.id, 1, BeloteRoundInput(FIRST, 81)).getOrThrow()

        assertEquals(BeloteRoundInput(FIRST, 81), store.getRounds(party.id).first().input)
        assertEquals(2, store.getPartyById(party.id)?.currentDealerIndex)
        val state = checkNotNull(observe(party.id).first())
        assertEquals(TeamPoints(0, 81), state.results[0].points)
        assertEquals(TeamPoints(62, 181), state.results[1].points)
    }

    @Test
    fun `a correction reaching the target finishes the party`() = runTest {
        val party = store.seedParty(EndRule.TargetScore(260))
        play(party.id, BeloteRoundInput(FIRST, 150)).getOrThrow()

        val status = correct(party.id, 1, BeloteRoundInput(FIRST, 162, capotTeam = FIRST)).getOrThrow()

        assertEquals(BelotePartyStatus.InProgress, status)
        val finished = correct(party.id, 1, BeloteRoundInput(FIRST, 162, beloteTeam = FIRST, capotTeam = FIRST)).getOrThrow()
        assertEquals(BelotePartyStatus.Won(FIRST), finished)
        assertEquals(PartyState.FINISHED, store.getPartyById(party.id)?.status)
    }

    @Test
    fun `an unknown round cannot be corrected`() = runTest {
        val party = store.seedParty()

        assertTrue(correct(party.id, 1, BeloteRoundInput(FIRST, 100)).exceptionOrNull() is InvalidBeloteRoundException)
    }

    // Joueur preneur et « 🗿 Chad de la game » (Story 8.3)

    private fun playerNamed(name: String) = store.participants.value.first { it.name == name }.id

    @Test
    fun `the taker player is stored with the round, and a correction replaces it`() = runTest {
        val party = store.seedParty()
        playRound(party.id, BeloteRoundInput(FIRST, 100), playerNamed("Chloé")).getOrThrow()

        assertEquals(playerNamed("Chloé"), store.getRounds(party.id).single().takerPlayerId)

        correctRound(party.id, 1, BeloteRoundInput(SECOND, 100), playerNamed("David")).getOrThrow()
        assertEquals(playerNamed("David"), store.getRounds(party.id).single().takerPlayerId)
    }

    @Test
    fun `a taker player outside the taking team or the party is refused`() = runTest {
        val party = store.seedParty()

        val otherTeam = playRound(party.id, BeloteRoundInput(FIRST, 100), playerNamed("Bob"))
        val stranger = playRound(party.id, BeloteRoundInput(FIRST, 100), UUID.randomUUID())

        assertTrue(otherTeam.exceptionOrNull() is InvalidBeloteRoundException)
        assertTrue(stranger.exceptionOrNull() is InvalidBeloteRoundException)
        assertTrue(store.getRounds(party.id).isEmpty())
    }

    @Test
    fun `the takers are ranked by takes, then contracts made, then table order`() = runTest {
        val party = store.seedParty()
        playRound(party.id, BeloteRoundInput(FIRST, 100), playerNamed("Chloé")).getOrThrow()
        playRound(party.id, BeloteRoundInput(FIRST, 50), playerNamed("Chloé")).getOrThrow()
        playRound(party.id, BeloteRoundInput(SECOND, 0, capotTeam = FIRST), playerNamed("Bob")).getOrThrow()
        playRound(party.id, BeloteRoundInput(SECOND, 162, capotTeam = SECOND), playerNamed("David")).getOrThrow()
        playRound(party.id, BeloteRoundInput(FIRST, 81), playerNamed("Alice")).getOrThrow()

        val ranking = checkNotNull(observe(party.id).first()).takerRanking()

        assertEquals(listOf("Chloé", "David", "Alice", "Bob"), ranking.map { it.player.name })
        assertEquals(listOf(2, 1, 1, 1), ranking.map { it.takes })
        assertEquals(listOf(1, 1, 0, 0), ranking.map { it.made })
    }

    @Test
    fun `rounds without a taker player do not count, and none gives no ranking`() = runTest {
        val party = store.seedParty()
        store.rounds.value = listOf(BeloteRound(UUID.randomUUID(), party.id, 1, BeloteRoundInput(FIRST, 100)))

        assertTrue(checkNotNull(observe(party.id).first()).takerRanking().isEmpty())

        playRound(party.id, BeloteRoundInput(SECOND, 100), playerNamed("Bob")).getOrThrow()
        val ranking = checkNotNull(observe(party.id).first()).takerRanking()
        assertEquals(listOf("Bob" to 1, "Alice" to 0, "Chloé" to 0, "David" to 0), ranking.map { it.player.name to it.takes })
    }

    // Historique (Story 7.4)

    private val history =
        ObservePartyHistoryUseCase(store, store, rules, FakeFreeGameRepository(), FreeGameEngine(), FakeTarotRepository(), TarotEngine())

    @Test
    fun `a finished Belote party is summarized by team with its winning team`() = runTest {
        val party = store.seedParty(EndRule.RoundCount(2))
        play(party.id, BeloteRoundInput(FIRST, 100)).getOrThrow()
        play(party.id, BeloteRoundInput(SECOND, 120)).getOrThrow()

        val summary = history().first().single()

        assertEquals("belote", summary.gameId)
        assertEquals(2, summary.roundCount)
        assertEquals(listOf(TeamScore("Nous", 142), TeamScore("Eux", 182)), summary.teamScores)
        assertEquals(listOf("Eux"), summary.winners)
        assertEquals(182, summary.winningScore)
        assertTrue(summary.isConsistent)
    }

    @Test
    fun `a finished Belote party without a winner or with broken data is incoherent`() = runTest {
        store.seedParty(status = PartyState.FINISHED)
        val broken = store.seedParty(status = PartyState.FINISHED)
        store.teams.value = store.teams.value.filterNot { it.partyId == broken.id && it.side == SECOND }

        val summaries = history().first()

        assertEquals(2, summaries.size)
        assertTrue(summaries.none { it.isConsistent })
        assertTrue(summaries.all { it.winners.isEmpty() })
    }

    @Test
    fun `the history can still be limited to the Papayoo`() = runTest {
        store.seedParty(status = PartyState.FINISHED)

        assertTrue(history("papayoo").first().isEmpty())
    }

    // Observing

    @Test
    fun `the observed party has its totals, its pending reserve and its dealer`() = runTest {
        val party = store.seedParty()
        play(party.id, BeloteRoundInput(FIRST, 100)).getOrThrow()
        play(party.id, BeloteRoundInput(SECOND, 81)).getOrThrow()

        val state = checkNotNull(observe(party.id).first())

        assertEquals(TeamPoints(181, 62), state.totals)
        assertEquals(81, state.pendingReserve)
        assertEquals("Chloé", state.dealer.name)
        assertEquals(listOf("Alice", "Chloé"), state.playersOf(FIRST).map { it.name })
    }

    @Test
    fun `an absent party is observed as null`() = runTest {
        assertNull(observe(UUID.randomUUID()).first())
    }

    @Test
    fun `an incoherent party fails instead of being shown`() = runTest {
        val party = store.seedParty()
        store.participants.value = store.participants.value.drop(1)

        val failure = runCatching { observe(party.id).first() }.exceptionOrNull()

        assertTrue(failure is IllegalStateException)
    }
}

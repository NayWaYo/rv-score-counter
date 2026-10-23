package com.rvetiveau.rvscorecount.ui.screens.belote

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.rvetiveau.rvscorecount.ui.components.TakerUiModel
import com.rvetiveau.rvscorecount.domain.model.BeloteOutcome
import com.rvetiveau.rvscorecount.domain.model.BeloteRoundResult
import com.rvetiveau.rvscorecount.domain.model.PartySummary
import com.rvetiveau.rvscorecount.domain.model.TeamPoints
import com.rvetiveau.rvscorecount.domain.model.TeamScore
import com.rvetiveau.rvscorecount.domain.model.TeamSide
import com.rvetiveau.rvscorecount.ui.components.PartySummaryRow
import com.rvetiveau.rvscorecount.ui.components.RoundTablePlayer
import com.rvetiveau.rvscorecount.ui.components.ScoreSeries
import com.rvetiveau.rvscorecount.ui.theme.RvScoreCounterTheme
import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/** Instrumented tests for the pure Belote screens (Story 7.3). */
class BeloteScreensTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun setContent(content: @Composable () -> Unit) {
        composeTestRule.setContent { RvScoreCounterTheme { content() } }
    }

    // Setup

    private val filledSetup = BeloteSetupUiState(
        isInitialized = true,
        teamNames = listOf("Nous", "Eux"),
        playerNames = listOf(listOf("Alice", "Chloé"), listOf("Bob", "David"))
    )

    @Test
    fun setupShowsTheTableOrderAndTheFirstDealer() {
        setContent { BeloteSetupScreen(filledSetup) }

        composeTestRule.onNodeWithTag("belote_setup_table_order").performScrollTo()
            .assertTextContains("Alice → Bob → Chloé → David")
        composeTestRule.onNodeWithTag("belote_setup_first_dealer").assertTextContains("Alice donne en premier")
        composeTestRule.onNodeWithTag("belote_setup_start").performScrollTo().assertIsEnabled()
    }

    @Test
    fun setupCannotStartWithAMissingPlayerOrAnInvalidEndValue() {
        val missingPlayer = filledSetup.copy(playerNames = listOf(listOf("Alice", ""), listOf("Bob", "David")))
        setContent { BeloteSetupScreen(missingPlayer.copy(targetScoreInput = "0")) }

        composeTestRule.onNodeWithText("Entrez un nombre entre 1 et 9999").performScrollTo()
        composeTestRule.onNodeWithTag("belote_setup_start").performScrollTo().assertIsNotEnabled()
    }

    @Test
    fun setupOpensTheBeloteHistoryFromItsHeader() {
        var opened = false
        setContent { BeloteSetupScreen(filledSetup, onHistorySelected = { opened = true }) }

        composeTestRule.onNodeWithTag("belote_setup_history_action").performClick()

        assertEquals(true, opened)
    }

    @Test
    fun setupReportsTheChosenEndMode() {
        var mode: BeloteEndMode? = null
        setContent { BeloteSetupScreen(filledSetup, onEndModeChanged = { mode = it }) }

        composeTestRule.onNodeWithTag("belote_setup_end_mode_ROUND_COUNT").performScrollTo().performClick()

        assertEquals(BeloteEndMode.ROUND_COUNT, mode)
    }

    // Party

    private val loadedParty = BelotePartyUiState.Loaded(
        teams = listOf(
            BeloteTeamUiModel(TeamSide.FIRST, "Nous", listOf("Alice", "Chloé"), 181, isLeader = true),
            BeloteTeamUiModel(TeamSide.SECOND, "Eux", listOf("Bob", "David"), 62, isLeader = false)
        ),
        progress = BeloteProgress.Target(1000, 819, listOf("Nous")),
        dealerName = "Chloé",
        pendingReserve = 81,
        rounds = listOf(
            BeloteRoundRowUiModel(1, "Nous", BeloteOutcome.MADE, 100, 62),
            BeloteRoundRowUiModel(2, "Eux", BeloteOutcome.DISPUTED, 81, 0)
        ),
        nextRoundNumber = 3
    )

    @Test
    fun partyShowsTheScoresTheLeaderTheProgressAndTheReserve() {
        setContent { BelotePartyScreen(loadedParty) }

        composeTestRule.onNodeWithTag("belote_team_total_FIRST").assertTextContains("181")
        composeTestRule.onNodeWithTag("belote_team_total_SECOND").assertTextContains("62")
        composeTestRule.onNodeWithContentDescription("Nous, en tête").assertExists()
        composeTestRule.onNodeWithTag("belote_party_progress").assertTextContains("il en manque 819 à Nous", substring = true)
        composeTestRule.onNodeWithTag("belote_party_dealer").assertTextContains("Donneur : Chloé")
        composeTestRule.onNodeWithTag("belote_party_reserve").assertTextContains("81 points", substring = true)
        composeTestRule.onNodeWithTag("belote_party_play_round").assertTextContains("Saisir la manche 3")
    }

    @Test
    fun partyRowsDescribeTheRoundAndOpenItsCorrection() {
        var selected: Int? = null
        setContent { BelotePartyScreen(loadedParty, onRoundSelected = { selected = it }) }

        composeTestRule.onNodeWithContentDescription("Manche 2, prise par Eux, Litige : Nous 81, Eux 0")
            .performScrollTo()
            .performClick()

        assertEquals(2, selected)
    }

    @Test
    fun partyWithoutRoundSaysSo() {
        setContent { BelotePartyScreen(loadedParty.copy(rounds = emptyList(), pendingReserve = 0, nextRoundNumber = 1)) }

        composeTestRule.onNodeWithTag("belote_party_no_round").assertExists()
        composeTestRule.onNodeWithTag("belote_party_reserve").assertDoesNotExist()
    }

    // Round entry

    private val emptyRound = BeloteRoundUiState.Loaded(roundNumber = 3, isCorrection = false, teamNames = listOf("Nous", "Eux"))

    private val seats = listOf("Alice" to TeamSide.FIRST, "Bob" to TeamSide.SECOND, "Chloé" to TeamSide.FIRST, "David" to TeamSide.SECOND)
        .map { (name, side) -> BeloteSeatUiModel(UUID.randomUUID(), name, side) }

    @Test
    fun roundEntryOffersThePlayersByTeamAndAsksForThePlayerOfAKnownTeam() {
        setContent { BeloteRoundScreen(emptyRound.copy(players = seats, taker = TeamSide.SECOND, isCorrection = true)) }

        seats.forEach { composeTestRule.onNodeWithTag("belote_round_taker_${it.id}").assertTextContains(it.name) }
        composeTestRule.onNodeWithTag("belote_round_taker_player_hint").assertTextEquals("Indiquez quel joueur de Eux a pris.")
        composeTestRule.onNodeWithTag("belote_round_validate").performScrollTo().assertIsNotEnabled()
    }

    @Test
    fun statsShowTheChadCardWithTheRanking() {
        val series = listOf("Nous", "Eux").map { ScoreSeries(RoundTablePlayer(UUID.randomUUID(), it), listOf(0, 100)) }
        val takers = listOf(
            TakerUiModel("Chloé", takes = 2, made = 1),
            TakerUiModel("David", takes = 1, made = 1),
            TakerUiModel("Alice", takes = 0, made = 0),
            TakerUiModel("Bob", takes = 0, made = 0)
        )
        setContent { BeloteStatsScreen(BeloteStatsUiState.Loaded(series, takers)) }

        composeTestRule.onNodeWithTag("stat_card_chad").performScrollTo().assert(
            hasContentDescription("Chad de la game : Chloé.", substring = true)
        ).assert(hasContentDescription("1. Chloé, 2 prises · 1 faite ; 2. David, 1 prise · 1 faite", substring = true))
    }

    @Test
    fun statsWithoutRecordedTakerHaveNoChadCard() {
        val series = listOf("Nous", "Eux").map { ScoreSeries(RoundTablePlayer(UUID.randomUUID(), it), listOf(0, 100)) }
        setContent { BeloteStatsScreen(BeloteStatsUiState.Loaded(series)) }

        composeTestRule.onNodeWithTag("stat_card_chad").assertDoesNotExist()
    }

    @Test
    fun roundEntryWaitsForTheTakerAndThePoints() {
        setContent { BeloteRoundScreen(emptyRound) }

        composeTestRule.onNodeWithTag("belote_round_title").assertTextContains("Manche 3")
        composeTestRule.onNodeWithText("Choisissez qui a pris et saisissez les points d'une équipe.").assertExists()
        composeTestRule.onNodeWithTag("belote_round_validate").performScrollTo().assertIsNotEnabled()
    }

    @Test
    fun roundEntryShowsThePreviewWithTheReserve() {
        val preview = BeloteRoundResult(BeloteOutcome.DISPUTED, TeamPoints(81, 20), reserveWon = 0, reserveAfter = 152)
        setContent {
            BeloteRoundScreen(
                emptyRound.copy(
                    players = seats,
                    takerPlayerId = seats[1].id,
                    taker = TeamSide.SECOND,
                    pointsInputs = listOf("91", "71"),
                    beloteTeam = TeamSide.SECOND,
                    preview = preview
                )
            )
        }

        composeTestRule.onNodeWithTag("belote_round_preview_outcome").performScrollTo().assertTextContains("Litige")
        composeTestRule.onNodeWithTag("belote_round_preview_points").assertTextContains("Nous 81 · Eux 20")
        composeTestRule.onNodeWithTag("belote_round_preview_reserve").assertTextContains("152 points", substring = true)
        composeTestRule.onNodeWithTag("belote_round_validate").performScrollTo().assertIsEnabled()
    }

    @Test
    fun roundEntryReportsChoicesAndFlagsInvalidPoints() {
        var taker: UUID? = null
        var capot: TeamSide? = TeamSide.FIRST
        setContent {
            BeloteRoundScreen(
                emptyRound.copy(pointsInputs = listOf("170", ""), pointsError = true, players = seats),
                onTakerPlayerSelected = { taker = it },
                onCapotChanged = { capot = it }
            )
        }

        composeTestRule.onNodeWithTag("belote_round_points_error").assertTextContains("Entre 0 et 162")
        composeTestRule.onNodeWithTag("belote_round_taker_${seats[1].id}").performClick()
        composeTestRule.onNodeWithTag("belote_round_capot_NONE").performScrollTo().performClick()

        assertEquals(seats[1].id, taker)
        assertEquals(null, capot)
    }

    @Test
    fun aCorrectionSaysSo() {
        setContent { BeloteRoundScreen(emptyRound.copy(roundNumber = 2, isCorrection = true)) }

        composeTestRule.onNodeWithTag("belote_round_title").assertTextContains("Corriger la manche 2")
        composeTestRule.onNodeWithTag("belote_round_validate").performScrollTo().assertTextContains("Enregistrer la correction")
    }

    // Historique, détail et statistiques (Story 7.4)

    @Test
    fun historyRowShowsBothTeamsAndTheWinner() {
        val entry = PartySummary(
            partyId = UUID.randomUUID(),
            gameId = "belote",
            createdAt = 0L,
            roundCount = 12,
            winners = listOf("Eux"),
            winningScore = 1012,
            teamScores = listOf(TeamScore("Nous", 840), TeamScore("Eux", 1012))
        )
        setContent { PartySummaryRow(entry = entry, onSelected = {}) }

        composeTestRule.onNodeWithText("Nous 840 · Eux 1012 — Eux gagne").assertExists()
        composeTestRule.onNodeWithText("Belote", substring = true).assertExists()
    }

    @Test
    fun detailIsReadOnlyAndReplays() {
        var replayed = false
        val state = BelotePartyDetailUiState.Loaded(
            createdAt = 0L,
            teams = loadedParty.teams.map { it.copy(isLeader = it.side == TeamSide.SECOND) },
            rounds = loadedParty.rounds
        )
        setContent { BelotePartyDetailScreen(state, onReplay = { replayed = true }) }

        composeTestRule.onNodeWithTag("belote_detail_subtitle").assertTextContains("Belote", substring = true)
        composeTestRule.onNodeWithTag("belote_round_row_1").assertHasNoClickAction()
        composeTestRule.onNodeWithTag("belote_detail_replay").performClick()

        assertEquals(true, replayed)
    }

    @Test
    fun statsDrawTheChartOnceARoundIsPlayed() {
        val series = listOf("Nous" to listOf(0, 100, 181), "Eux" to listOf(0, 62, 62)).map { (name, points) ->
            ScoreSeries(RoundTablePlayer(UUID.randomUUID(), name), points)
        }
        setContent { BeloteStatsScreen(BeloteStatsUiState.Loaded(series)) }

        composeTestRule.onNodeWithTag("score_chart_plot").assertExists()
        composeTestRule.onNodeWithTag("belote_stats_no_round").assertDoesNotExist()
    }

    @Test
    fun statsWithoutRoundSaySo() {
        val series = listOf("Nous", "Eux").map { ScoreSeries(RoundTablePlayer(UUID.randomUUID(), it), listOf(0)) }
        setContent { BeloteStatsScreen(BeloteStatsUiState.Loaded(series)) }

        composeTestRule.onNodeWithTag("belote_stats_no_round").assertExists()
        composeTestRule.onNodeWithTag("score_chart_plot").assertDoesNotExist()
    }

    // Results

    @Test
    fun resultsShowTheWinnerAndReplay() {
        var replayed = false
        val state = BeloteResultsUiState.Loaded(
            teams = listOf(
                BeloteTeamUiModel(TeamSide.SECOND, "Eux", listOf("Bob", "David"), 1012, isLeader = true),
                BeloteTeamUiModel(TeamSide.FIRST, "Nous", listOf("Alice", "Chloé"), 840, isLeader = false)
            ),
            roundCount = 12
        )
        setContent { BeloteResultsScreen(state, onReplay = { replayed = true }) }

        composeTestRule.onNodeWithTag("belote_results_winner").assertTextContains("Eux gagne", substring = true)
        composeTestRule.onNodeWithText("1012 points").assertExists()
        composeTestRule.onNodeWithTag("belote_results_stats").assertTextContains("Statistiques (12)")
        composeTestRule.onNodeWithTag("belote_results_replay").performClick()

        assertEquals(true, replayed)
    }
}

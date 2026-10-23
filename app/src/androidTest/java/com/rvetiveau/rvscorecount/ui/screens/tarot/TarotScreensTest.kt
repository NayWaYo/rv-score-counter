package com.rvetiveau.rvscorecount.ui.screens.tarot

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import com.rvetiveau.rvscorecount.domain.model.EndRule
import com.rvetiveau.rvscorecount.domain.model.TarotContract
import com.rvetiveau.rvscorecount.domain.model.TarotDealing
import com.rvetiveau.rvscorecount.ui.components.RoundTable
import com.rvetiveau.rvscorecount.ui.components.RoundTablePlayer
import com.rvetiveau.rvscorecount.ui.components.RoundTableRow
import com.rvetiveau.rvscorecount.ui.components.StandingRow
import com.rvetiveau.rvscorecount.ui.components.TakerUiModel
import com.rvetiveau.rvscorecount.ui.screens.setup.ParticipantDraft
import com.rvetiveau.rvscorecount.ui.theme.RvScoreCounterTheme
import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/** The creation, the Partie and the saisie of a Manche of Tarot (Story 13.3), stateless. */
class TarotScreensTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val calls = mutableListOf<Any>()

    private val alice = TarotSeat(UUID.randomUUID(), "Alice")
    private val bob = TarotSeat(UUID.randomUUID(), "Bob")
    private val chloe = TarotSeat(UUID.randomUUID(), "Chloé")
    private val david = TarotSeat(UUID.randomUUID(), "David")
    private val eve = TarotSeat(UUID.randomUUID(), "Eve")

    private fun setSetup(state: TarotSetupUiState) {
        composeTestRule.setContent {
            RvScoreCounterTheme {
                TarotSetupScreen(
                    state = state,
                    actions = TarotSetupActions(
                        onRoundLimitChanged = { calls += it },
                        onRoundCountStep = { calls += "step $it" },
                        onStartParty = { calls += "start" }
                    ),
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    @Test
    fun setupStartsWithThreeJoueursAndShowsTheRoundCountWhenChosen() {
        val three = listOf("Alice", "Bob", "Chloé").map { ParticipantDraft(name = it) }
        setSetup(TarotSetupUiState(participants = three, hasRoundLimit = true, roundCount = 8))

        composeTestRule.onNodeWithTag("tarot_setup_title").assertTextEquals("Nouvelle partie de Tarot")
        composeTestRule.onNodeWithTag("tarot_setup_end_rounds").assertIsSelected()
        composeTestRule.onNodeWithTag("tarot_setup_round_count").assertTextEquals("8")
        composeTestRule.onNodeWithContentDescription("Une manche de moins").assertHeightIsAtLeast(48.dp).performClick()
        composeTestRule.onNodeWithContentDescription("Une manche de plus").performClick()
        composeTestRule.onNodeWithTag("tarot_setup_end_manual").performClick()
        composeTestRule.onNodeWithTag("tarot_setup_start").assertIsEnabled().performClick()

        assertEquals(listOf<Any>("step -1", "step 1", false, "start"), calls)
    }

    @Test
    fun setupCannotStartWithTwoJoueursAndTheCountStopsAtOne() {
        setSetup(
            TarotSetupUiState(
                participants = listOf(ParticipantDraft(name = "Alice"), ParticipantDraft(name = "Bob")),
                hasRoundLimit = true,
                roundCount = 1
            )
        )
        composeTestRule.onNodeWithTag("tarot_setup_start").assertIsNotEnabled()
        composeTestRule.onNodeWithTag("tarot_setup_round_count_minus").assertIsNotEnabled()
        composeTestRule.onNodeWithTag("tarot_setup_round_count_plus").assertIsEnabled()
    }

    private val players = listOf(alice, bob, chloe, david)

    private fun points(vararg byPlayer: Int) = players.zip(byPlayer.toList()).associate { (seat, points) -> seat.participantId to points }

    private fun party(isFinished: Boolean = false, isResults: Boolean = false) = TarotPartyUiState.Loaded(
        endRule = EndRule.RoundCount(3),
        isFinished = isFinished,
        standings = listOf(
            StandingRow(alice.participantId, 1, "Alice", 138, isDealer = false),
            StandingRow(chloe.participantId, 2, "Chloé", 2, isDealer = !isFinished),
            StandingRow(david.participantId, 2, "David", 2, isDealer = false),
            StandingRow(bob.participantId, 4, "Bob", -142, isDealer = false)
        ),
        winnerNames = if (isFinished) listOf("Alice") else emptyList(),
        progress = if (isFinished) null else TarotProgress(3, 3),
        playerCount = 4,
        dealing = TarotDealing(18, 6),
        contracts = listOf(
            TarotContractLine(1, "Alice", "Chloé", false, TarotContract.GARDE, made = true, margin = 9),
            TarotContractLine(2, "Bob", null, true, TarotContract.PRISE, made = false, margin = 11)
        ),
        roundTable = RoundTable(
            players = players.map { RoundTablePlayer(it.participantId, it.name) },
            rows = listOf(
                RoundTableRow(1, null, points(102, -34, -34, -34), highestIsBest = true),
                RoundTableRow(2, null, points(36, -108, 36, 36), highestIsBest = true)
            )
        ),
        nextRoundNumber = 3,
        isResults = isResults
    )

    private fun setParty(state: TarotPartyUiState) {
        composeTestRule.setContent {
            RvScoreCounterTheme {
                TarotPartyScreen(
                    state = state,
                    actions = TarotPartyActions(
                        onPlayRound = { calls += "play" },
                        onRoundSelected = { calls += it },
                        onEndPartyClick = { calls += "end" },
                        onReplay = { calls += "replay" },
                        onShowStats = { calls += "stats" }
                    ),
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    @Test
    fun aPartieInProgressShowsItsProgressDealingAndContracts() {
        setParty(party())

        composeTestRule.onNodeWithTag("tarot_party_title").assertTextEquals("Tarot")
        composeTestRule.onNodeWithTag("tarot_party_rules").assertTextEquals("Fin après 3 manches")
        composeTestRule.onNodeWithTag("tarot_party_progress").assertTextEquals("Manche 3 sur 3")
        composeTestRule.onNodeWithTag("tarot_party_dealing")
            .assertTextEquals("4 joueurs : distribuez 3 par 3, 18 cartes chacun, 6 au chien.")
        composeTestRule.onNodeWithTag("tarot_party_contract_1").performScrollTo()
            .assertTextEquals("1. Alice avec Chloé · Garde · faite de 9")
            .assertHeightIsAtLeast(48.dp)
        composeTestRule.onNodeWithTag("tarot_party_contract_2").performScrollTo()
            .assertTextEquals("2. Bob seul · Prise · chutée de 11")
            .performClick()
        composeTestRule.onNodeWithTag("tarot_party_play_round").assertTextEquals("Manche 3").performClick()
        composeTestRule.onNodeWithTag("tarot_party_end").performClick()

        assertEquals(listOf<Any>(2, "play", "end"), calls)
    }

    @Test
    fun theResultsShowTheWinnersAndReplay() {
        setParty(party(isFinished = true, isResults = true))

        composeTestRule.onNodeWithTag("tarot_party_winners").assertTextEquals("Vainqueur : Alice")
        composeTestRule.onNodeWithTag("tarot_party_play_round").assertDoesNotExist()
        composeTestRule.onNodeWithTag("tarot_party_dealing").assertDoesNotExist()
        composeTestRule.onNodeWithTag("tarot_results_replay").performClick()

        assertEquals(listOf<Any>("replay"), calls)
    }

    private fun setRound(state: TarotRoundUiState) {
        composeTestRule.setContent {
            RvScoreCounterTheme {
                TarotRoundScreen(
                    state = state,
                    actions = TarotRoundActions(
                        onTakerSelected = { calls += it },
                        onContractSelected = { calls += it },
                        onPartnerSelected = { calls += it },
                        onValidate = { calls += "validate" }
                    ),
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    private val blankRound = TarotRoundUiState.Loaded(
        roundNumber = 4,
        isCorrection = false,
        players = players,
        callsPartner = false,
        totalPoints = 91
    )

    @Test
    fun anIncompleteMancheCannotBeValidatedAndHasNoPartnerBelowFive() {
        setRound(blankRound)

        composeTestRule.onNodeWithTag("tarot_round_title").assertTextEquals("Manche 4")
        composeTestRule.onNodeWithTag("tarot_round_partner_alone").assertDoesNotExist()
        composeTestRule.onNodeWithTag("tarot_round_taker_${bob.participantId}").assertHeightIsAtLeast(48.dp).performClick()
        composeTestRule.onNodeWithTag("tarot_round_contract_GARDE_CONTRE").performClick()
        composeTestRule.onNodeWithTag("tarot_round_validate").assertIsNotEnabled()

        assertEquals(listOf<Any>(bob.participantId, TarotContract.GARDE_CONTRE), calls)
    }

    @Test
    fun aCompleteMancheShowsItsOutcomeAndEveryJoueursPoints() {
        setRound(
            blankRound.copy(
                takerId = alice.participantId,
                contract = TarotContract.GARDE,
                bouts = 2,
                pointsText = "50",
                requiredPoints = 41,
                preview = TarotRoundPreview(41, true, 9, 68, listOf("Alice" to 204, "Bob" to -68, "Chloé" to -68, "David" to -68))
            )
        )

        composeTestRule.onNodeWithTag("tarot_round_taker_${alice.participantId}").assertIsSelected()
        composeTestRule.onNodeWithTag("tarot_round_points_hint").assertTextEquals("Défense : 41 · à faire avec 2 bouts : 41")
        composeTestRule.onNodeWithTag("tarot_round_outcome").performScrollTo()
            .assertTextEquals("Contrat fait de 9 (41 à faire) : 68 par défenseur")
        composeTestRule.onNodeWithTag("tarot_round_points_of_Alice").assertTextEquals("Alice : +204")
        composeTestRule.onNodeWithTag("tarot_round_points_of_Bob").assertTextEquals("Bob : -68")
        composeTestRule.onNodeWithTag("tarot_round_validate").assertIsEnabled().performClick()

        assertEquals(listOf<Any>("validate"), calls)
    }

    @Test
    fun atFiveThePartnerIsAnotherJoueurOrAlone() {
        setRound(blankRound.copy(players = players + eve, callsPartner = true, takerId = alice.participantId))

        composeTestRule.onNodeWithTag("tarot_round_partner_${alice.participantId}").assertDoesNotExist()
        composeTestRule.onNodeWithTag("tarot_round_partner_alone").performClick()
        composeTestRule.onNodeWithTag("tarot_round_partner_${eve.participantId}").performClick()

        assertEquals(listOf<Any>(TarotPartnerChoice.Alone, TarotPartnerChoice.Player(eve.participantId)), calls)
    }

    // --- Story 13.4 ---

    @Test
    fun theStatistiquesLinkCountsTheManches() {
        setParty(party(isFinished = true))

        composeTestRule.onNodeWithTag("tarot_stats_link").assertTextEquals("Statistiques (2)").performClick()

        assertEquals(listOf<Any>("stats"), calls)
    }

    @Test
    fun theStatistiquesShowTheCurveAndTheChadCard() {
        composeTestRule.setContent {
            RvScoreCounterTheme {
                TarotStatsScreen(
                    state = TarotStatsUiState.Loaded(
                        table = party().roundTable,
                        takers = listOf(TakerUiModel("Alice", 1, 1), TakerUiModel("Bob", 1, 0), TakerUiModel("Chloé", 0, 0))
                    ),
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        composeTestRule.onNodeWithTag("tarot_stats_title").assertTextEquals("Statistiques")
        composeTestRule.onNodeWithTag("tarot_stats_chart").assertIsDisplayed()
        composeTestRule.onNodeWithTag("stat_card_chad").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun statistiquesBeforeTheFirstMancheSaySo() {
        composeTestRule.setContent {
            RvScoreCounterTheme { TarotStatsScreen(state = TarotStatsUiState.Loaded(RoundTable(), emptyList())) }
        }
        composeTestRule.onNodeWithTag("tarot_stats_no_round").assertIsDisplayed()
        composeTestRule.onNodeWithTag("stat_card_chad").assertDoesNotExist()
    }
}

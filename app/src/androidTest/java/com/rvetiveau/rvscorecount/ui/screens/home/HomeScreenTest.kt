package com.rvetiveau.rvscorecount.ui.screens.home

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.rvetiveau.rvscorecount.R
import com.rvetiveau.rvscorecount.ui.theme.RvScoreCounterTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Instrumented tests for the pure [HomeScreen] (AC 1-3, 6, 7): three fake tiles are the
 * minimum needed to prove a genuine 2-column grid layout, not just a single Papayoo tile. */
class HomeScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val threeTiles = listOf(
        GameTileUiModel("papayoo", R.string.game_name_papayoo, R.drawable.ic_papayoo_logo),
        GameTileUiModel("fake-a", android.R.string.ok, R.drawable.ic_history),
        GameTileUiModel("fake-b", android.R.string.cancel, R.drawable.ic_papayoo_logo)
    )

    @Test
    fun displaysTilesInATwoColumnGrid() {
        setContent(threeTiles)

        val papayooBounds = composeTestRule.onNodeWithTag("game_tile_papayoo").getUnclippedBoundsInRoot()
        val fakeABounds = composeTestRule.onNodeWithTag("game_tile_fake-a").getUnclippedBoundsInRoot()
        val fakeBBounds = composeTestRule.onNodeWithTag("game_tile_fake-b").getUnclippedBoundsInRoot()

        assertEquals(papayooBounds.top, fakeABounds.top)
        assertTrue(papayooBounds.left != fakeABounds.left)
        assertTrue(fakeBBounds.top > papayooBounds.top)
    }

    @Test
    fun logoFillsTheWholeTileWithTheNameInItsBottomHalf() {
        setContent(threeTiles)

        val tileBounds = composeTestRule.onNodeWithTag("game_tile_papayoo").getUnclippedBoundsInRoot()
        val logoBounds = composeTestRule.onNodeWithTag("game_tile_logo_papayoo", useUnmergedTree = true)
            .getUnclippedBoundsInRoot()
        val nameBounds = composeTestRule.onNodeWithTag("game_tile_name_papayoo", useUnmergedTree = true)
            .getUnclippedBoundsInRoot()

        assertEquals(tileBounds, logoBounds)
        assertTrue(nameBounds.top > tileBounds.top + (tileBounds.bottom - tileBounds.top) / 2)
        assertTrue(nameBounds.bottom <= tileBounds.bottom)
        composeTestRule.onNodeWithTag("game_tile_papayoo")
            .assertWidthIsAtLeast(48.dp)
            .assertHeightIsAtLeast(48.dp)
    }

    @Test
    fun showsEachGameNameAndTheHistoryAction() {
        setContent(threeTiles)

        composeTestRule.onNodeWithText("Papayoo").assertExists()
        composeTestRule.onNodeWithContentDescription("Historique").assertExists()
    }

    @Test
    fun clickingThePapayooTileInvokesTheCallbackWithItsExactGameId() {
        var selectedGameId: String? = null
        setContent(threeTiles, onGameSelected = { selectedGameId = it })

        composeTestRule.onNodeWithTag("game_tile_papayoo").performClick()

        assertEquals("papayoo", selectedGameId)
    }

    @Test
    fun clickingTheHistoryActionInvokesItsCallback() {
        var historySelected = false
        setContent(threeTiles, onHistorySelected = { historySelected = true })

        composeTestRule.onNodeWithTag("home_history_action").performClick()

        assertTrue(historySelected)
    }

    @Test
    fun thePlayersActionIsDescribedAndInvokesItsCallback() {
        var playersSelected = false
        setContent(threeTiles, onPlayersSelected = { playersSelected = true })

        composeTestRule.onNodeWithContentDescription("Joueurs").performClick()

        assertTrue(playersSelected)
    }

    @Test
    fun theBackupActionIsDescribedAndInvokesItsCallback() {
        var backupSelected = false
        setContent(threeTiles, onBackupSelected = { backupSelected = true })

        composeTestRule.onNodeWithContentDescription("Sauvegarde").performClick()

        assertTrue(backupSelected)
    }

    @Test
    fun tileExposesButtonRoleAndAnExplicitContentDescription() {
        setContent(threeTiles)

        composeTestRule.onNodeWithTag("game_tile_papayoo")
            .assertHasClickAction()
            .assertContentDescriptionEquals("Cr\u00e9er une partie de Papayoo")
    }

    @Test
    fun tileNameIsNotTruncatedAtALargeFontScale() {
        composeTestRule.setContent {
            val largeFontDensity = Density(density = LocalDensity.current.density, fontScale = 2f)
            CompositionLocalProvider(LocalDensity provides largeFontDensity) {
                RvScoreCounterTheme {
                    HomeScreen(state = HomeUiState(tiles = threeTiles), onGameSelected = {}, onHistorySelected = {})
                }
            }
        }

        val textLayoutResults = mutableListOf<androidx.compose.ui.text.TextLayoutResult>()
        val getTextLayoutResult = composeTestRule
            .onNodeWithTag("game_tile_name_papayoo", useUnmergedTree = true)
            .fetchSemanticsNode()
            .config[SemanticsActions.GetTextLayoutResult]

        assertTrue(getTextLayoutResult.action?.invoke(textLayoutResults) == true)
        assertTrue(textLayoutResults.none { it.hasVisualOverflow })
    }

    private fun setContent(
        tiles: List<GameTileUiModel>,
        onGameSelected: (String) -> Unit = {},
        onHistorySelected: () -> Unit = {},
        onPlayersSelected: () -> Unit = {},
        onBackupSelected: () -> Unit = {}
    ) {
        composeTestRule.setContent {
            RvScoreCounterTheme {
                HomeScreen(
                    state = HomeUiState(tiles = tiles),
                    onGameSelected = onGameSelected,
                    onHistorySelected = onHistorySelected,
                    onPlayersSelected = onPlayersSelected,
                    onBackupSelected = onBackupSelected
                )
            }
        }
    }
}

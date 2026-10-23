package com.rvetiveau.rvscorecount.ui.screens.backup

import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.rvetiveau.rvscorecount.domain.model.BackupContent
import com.rvetiveau.rvscorecount.ui.theme.RvScoreCounterTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Rendering and callbacks of the pure [BackupScreen] (Story 12.1); rules are in [BackupViewModelTest]. */
class BackupScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun setContent(state: BackupUiState, onExportRequested: () -> Unit = {}) {
        composeTestRule.setContent {
            RvScoreCounterTheme {
                BackupScreen(state = state, onExportRequested = onExportRequested)
            }
        }
    }

    @Test
    fun announcesWhatTheBackupHoldsAndExportsOnTap() {
        var exportRequested = false
        setContent(BackupUiState.Loaded(BackupContent(players = 4, parties = 1)), onExportRequested = { exportRequested = true })

        composeTestRule.onNodeWithTag("backup_title").assertTextEquals("Sauvegarde")
        composeTestRule.onNodeWithTag("backup_content")
            .assertTextEquals("La sauvegarde contiendra 4 joueurs et 1 partie, parties en cours comprises.")
        composeTestRule.onNodeWithTag("backup_export").assertHeightIsAtLeast(48.dp).assertIsEnabled().performClick()

        assertTrue(exportRequested)
    }

    @Test
    fun nothingToSaveTurnsExportOff() {
        setContent(BackupUiState.Loaded(BackupContent(players = 0, parties = 0)))

        composeTestRule.onNodeWithTag("backup_content")
            .assertTextEquals("Rien à sauvegarder pour l'instant : jouez une première partie.")
        composeTestRule.onNodeWithTag("backup_export").assertIsNotEnabled()
    }

    @Test
    fun exportIsOffWhileWriting() {
        setContent(BackupUiState.Loaded(BackupContent(2, 3), isExporting = true))

        composeTestRule.onNodeWithTag("backup_export").assertIsNotEnabled()
    }

    @Test
    fun showsTheOutcomeOfTheLastExport() {
        setContent(BackupUiState.Loaded(BackupContent(2, 3), exportResult = ExportResult.EXPORTED))
        composeTestRule.onNodeWithTag("backup_exported").assertTextEquals("Sauvegarde enregistrée.")
    }

    @Test
    fun showsAFailedExport() {
        setContent(BackupUiState.Loaded(BackupContent(2, 3), exportResult = ExportResult.FAILED))
        composeTestRule.onNodeWithTag("backup_export_failed")
            .assertTextEquals("Impossible d'enregistrer la sauvegarde. Réessayez.")
    }

    @Test
    fun showsALoadError() {
        setContent(BackupUiState.LoadError)
        composeTestRule.onNodeWithTag("backup_load_error").assertIsDisplayed()
    }
}

package com.rvetiveau.rvscorecount.ui.screens.backup

import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import com.rvetiveau.rvscorecount.domain.model.BackupContent
import com.rvetiveau.rvscorecount.ui.theme.RvScoreCounterTheme
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/** Rendering and callbacks of the pure [BackupScreen] (Epic 12); rules are in [BackupViewModelTest]. */
class BackupScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val calls = mutableListOf<String>()

    private fun setContent(state: BackupUiState) {
        composeTestRule.setContent {
            RvScoreCounterTheme {
                BackupScreen(
                    state = state,
                    onExportRequested = { calls += "export" },
                    onRestoreRequested = { calls += "restore" },
                    onRestoreConfirmed = { calls += "confirm" },
                    onRestoreDismissed = { calls += "dismiss" }
                )
            }
        }
    }

    @Test
    fun announcesWhatTheBackupHoldsAndExportsOrRestoresOnTap() {
        setContent(BackupUiState.Loaded(BackupContent(players = 4, parties = 1)))

        composeTestRule.onNodeWithTag("backup_title").assertTextEquals("Sauvegarde")
        composeTestRule.onNodeWithTag("backup_content")
            .assertTextEquals("La sauvegarde contiendra 4 joueurs et 1 partie, parties en cours comprises.")
        composeTestRule.onNodeWithTag("backup_export").assertHeightIsAtLeast(48.dp).assertIsEnabled().performClick()
        composeTestRule.onNodeWithTag("backup_restore").performScrollTo().assertHeightIsAtLeast(48.dp).performClick()

        assertEquals(listOf("export", "restore"), calls)
    }

    @Test
    fun nothingToSaveTurnsExportOffButNotRestore() {
        setContent(BackupUiState.Loaded(BackupContent(players = 0, parties = 0)))

        composeTestRule.onNodeWithTag("backup_content")
            .assertTextEquals("Rien à sauvegarder pour l'instant : jouez une première partie.")
        composeTestRule.onNodeWithTag("backup_export").assertIsNotEnabled()
        composeTestRule.onNodeWithTag("backup_restore").assertIsEnabled()
    }

    @Test
    fun bothActionsAreOffWhileAnOperationRuns() {
        setContent(BackupUiState.Loaded(BackupContent(2, 3), operation = BackupOperation.READING))

        composeTestRule.onNodeWithTag("backup_export").assertIsNotEnabled()
        composeTestRule.onNodeWithTag("backup_restore").assertIsNotEnabled()
        composeTestRule.onNodeWithTag("backup_progress").assertIsDisplayed()
    }

    @Test
    fun showsTheOutcomeOfTheLastOperation() {
        setContent(BackupUiState.Loaded(BackupContent(2, 3), message = BackupMessage.NEWER_FORMAT))
        composeTestRule.onNodeWithTag("backup_message").assertTextEquals(
            "Cette sauvegarde vient d'une version plus récente de l'application : mettez l'application à jour. Rien n'a été modifié."
        )
    }

    @Test
    fun theConfirmationNamesTheBackupAndReplacesOrCancels() {
        val createdAt = LocalDate.of(2026, 10, 23).atTime(19, 5).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        setContent(
            BackupUiState.Loaded(
                BackupContent(2, 3),
                pendingRestore = PendingRestore("content://backup", createdAt, BackupContent(players = 6, parties = 12))
            )
        )

        composeTestRule.onNodeWithTag("backup_restore_question").assertTextEquals(
            "Remplacer toutes les données actuelles par la sauvegarde du 23/10/2026 (6 joueurs et 12 parties) ? " +
                "Les joueurs et parties actuels seront perdus."
        )
        composeTestRule.onNodeWithTag("backup_restore_cancel").performClick()
        composeTestRule.onNodeWithTag("backup_restore_confirm").performClick()

        assertEquals(listOf("dismiss", "confirm"), calls)
    }

    @Test
    fun showsALoadError() {
        setContent(BackupUiState.LoadError)
        composeTestRule.onNodeWithTag("backup_load_error").assertIsDisplayed()
    }
}

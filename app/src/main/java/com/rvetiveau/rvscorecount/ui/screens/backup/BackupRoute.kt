package com.rvetiveau.rvscorecount.ui.screens.backup

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.LocalDate

/**
 * Compose/Hilt adapter for « Sauvegarde » (Epic 12): the system file picker chooses where the backup
 * goes and which one to restore.
 */
@Composable
fun BackupRoute(
    modifier: Modifier = Modifier,
    viewModel: BackupViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val chooseDestination = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri -> viewModel.onExportDestinationChosen(uri?.toString()) }
    val chooseSource = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> viewModel.onRestoreSourceChosen(uri?.toString()) }

    BackupScreen(
        state = state,
        onExportRequested = { chooseDestination.launch(backupFileName(LocalDate.now())) },
        onRestoreRequested = { chooseSource.launch(BACKUP_SOURCE_TYPES) },
        onRestoreConfirmed = viewModel::onRestoreConfirmed,
        onRestoreDismissed = viewModel::onRestoreDismissed,
        modifier = modifier
    )
}

// Some file managers and Drive report a .json file as plain text or bytes.
private val BACKUP_SOURCE_TYPES = arrayOf("application/json", "text/plain", "application/octet-stream")

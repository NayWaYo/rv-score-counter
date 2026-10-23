package com.rvetiveau.rvscorecount.ui.screens.backup

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.LocalDate

/** Compose/Hilt adapter for « Sauvegarde » (Story 12.1): the system file picker chooses where the file goes. */
@Composable
fun BackupRoute(
    modifier: Modifier = Modifier,
    viewModel: BackupViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val chooseDestination = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri -> viewModel.onExportDestinationChosen(uri?.toString()) }

    BackupScreen(
        state = state,
        onExportRequested = { chooseDestination.launch(backupFileName(LocalDate.now())) },
        modifier = modifier
    )
}

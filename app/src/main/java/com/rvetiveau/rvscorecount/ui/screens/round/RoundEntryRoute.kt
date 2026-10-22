package com.rvetiveau.rvscorecount.ui.screens.round

import android.content.ActivityNotFoundException
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.keepScreenOn
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import java.io.File
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rvetiveau.rvscorecount.ui.navigation.NavigationEvent
import java.util.UUID

/**
 * Compose/Hilt adapter for the Saisie de Manche screen (AC 1), or for the correction of the validated
 * Manche [correctedRoundNumber] when set: loads Room state for [partyId]
 * once via [RoundEntryViewModel.initialize] and relays the ViewModel's one-shot
 * [NavigationEvent]s to [onNavigate].
 */
@Composable
fun RoundEntryRoute(
    partyId: UUID,
    correctedRoundNumber: Int? = null,
    onNavigate: (NavigationEvent) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RoundEntryViewModel = hiltViewModel()
) {
    LaunchedEffect(partyId, correctedRoundNumber) { viewModel.initialize(partyId, correctedRoundNumber) }

    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val currentOnNavigate by rememberUpdatedState(onNavigate)

    LaunchedEffect(viewModel) {
        viewModel.navigationEvents.collect { event -> currentOnNavigate(event) }
    }

    // Photos of trumps (Story 9.1) go through the phone's camera app: the app needs no permission.
    val context = LocalContext.current
    val photoFile = remember(context) { File(File(context.cacheDir, SCAN_DIRECTORY), SCAN_FILE) }
    val takePhoto = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { taken ->
        viewModel.onPhotoTaken(photoFile.path.takeIf { taken })
    }
    LaunchedEffect(viewModel) {
        viewModel.photoRequests.collect {
            photoFile.parentFile?.mkdirs()
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.scans", photoFile)
            try {
                takePhoto.launch(uri)
            } catch (_: ActivityNotFoundException) {
                viewModel.onPhotoTaken(null)
            }
        }
    }

    RoundEntryScreen(
        state = state,
        onRoll = viewModel::onRoll,
        onScoreChanged = viewModel::onScoreChanged,
        onRetry = { viewModel.retry(partyId, correctedRoundNumber) },
        onValidate = viewModel::onValidate,
        onSevenHolderSelected = viewModel::onSevenHolderSelected,
        onPanelSelected = viewModel::onPanelSelected,
        scanActions = TrumpScanActions(
            onScanRequested = viewModel::onScanRequested,
            onTrumpToggled = viewModel::onScanTrumpToggled,
            onRetake = viewModel::onScanRetake,
            onDismiss = viewModel::onScanDismissed,
            onConfirm = viewModel::onScanConfirmed
        ),
        modifier = modifier.keepScreenOn()
    )
}

/** Matches `res/xml/scan_paths.xml`; one photo at a time, deleted once read. */
private const val SCAN_DIRECTORY = "scans"
private const val SCAN_FILE = "trumps.jpg"

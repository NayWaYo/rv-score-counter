package com.rvetiveau.rvscorecount

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rvetiveau.rvscorecount.ui.navigation.RootUiState
import com.rvetiveau.rvscorecount.ui.navigation.RootViewModel
import com.rvetiveau.rvscorecount.ui.navigation.RvScoreCounterNavHost
import com.rvetiveau.rvscorecount.ui.theme.RvScoreCounterTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RvScoreCounterTheme {
                // safeDrawing includes the IME: edge-to-edge ignores adjustResize, so the content
                // is padded above the keyboard here and focused fields can scroll into view.
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    contentWindowInsets = WindowInsets.safeDrawing
                ) { innerPadding ->
                    RvScoreCounterRoot(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

/**
 * Resolves the start destination once (resume vs Home) before composing the NavHost,
 * so the Accueil is never flashed while a Party is being resumed (AD-6, FR-16).
 */
@Composable
private fun RvScoreCounterRoot(
    modifier: Modifier = Modifier,
    rootViewModel: RootViewModel = hiltViewModel()
) {
    when (val state = rootViewModel.uiState.collectAsStateWithLifecycle().value) {
        RootUiState.Loading -> Box(modifier = modifier.fillMaxSize())
        is RootUiState.Ready -> RvScoreCounterNavHost(startDestination = state.startDestination, modifier = modifier)
        is RootUiState.Error -> RootErrorContent(modifier = modifier, onRetry = rootViewModel::retry)
    }
}

@Composable
private fun RootErrorContent(onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = stringResource(id = R.string.root_error_message))
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onRetry) {
                Text(text = stringResource(id = R.string.root_error_retry))
            }
        }
    }
}

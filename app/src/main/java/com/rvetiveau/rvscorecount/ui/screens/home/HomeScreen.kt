package com.rvetiveau.rvscorecount.ui.screens.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.rvetiveau.rvscorecount.R
import com.rvetiveau.rvscorecount.ui.components.ScreenHeader

/**
 * Stateless Accueil screen (AC 1-3, 6, 7): fixed 2-column grid of Game tiles plus the
 * Historique action. No NavController, Hilt access or business logic is allowed here.
 */
@Composable
fun HomeScreen(
    state: HomeUiState,
    onGameSelected: (String) -> Unit,
    onHistorySelected: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        HomeTopBar(onHistorySelected = onHistorySelected)
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxSize()
                .testTag("home_game_grid"),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(items = state.tiles, key = { it.gameId }) { tile ->
                GameTile(tile = tile, onClick = { onGameSelected(tile.gameId) })
            }
        }
    }
}

@Composable
private fun HomeTopBar(onHistorySelected: () -> Unit, modifier: Modifier = Modifier) {
    ScreenHeader(
        title = stringResource(R.string.home_title),
        titleTag = "home_title",
        modifier = modifier
            .heightIn(min = 56.dp)
            .padding(horizontal = 16.dp)
    ) {
        IconButton(
            onClick = onHistorySelected,
            modifier = Modifier.testTag("home_history_action")
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_history),
                contentDescription = stringResource(R.string.history_content_description),
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onBackground
            )
        }
    }
}

@Composable
private fun GameTile(tile: GameTileUiModel, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val name = stringResource(tile.nameRes)
    val tileContentDescription = stringResource(R.string.game_tile_content_description, name)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick, role = Role.Button)
            .semantics { contentDescription = tileContentDescription }
            .testTag("game_tile_${tile.gameId}"),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Image(
                painter = painterResource(tile.logoRes),
                contentDescription = null,
                modifier = Modifier
                    .size(72.dp)
                    .testTag("game_tile_logo_${tile.gameId}")
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = name,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp)
                    .testTag("game_tile_name_${tile.gameId}")
            )
        }
    }
}

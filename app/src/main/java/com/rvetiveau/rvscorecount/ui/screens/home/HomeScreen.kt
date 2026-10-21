package com.rvetiveau.rvscorecount.ui.screens.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.rvetiveau.rvscorecount.R
import com.rvetiveau.rvscorecount.ui.components.HistoryButton
import com.rvetiveau.rvscorecount.ui.components.ScreenHeader
import com.rvetiveau.rvscorecount.ui.theme.OnAccent

/**
 * Stateless Accueil screen (AC 1-3, 6, 7): fixed 2-column grid of Game tiles plus the
 * Historique action. No NavController, Hilt access or business logic is allowed here.
 * Each tile is the game's logo itself, a full-bleed square on an amber ground whose bottom band
 * is left free for the game's name.
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
        HistoryButton(onClick = onHistorySelected, modifier = Modifier.testTag("home_history_action"))
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
            .clip(LogoShape)
            .clickable(onClick = onClick, role = Role.Button)
            .semantics { contentDescription = tileContentDescription }
            .testTag("game_tile_${tile.gameId}")
    ) {
        Image(
            painter = painterResource(tile.logoRes),
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .testTag("game_tile_logo_${tile.gameId}")
        )
        Text(
            text = name,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = OnAccent,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(start = 8.dp, end = 8.dp, bottom = 16.dp)
                .testTag("game_tile_name_${tile.gameId}")
        )
    }
}

/** Corners of the game logos (24 on a 108 viewport), so the ripple follows the logo's edge. */
private val LogoShape = RoundedCornerShape(percent = 22)

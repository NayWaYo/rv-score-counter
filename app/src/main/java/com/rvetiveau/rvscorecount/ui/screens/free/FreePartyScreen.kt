package com.rvetiveau.rvscorecount.ui.screens.free

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.rvetiveau.rvscorecount.R
import com.rvetiveau.rvscorecount.domain.model.EndRule
import com.rvetiveau.rvscorecount.domain.model.WinDirection
import com.rvetiveau.rvscorecount.ui.components.ScreenHeader

/**
 * Stateless Partie of Jeu libre (Story 11.2): its game as title, its rules, then its Participants
 * in table order with the Donneur. The Manches, the Classement and the end come with Story 11.3.
 */
@Composable
fun FreePartyScreen(state: FreePartyUiState, modifier: Modifier = Modifier) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        ScreenHeader(
            title = (state as? FreePartyUiState.Loaded)?.gameName ?: stringResource(R.string.game_name_free),
            titleTag = "free_party_title",
            modifier = Modifier.padding(top = 8.dp)
        )
        when (state) {
            FreePartyUiState.Loading -> CircularProgressIndicator(modifier = Modifier.testTag("free_party_loading"))
            FreePartyUiState.LoadError -> Text(
                text = stringResource(R.string.round_entry_load_error),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.testTag("free_party_load_error")
            )
            is FreePartyUiState.Loaded -> {
                Text(
                    text = rulesText(state.winDirection, state.endRule),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.testTag("free_party_rules")
                )
                Text(text = stringResource(R.string.belote_setup_table_title), style = MaterialTheme.typography.titleMedium)
                state.participants.forEach { participant ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 56.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(horizontal = 16.dp)
                            .testTag("free_party_seat_${participant.id}")
                    ) {
                        Text(
                            text = participant.name,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        if (participant.id == state.dealerId) {
                            Text(
                                text = stringResource(R.string.setup_dealer_badge),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                    .testTag("free_party_dealer")
                            )
                        }
                    }
                }
            }
        }
    }
}

/** « Le plus petit total gagne · Fin à 100 points ». */
@Composable
private fun rulesText(direction: WinDirection, endRule: EndRule?): String {
    val winner = stringResource(if (direction == WinDirection.LOWEST_WINS) R.string.free_rules_lowest else R.string.free_rules_highest)
    val end = when (endRule) {
        null -> stringResource(R.string.free_rules_manual)
        is EndRule.TargetScore -> stringResource(R.string.free_rules_target, endRule.points)
        is EndRule.RoundCount -> stringResource(R.string.free_rules_rounds, endRule.rounds)
    }
    return "$winner · $end"
}

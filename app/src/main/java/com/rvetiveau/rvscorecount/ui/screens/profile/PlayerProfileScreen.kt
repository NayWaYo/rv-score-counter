package com.rvetiveau.rvscorecount.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.rvetiveau.rvscorecount.R
import com.rvetiveau.rvscorecount.domain.model.BeloteRecords
import com.rvetiveau.rvscorecount.domain.model.PapayooRecords
import com.rvetiveau.rvscorecount.domain.model.PlayerRecords
import com.rvetiveau.rvscorecount.domain.model.TeammateRecord
import com.rvetiveau.rvscorecount.domain.model.WinRecord
import com.rvetiveau.rvscorecount.ui.components.ScreenHeader
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Stateless profile of a Joueur (Story 10.3): a summary, then one card per Jeu they finished a
 * Partie of, its win rate first.
 */
@Composable
fun PlayerProfileScreen(
    state: PlayerProfileUiState,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ScreenHeader(
            title = (state as? PlayerProfileUiState.Loaded)?.records?.player?.name ?: stringResource(R.string.players_title),
            titleTag = "profile_title",
            modifier = Modifier.padding(top = 8.dp)
        )
        when (state) {
            PlayerProfileUiState.Loading -> Centered { CircularProgressIndicator(modifier = Modifier.testTag("profile_loading")) }
            PlayerProfileUiState.NotFound -> Centered { Message(stringResource(R.string.profile_not_found), "profile_not_found") }
            PlayerProfileUiState.LoadError -> Centered { Message(stringResource(R.string.profile_load_error), "profile_load_error") }
            is PlayerProfileUiState.Loaded -> Records(state.records)
        }
    }
}

@Composable
private fun Records(records: PlayerRecords) {
    val player = records.player
    val dateFormatter = remember { DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM) }
    val summary = player.lastPlayedAt?.let { lastPlayedAt ->
        pluralStringResource(R.plurals.players_finished_parties, player.finishedParties, player.finishedParties) +
            " · " + stringResource(R.string.players_last_played, formatDate(lastPlayedAt, dateFormatter))
    } ?: stringResource(R.string.players_no_party)
    Text(
        text = summary,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.testTag("profile_summary")
    )
    if (records.papayoo == null && records.belote == null) {
        Message(stringResource(R.string.profile_no_finished_party), "profile_no_finished_party")
        return
    }
    records.papayoo?.let { PapayooSection(it, dateFormatter) }
    records.belote?.let { BeloteSection(it) }
}

@Composable
private fun PapayooSection(records: PapayooRecords, dateFormatter: DateTimeFormatter) {
    GameCard(title = stringResource(R.string.game_name_papayoo), wins = records.wins, tag = "profile_papayoo") {
        StatLine(
            label = stringResource(R.string.profile_sevens_label),
            value = records.sevenRate?.let { stringResource(R.string.profile_sevens_value, records.sevensTaken, it.percent()) }
                ?: records.sevensTaken.toString(),
            tag = "profile_papayoo_sevens"
        )
        records.zeroRate?.let { rate ->
            StatLine(
                label = stringResource(R.string.profile_zero_label),
                value = stringResource(R.string.profile_zero_value, rate.percent(), records.zeroRounds),
                tag = "profile_papayoo_zeros"
            )
        }
        records.worstRound?.let { worst ->
            StatLine(
                label = stringResource(R.string.profile_worst_label),
                value = stringResource(R.string.profile_worst_value, worst.points, formatDate(worst.playedAt, dateFormatter)),
                tag = "profile_papayoo_worst"
            )
        }
        StatLine(
            label = stringResource(R.string.profile_full_label, records.fullRoundPoints),
            value = records.fullRounds.toString(),
            tag = "profile_papayoo_full"
        )
    }
}

@Composable
private fun BeloteSection(records: BeloteRecords) {
    GameCard(title = stringResource(R.string.game_name_belote), wins = records.wins, tag = "profile_belote") {
        val perParty = String.format(Locale.FRANCE, "%.1f", records.takesPerParty)
        StatLine(
            label = stringResource(R.string.profile_takes_label),
            value = records.contractRate?.let { stringResource(R.string.profile_takes_value, perParty, it.percent()) }
                ?: stringResource(R.string.profile_takes_value_no_rate, perParty),
            tag = "profile_belote_takes"
        )
        records.bestTeammate?.let { TeammateLine(R.string.profile_best_teammate_label, it, "profile_belote_best_teammate") }
        records.worstTeammate?.let { TeammateLine(R.string.profile_worst_teammate_label, it, "profile_belote_worst_teammate") }
    }
}

@Composable
private fun TeammateLine(labelRes: Int, teammate: TeammateRecord, tag: String) {
    val together = pluralStringResource(R.plurals.profile_parties_together, teammate.wins.parties, teammate.wins.parties)
    StatLine(
        label = stringResource(labelRes),
        value = stringResource(R.string.profile_teammate_value, teammate.name, teammate.wins.rate.percent(), together),
        tag = tag
    )
}

/** A Jeu's card: its win rate in large, the wins over the Parties under it, then [content]. */
@Composable
private fun GameCard(title: String, wins: WinRecord, tag: String, content: @Composable () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .testTag(tag)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(16.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
            Text(
                text = stringResource(R.string.profile_win_rate_label),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = stringResource(R.string.profile_percent, wins.rate.percent()),
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.testTag("${tag}_win_rate")
            )
            Text(
                text = pluralStringResource(R.plurals.profile_wins, wins.wins, wins.wins) + " " +
                    pluralStringResource(R.plurals.profile_out_of_parties, wins.parties, wins.parties),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.testTag("${tag}_wins")
            )
            content()
        }
    }
}

@Composable
private fun StatLine(label: String, value: String, tag: String) {
    Row(verticalAlignment = Alignment.Top, modifier = Modifier.fillMaxWidth().testTag(tag)) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.End,
            modifier = Modifier
                .weight(1.4f)
                .testTag("${tag}_value")
        )
    }
}

@Composable
private fun Message(text: String, tag: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.testTag(tag)
    )
}

@Composable
private fun Centered(content: @Composable () -> Unit) {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth().padding(top = 32.dp)) { content() }
}

private fun Double.percent(): Int = (this * 100).roundToInt()

private fun formatDate(epochMillis: Long, formatter: DateTimeFormatter): String =
    Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).format(formatter)

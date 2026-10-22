package com.rvetiveau.rvscorecount.ui.screens.profile

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rvetiveau.rvscorecount.R
import com.rvetiveau.rvscorecount.domain.engine.GameIds
import com.rvetiveau.rvscorecount.domain.model.BeloteRecords
import com.rvetiveau.rvscorecount.domain.model.PapayooRecords
import com.rvetiveau.rvscorecount.domain.model.PlayerRecords
import com.rvetiveau.rvscorecount.domain.model.TeammateRecord
import com.rvetiveau.rvscorecount.domain.model.WinRecord
import com.rvetiveau.rvscorecount.ui.components.ScreenHeader
import com.rvetiveau.rvscorecount.ui.components.gamePresentation
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Stateless profile of a Joueur (Story 10.3): two summary chips, then one card per Jeu they finished
 * a Partie of. Each card leads with the win rate and its bar, then tiles of two (a large figure and
 * a short caption, so that nothing wraps mid-value), then the teammates for the Belote.
 */
@Composable
fun PlayerProfileScreen(
    state: PlayerProfileUiState,
    modifier: Modifier = Modifier,
    actions: ProfileActions = ProfileActions()
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
            is PlayerProfileUiState.Loaded -> {
                Records(state.records)
                ProfileButtons(state, actions)
                ProfileDialogHost(state, actions)
            }
        }
    }
}

@Composable
private fun Records(records: PlayerRecords) {
    val player = records.player
    val dateFormatter = remember { DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM) }
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.testTag("profile_summary")
    ) {
        val lastPlayedAt = player.lastPlayedAt
        if (lastPlayedAt != null) {
            SummaryChip(pluralStringResource(R.plurals.players_finished_parties, player.finishedParties, player.finishedParties))
            SummaryChip(stringResource(R.string.profile_last_played, formatDate(lastPlayedAt, dateFormatter)))
        } else {
            SummaryChip(stringResource(R.string.players_no_party))
        }
    }
    if (records.papayoo == null && records.belote == null) {
        Message(stringResource(R.string.profile_no_finished_party), "profile_no_finished_party")
        return
    }
    records.papayoo?.let { PapayooSection(it, dateFormatter) }
    records.belote?.let { BeloteSection(it) }
}

@Composable
private fun PapayooSection(records: PapayooRecords, dateFormatter: DateTimeFormatter) {
    GameCard(gameId = GameIds.PAPAYOO, wins = records.wins, tag = "profile_papayoo") {
        TileRow(
            start = {
                StatTile(
                    label = stringResource(R.string.profile_sevens_label),
                    value = records.sevensTaken.toString(),
                    caption = records.sevenRate?.let { stringResource(R.string.profile_sevens_caption, it.percent()) }
                        ?: stringResource(R.string.profile_sevens_caption_unknown),
                    tag = "profile_papayoo_sevens"
                )
            },
            end = {
                StatTile(
                    label = stringResource(R.string.profile_zero_label),
                    value = records.zeroRate?.let { stringResource(R.string.profile_percent, it.percent()) }
                        ?: stringResource(R.string.profile_no_value),
                    caption = pluralStringResource(R.plurals.profile_rounds, records.zeroRounds, records.zeroRounds),
                    tag = "profile_papayoo_zeros"
                )
            }
        )
        TileRow(
            start = {
                val worst = records.worstRound
                StatTile(
                    label = stringResource(R.string.profile_worst_label),
                    value = worst?.let { stringResource(R.string.profile_points, it.points) } ?: stringResource(R.string.profile_no_value),
                    caption = worst?.let { formatDate(it.playedAt, dateFormatter) }.orEmpty(),
                    tag = "profile_papayoo_worst"
                )
            },
            end = {
                StatTile(
                    label = stringResource(R.string.profile_full_label, records.fullRoundPoints),
                    value = records.fullRounds.toString(),
                    caption = stringResource(R.string.profile_full_caption),
                    tag = "profile_papayoo_full"
                )
            }
        )
    }
}

@Composable
private fun BeloteSection(records: BeloteRecords) {
    GameCard(gameId = GameIds.BELOTE, wins = records.wins, tag = "profile_belote") {
        TileRow(
            start = {
                StatTile(
                    label = stringResource(R.string.profile_takes_label),
                    value = String.format(Locale.FRANCE, "%.1f", records.takesPerParty),
                    caption = stringResource(R.string.profile_takes_caption),
                    tag = "profile_belote_takes"
                )
            },
            end = {
                StatTile(
                    label = stringResource(R.string.profile_contracts_label),
                    value = records.contractRate?.let { stringResource(R.string.profile_percent, it.percent()) }
                        ?: stringResource(R.string.profile_no_value),
                    caption = if (records.takes > 0) {
                        pluralStringResource(R.plurals.profile_contracts_caption, records.takes, records.takes, records.made)
                    } else {
                        stringResource(R.string.profile_contracts_unknown)
                    },
                    tag = "profile_belote_contracts"
                )
            }
        )
        records.bestTeammate?.let { TeammateRow(R.string.profile_best_teammate_label, it, "profile_belote_best_teammate") }
        records.worstTeammate?.let { TeammateRow(R.string.profile_worst_teammate_label, it, "profile_belote_worst_teammate") }
    }
}

/** A Jeu's card: its logo and name, then the win rate in large with its bar, then [content]. */
@Composable
private fun GameCard(gameId: String, wins: WinRecord, tag: String, content: @Composable () -> Unit) {
    val presentation = gamePresentation(gameId)
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp)
            .testTag(tag)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Image(
                painter = painterResource(presentation.logoRes),
                contentDescription = null,
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
            )
            Text(
                text = stringResource(presentation.nameRes),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Column(
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.semantics(mergeDescendants = true) {}
        ) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = stringResource(R.string.profile_percent, wins.rate.percent()),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.testTag("${tag}_win_rate")
                )
                Spacer(modifier = Modifier.size(12.dp))
                Column(modifier = Modifier.padding(bottom = 6.dp)) {
                    Text(
                        text = stringResource(R.string.profile_win_rate_label),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = pluralStringResource(R.plurals.profile_wins, wins.wins, wins.wins) + " " +
                            pluralStringResource(R.plurals.profile_out_of_parties, wins.parties, wins.parties),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.testTag("${tag}_wins")
                    )
                }
            }
            LinearProgressIndicator(
                progress = { wins.rate.toFloat() },
                strokeCap = StrokeCap.Round,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                drawStopIndicator = {},
                gapSize = 0.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
            )
        }
        content()
    }
}

/** Two tiles side by side, of the same height. */
@Composable
private fun TileRow(start: @Composable () -> Unit, end: @Composable () -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
    ) {
        Box(modifier = Modifier.weight(1f).fillMaxHeight()) { start() }
        Box(modifier = Modifier.weight(1f).fillMaxHeight()) { end() }
    }
}

/** One figure: its label, the value in large, a short caption under it; read as one by TalkBack. */
@Composable
private fun StatTile(label: String, value: String, caption: String, tag: String) {
    Column(
        verticalArrangement = Arrangement.spacedBy(2.dp),
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(12.dp)
            .semantics(mergeDescendants = true) {}
            .testTag(tag)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.testTag("${tag}_value")
        )
        if (caption.isNotEmpty()) {
            Text(
                text = caption,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.testTag("${tag}_caption")
            )
        }
    }
}

/** A teammate: the label and their name on the left, the win rate together and its count on the right. */
@Composable
private fun TeammateRow(labelRes: Int, teammate: TeammateRecord, tag: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .semantics(mergeDescendants = true) {}
            .testTag(tag)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(labelRes),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = teammate.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.testTag("${tag}_name")
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = stringResource(R.string.profile_percent, teammate.wins.rate.percent()),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.testTag("${tag}_rate")
            )
            Text(
                text = pluralStringResource(R.plurals.profile_parties_together, teammate.wins.parties, teammate.wins.parties),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.End,
                modifier = Modifier.testTag("${tag}_together")
            )
        }
    }
}

@Composable
private fun SummaryChip(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    )
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

package com.rvetiveau.rvscorecount.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import com.rvetiveau.rvscorecount.R
import com.rvetiveau.rvscorecount.domain.model.CardSuit
import com.rvetiveau.rvscorecount.domain.model.Participant
import com.rvetiveau.rvscorecount.domain.model.Round
import com.rvetiveau.rvscorecount.domain.model.RoundScore
import com.rvetiveau.rvscorecount.ui.theme.color
import com.rvetiveau.rvscorecount.ui.theme.nameRes
import com.rvetiveau.rvscorecount.ui.theme.pictogramRes
import com.rvetiveau.rvscorecount.ui.theme.tint
import java.util.UUID

data class RoundTablePlayer(
    val participantId: UUID,
    val name: String
)

/**
 * One validated Manche. [points] only holds the Participants who played it; [sevenHolderId] is
 * who took the 7 of [suit], null for Manches recorded before it was asked.
 */
data class RoundTableRow(
    val roundNumber: Int,
    val suit: CardSuit,
    val points: Map<UUID, Int>,
    val sevenHolderId: UUID? = null
) {
    val total: Int get() = points.values.sum()

    /** Fewest points of the Manche, the best result; null when everyone scored the same. */
    val bestPoints: Int? get() = points.values.minOrNull()?.takeIf { it != points.values.max() }

    val worstPoints: Int? get() = points.values.maxOrNull()?.takeIf { it != points.values.min() }
}

/** Score sheet of a Partie: one column per Participant in table order, Manches in chronological order. */
data class RoundTable(
    val players: List<RoundTablePlayer> = emptyList(),
    val rows: List<RoundTableRow> = emptyList()
) {
    fun totalOf(participantId: UUID): Int = rows.sumOf { it.points[participantId] ?: 0 }
}

/**
 * Every score and 7 holder must belong to one of [participants]; callers check this first
 * (computeStandings, sevenChampion), otherwise it throws.
 */
fun roundTable(participants: List<Participant>, scoresByRound: Map<Round, List<RoundScore>>): RoundTable {
    val participantIds = participants.mapTo(mutableSetOf()) { it.id }
    return RoundTable(
        players = participants.sortedBy { it.listPosition }.map { RoundTablePlayer(it.id, it.name) },
        rows = scoresByRound.entries
            .sortedBy { (round, _) -> round.roundNumber }
            .map { (round, scores) ->
                require(scores.all { it.participantId in participantIds }) {
                    "Every round score must belong to one of the party's participants"
                }
                require(round.sevenHolderId == null || round.sevenHolderId in participantIds) {
                    "Every 7 holder must be one of the party's participants"
                }
                RoundTableRow(
                    roundNumber = round.roundNumber,
                    suit = round.suit,
                    points = scores.associate { it.participantId to it.points },
                    sevenHolderId = round.sevenHolderId
                )
            }
    )
}

private val RoundColumnWidth = 48.dp
private val MinPlayerColumnWidth = 64.dp
private val CellHeight = 40.dp

/**
 * Score sheet: header of names, one row per Manche led by its number and suit, then the totals.
 * The 7 holder's cell takes the suit's tint, the Manche's best score is bold in the primary colour and
 * its worst muted.
 * Scrolls sideways when the columns do not fit; each row is read as one by TalkBack. With [onRoundClick], touching a Manche's row reports its number.
 */
@Composable
fun RoundTable(table: RoundTable, modifier: Modifier = Modifier, onRoundClick: ((Int) -> Unit)? = null) {
    Column(modifier = modifier.fillMaxWidth()) {
        RoundTableGrid(table, onRoundClick)
        Text(
            text = stringResource(R.string.round_table_legend),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
private fun RoundTableGrid(table: RoundTable, onRoundClick: ((Int) -> Unit)?) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val playerCount = table.players.size.coerceAtLeast(1)
        val columnWidth = max(MinPlayerColumnWidth, (maxWidth - RoundColumnWidth) / playerCount)
        Column(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            HeaderRow(table.players, columnWidth)
            table.rows.forEach { row ->
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant,
                    modifier = Modifier.width(RoundColumnWidth + columnWidth * playerCount)
                )
                RoundRow(row, table.players, columnWidth, onRoundClick)
            }
            HorizontalDivider(
                thickness = 2.dp,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.width(RoundColumnWidth + columnWidth * playerCount)
            )
            TotalRow(table, columnWidth)
        }
    }
}

@Composable
private fun HeaderRow(players: List<RoundTablePlayer>, columnWidth: Dp) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.clearAndSetSemantics {}
    ) {
        Cell(text = "", width = RoundColumnWidth)
        players.forEach { player ->
            Cell(
                text = player.name,
                width = columnWidth,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun RoundRow(
    row: RoundTableRow,
    players: List<RoundTablePlayer>,
    columnWidth: Dp,
    onRoundClick: ((Int) -> Unit)?
) {
    val separator = stringResource(R.string.round_history_score_separator)
    val scoresText = players
        .mapNotNull { player -> row.points[player.participantId]?.let { player.name to it } }
        .map { (name, points) -> stringResource(R.string.round_history_score, name, points) }
        .joinToString(separator)
    val baseDescription = stringResource(
        R.string.round_history_content_description,
        row.roundNumber,
        stringResource(row.suit.nameRes),
        scoresText,
        row.total
    )
    val holderName = players.firstOrNull { it.participantId == row.sevenHolderId }?.name
    val description = if (holderName != null) {
        "$baseDescription, ${stringResource(R.string.round_history_seven_holder, holderName)}"
    } else {
        baseDescription
    }
    val correctLabel = stringResource(R.string.round_table_correct_action)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .testTag("round_table_row_${row.roundNumber}")
            .let { base ->
                if (onRoundClick != null) {
                    base.clickable(onClickLabel = correctLabel) { onRoundClick(row.roundNumber) }
                } else {
                    base
                }
            }
            .clearAndSetSemantics {
                contentDescription = description
                if (onRoundClick != null) {
                    onClick(label = correctLabel) { onRoundClick(row.roundNumber); true }
                }
            }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.width(RoundColumnWidth)
        ) {
            Text(
                text = row.roundNumber.toString(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = stringResource(row.suit.pictogramRes),
                style = MaterialTheme.typography.bodyLarge,
                color = row.suit.color,
                modifier = Modifier.padding(start = 2.dp)
            )
        }
        players.forEach { player ->
            val points = row.points[player.participantId]
            val isHolder = player.participantId == row.sevenHolderId
            Cell(
                text = points?.toString() ?: stringResource(R.string.round_table_absent),
                width = columnWidth,
                color = when (points) {
                    null -> MaterialTheme.colorScheme.outline
                    row.bestPoints -> MaterialTheme.colorScheme.primary
                    row.worstPoints -> MaterialTheme.colorScheme.outline
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                },
                fontWeight = if (points != null && points == row.bestPoints) FontWeight.Bold else null,
                background = if (isHolder) row.suit.tint else null
            )
        }
    }
}

@Composable
private fun TotalRow(table: RoundTable, columnWidth: Dp) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Cell(
            text = stringResource(R.string.round_table_total),
            width = RoundColumnWidth,
            style = MaterialTheme.typography.bodySmall
        )
        table.players.forEach { player ->
            Cell(
                text = table.totalOf(player.participantId).toString(),
                width = columnWidth,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.testTag("round_table_total_${player.participantId}")
            )
        }
    }
}

@Composable
private fun Cell(
    text: String,
    width: Dp,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    fontWeight: FontWeight? = null,
    background: Color? = null,
    style: TextStyle = MaterialTheme.typography.bodyLarge
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .width(width)
            .height(CellHeight)
            .let { base -> if (background != null) base.background(background) else base }
            .padding(horizontal = 4.dp)
    ) {
        Text(
            text = text,
            style = style,
            color = color,
            fontWeight = fontWeight,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = modifier
        )
    }
}

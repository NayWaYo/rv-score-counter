package com.rvetiveau.rvscorecount.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.rvetiveau.rvscorecount.R
import com.rvetiveau.rvscorecount.ui.theme.ChartSeries
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * One Participant's Score Cumulé after each Manche: [points] has one entry per position 0..N
 * (0 = before the first Manche), null until the position just before their first Manche.
 */
data class ScoreSeries(
    val player: RoundTablePlayer,
    val points: List<Int?>
) {
    val total: Int get() = points.lastOrNull() ?: 0
}

/** Series in table order; a Participant who never played a Manche has none. */
fun RoundTable.cumulativeSeries(): List<ScoreSeries> = players.mapNotNull { player ->
    val firstRow = rows.indexOfFirst { player.participantId in it.points }
    if (firstRow < 0) return@mapNotNull null
    var sum = 0
    val points = List(rows.size + 1) { position ->
        when {
            position < firstRow -> null
            position == firstRow -> 0
            else -> {
                sum += rows[position - 1].points[player.participantId] ?: 0
                sum
            }
        }
    }
    ScoreSeries(player, points)
}

/** Clean axis step (1, 2, 2.5 or 5 × 10^n) giving at most [maxTicks] intervals up to [max]. */
internal fun niceStep(max: Int, maxTicks: Int = 4): Int {
    if (max <= 0) return 1
    val raw = max.toDouble() / maxTicks
    val magnitude = 10.0.pow(floor(log10(raw)))
    val step = listOf(1.0, 2.0, 2.5, 5.0, 10.0).first { it * magnitude >= raw } * magnitude
    return step.roundToInt().coerceAtLeast(1)
}

private val ChartHeight = 200.dp
private const val DIRECT_LABEL_MAX_SERIES = 4

/**
 * « Évolution des scores » : one 2px line per Participant, its colour fixed by table position,
 * with an end dot; totals are written at the line ends when there are few lines and they do not
 * collide, otherwise the legend carries them. Touching or dragging across the chart shows a
 * crosshair on the nearest Manche and every Score Cumulé at that point. TalkBack reads a summary;
 * the score sheet is the full table view.
 */
@Composable
fun ScoreChart(table: RoundTable, modifier: Modifier = Modifier) {
    val series = remember(table) { table.cumulativeSeries() }
    if (series.isEmpty() || table.rows.isEmpty()) return
    val roundCount = table.rows.size
    val summary = stringResource(
        R.string.score_chart_content_description,
        roundCount,
        series.map { stringResource(R.string.round_history_score, it.player.name, it.total) }.joinToString(", ")
    )
    val roundLabels = (1..roundCount).map { stringResource(R.string.score_chart_round, it) }
    val surface = MaterialTheme.colorScheme.surface
    val ink = MaterialTheme.colorScheme.onSurface
    val mutedInk = ink.copy(alpha = 0.7f)
    val grid = ink.copy(alpha = 0.12f)
    val axisStyle = MaterialTheme.typography.labelSmall.copy(color = mutedInk)
    val valueStyle = MaterialTheme.typography.labelMedium.copy(color = ink)
    val textMeasurer = rememberTextMeasurer()
    var selected by remember(table) { mutableStateOf<Int?>(null) }
    val selectedDescription = selected?.let { round ->
        val scores = series.mapNotNull { s -> s.points[round]?.let { s.player.name to it } }
            .sortedBy { it.second }
            .map { (name, value) -> stringResource(R.string.round_history_score, name, value) }
            .joinToString(", ")
        stringResource(R.string.score_chart_selection, roundLabels[round - 1], scores)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(surface)
            .padding(12.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(ChartHeight)
                .testTag("score_chart_plot")
                .clearAndSetSemantics {
                    contentDescription = summary
                    selectedDescription?.let { stateDescription = it }
                }
        ) {
            // Written while drawing and read by the gestures: plain holder, not state, so drawing never recomposes.
            val geometry = remember { GeometryHolder() }
            Canvas(
                modifier = Modifier
                    .matchParentSize()
                    .pointerInput(table) {
                        detectTapGestures { offset ->
                            val round = geometry.current?.roundAt(offset.x)
                            selected = if (round == selected) null else round
                        }
                    }
                    .pointerInput(table) {
                        detectHorizontalDragGestures(
                            onDragStart = { offset -> selected = geometry.current?.roundAt(offset.x) },
                            onHorizontalDrag = { change, _ -> selected = geometry.current?.roundAt(change.position.x) }
                        )
                    }
            ) {
                val maxValue = series.maxOf { s -> s.points.maxOf { it ?: 0 } }
                val step = niceStep(maxValue)
                val yMax = (ceil(maxValue.toDouble() / step).toInt() * step).coerceAtLeast(step)
                val yLabels = (0..yMax step step).map { it to textMeasurer.measure(it.toString(), axisStyle) }
                val left = yLabels.maxOf { it.second.size.width } + 6.dp.toPx()
                val top = 8.dp.toPx()
                val bottom = size.height - textMeasurer.measure("0", axisStyle).size.height - 6.dp.toPx()
                // Totals ride the line ends only when no two would overlap; the legend carries them otherwise.
                val endLabels = if (series.size <= DIRECT_LABEL_MAX_SERIES) {
                    series.map { textMeasurer.measure(it.total.toString(), valueStyle) }
                        .takeIf { labels ->
                            series.map { bottom - (bottom - top) * it.total / yMax }.sorted()
                                .zipWithNext()
                                .none { (a, b) -> b - a < labels.maxOf { it.size.height } }
                        }
                        .orEmpty()
                } else {
                    emptyList()
                }
                val right = endLabels.maxOfOrNull { it.size.width + 14.dp.toPx() } ?: 8.dp.toPx()
                val g = ChartGeometry(
                    left = left,
                    top = top,
                    right = size.width - right,
                    bottom = bottom,
                    roundCount = roundCount,
                    yMax = yMax
                )
                geometry.current = g

                // Recessive hairline grid with its tick values.
                yLabels.forEach { (value, layout) ->
                    val y = g.y(value)
                    drawLine(grid, Offset(g.left, y), Offset(g.right, y), strokeWidth = 1.dp.toPx())
                    drawText(layout, topLeft = Offset(g.left - 6.dp.toPx() - layout.size.width, y - layout.size.height / 2f))
                }
                val labelEvery = ceil(roundCount / 8.0).toInt()
                (1..roundCount).filter { it % labelEvery == 0 || it == roundCount }.forEach { round ->
                    val layout = textMeasurer.measure(round.toString(), axisStyle)
                    drawText(layout, topLeft = Offset(g.x(round) - layout.size.width / 2f, g.bottom + 4.dp.toPx()))
                }

                series.forEachIndexed { index, s ->
                    val color = ChartSeries.getOrElse(index) { ink }
                    val path = Path()
                    s.points.forEachIndexed { position, value ->
                        if (value == null) return@forEachIndexed
                        val point = Offset(g.x(position), g.y(value))
                        if (path.isEmpty) path.moveTo(point.x, point.y) else path.lineTo(point.x, point.y)
                    }
                    drawPath(path, color, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
                }
                series.forEachIndexed { index, s ->
                    drawDot(ChartSeries.getOrElse(index) { ink }, Offset(g.x(roundCount), g.y(s.total)), surface)
                }
                drawEndLabels(series, endLabels, g)

                selected?.let { round -> drawSelection(round, series, g, textMeasurer, roundLabels[round - 1], surface, ink, valueStyle) }
            }
        }
        ChartLegend(series)
    }
}

/** Plot area in pixels and the value-to-position mapping. */
private class ChartGeometry(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
    val roundCount: Int,
    val yMax: Int
) {
    fun x(position: Int): Float = left + (right - left) * position / roundCount
    fun y(value: Int): Float = bottom - (bottom - top) * value / yMax

    /** Nearest Manche (1..N) to a touch anywhere on the chart, the margins included. */
    fun roundAt(touchX: Float): Int =
        ((touchX - left) / (right - left) * roundCount).roundToInt().coerceIn(1, roundCount)
}

/** End dot: ≥ 8px, series colour, 2px ring in the surface colour. */
private fun DrawScope.drawDot(color: Color, center: Offset, surface: Color) {
    drawCircle(surface, radius = 6.dp.toPx(), center = center)
    drawCircle(color, radius = 4.dp.toPx(), center = center)
}

/** Totals beside the end dots; [labels] is empty when they would overlap. */
private fun DrawScope.drawEndLabels(series: List<ScoreSeries>, labels: List<TextLayoutResult>, g: ChartGeometry) {
    if (labels.isEmpty()) return
    series.forEachIndexed { index, s ->
        val layout = labels[index]
        drawText(layout, topLeft = Offset(g.right + 8.dp.toPx(), g.y(s.total) - layout.size.height / 2f))
    }
}

/** Crosshair on [round], a dot on every line there and a tooltip with each Score Cumulé. */
private fun DrawScope.drawSelection(
    round: Int,
    series: List<ScoreSeries>,
    g: ChartGeometry,
    textMeasurer: TextMeasurer,
    roundLabel: String,
    surface: Color,
    ink: Color,
    valueStyle: TextStyle
) {
    val x = g.x(round)
    drawLine(ink.copy(alpha = 0.5f), Offset(x, g.top), Offset(x, g.bottom), strokeWidth = 1.dp.toPx())
    val visible = series.mapIndexedNotNull { index, s ->
        s.points[round]?.let { value -> Triple(ChartSeries.getOrElse(index) { ink }, s.player.name, value) }
    }
    visible.forEach { (color, _, value) -> drawDot(color, Offset(x, g.y(value)), surface) }

    val title = textMeasurer.measure(roundLabel, valueStyle.copy(color = ink.copy(alpha = 0.7f)))
    val lines = visible
        .sortedBy { it.third }
        .map { (color, name, value) -> color to textMeasurer.measure("$name  $value", valueStyle) }
    val padding = 8.dp.toPx()
    val keySize = 8.dp.toPx()
    val lineHeight = lines.maxOfOrNull { it.second.size.height } ?: 0
    val width = maxOf(title.size.width.toFloat(), (lines.maxOfOrNull { it.second.size.width } ?: 0) + keySize + 6.dp.toPx()) + padding * 2
    val height = title.size.height + lineHeight * lines.size + padding * 2
    val boxLeft = if (x + 12.dp.toPx() + width <= size.width) x + 12.dp.toPx() else x - 12.dp.toPx() - width
    val boxTop = g.top
    drawRoundRect(
        color = surface,
        topLeft = Offset(boxLeft, boxTop),
        size = Size(width, height),
        cornerRadius = CornerRadius(6.dp.toPx())
    )
    drawRoundRect(
        color = ink.copy(alpha = 0.3f),
        topLeft = Offset(boxLeft, boxTop),
        size = Size(width, height),
        cornerRadius = CornerRadius(6.dp.toPx()),
        style = Stroke(1.dp.toPx())
    )
    drawText(title, topLeft = Offset(boxLeft + padding, boxTop + padding))
    lines.forEachIndexed { index, (color, layout) ->
        val top = boxTop + padding + title.size.height + lineHeight * index
        drawCircle(color, radius = keySize / 2, center = Offset(boxLeft + padding + keySize / 2, top + lineHeight / 2f))
        drawText(layout, topLeft = Offset(boxLeft + padding + keySize + 6.dp.toPx(), top))
    }
}

/** Legend: colour dot, then the name and Score Cumulé in text ink. */
@Composable
private fun ChartLegend(series: List<ScoreSeries>) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .semantics { hideFromAccessibility() }
            .testTag("score_chart_legend")
    ) {
        series.forEachIndexed { index, s ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(ChartSeries.getOrElse(index) { MaterialTheme.colorScheme.onSurface })
                )
                Text(
                    text = stringResource(R.string.round_history_score, s.player.name, s.total),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
        }
    }
}

private class GeometryHolder {
    var current: ChartGeometry? = null
}

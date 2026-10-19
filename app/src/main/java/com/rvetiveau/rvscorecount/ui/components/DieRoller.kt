package com.rvetiveau.rvscorecount.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rvetiveau.rvscorecount.R
import com.rvetiveau.rvscorecount.domain.model.CardSuit
import com.rvetiveau.rvscorecount.ui.theme.color
import com.rvetiveau.rvscorecount.ui.theme.nameRes
import com.rvetiveau.rvscorecount.ui.theme.pictogramRes
import com.rvetiveau.rvscorecount.ui.theme.tint

/** Duration of the roll animation played after each tap on the die, « Lancer » or « Relancer ». */
internal const val DIE_ROLL_ANIMATION_MILLIS = 800

/** Number of suit faces shown while the die rolls, the last one being the drawn suit. */
private const val DIE_ROLL_FACES = 9

private class DieDimensions(val face: Dp, val pictogram: Dp, val blankIcon: Dp, val corner: Dp)

private val RegularDie = DieDimensions(face = 168.dp, pictogram = 104.dp, blankIcon = 72.dp, corner = 28.dp)
private val CompactDie = DieDimensions(face = 64.dp, pictogram = 40.dp, blankIcon = 32.dp, corner = 12.dp)

/**
 * Stateless die roll control (AC 2-4, 6, 10): no [kotlin.random.Random] and no ViewModel access
 * here. A large die face sits above the action: `suit == null` shows a blank face and « Lancer »;
 * a drawn suit shows its pictogram on the suit's tint, its French name and « Relancer », gated
 * only by [rerollEnabled]. Tapping the face rolls too, whenever the button would.
 *
 * [compact] lays a small face, the name and the action out on one row, leaving room for the score
 * fields while the keyboard is open.
 *
 * Each tap plays a short roll animation (faces cycling while the pictogram spins) before the drawn
 * suit is revealed; the suit itself is already drawn by the caller. Only taps trigger it, so a
 * restored suit is shown at once. Compose honors the system animation scale, so the animation is
 * skipped when animations are turned off.
 */
@Composable
fun DieRoller(
    suit: CardSuit?,
    onRoll: () -> Unit,
    modifier: Modifier = Modifier,
    rerollEnabled: Boolean = true,
    compact: Boolean = false
) {
    var rollCount by remember { mutableIntStateOf(0) }
    var startedRollCount by remember { mutableIntStateOf(0) }
    val rollProgress = remember { Animatable(1f) }
    LaunchedEffect(rollCount) {
        if (rollCount == 0) return@LaunchedEffect
        rollProgress.snapTo(0f)
        startedRollCount = rollCount
        rollProgress.animateTo(1f, tween(DIE_ROLL_ANIMATION_MILLIS, easing = FastOutSlowInEasing))
    }
    // A tap counts as rolling at once, so the result never flashes for a frame before the animation.
    val pendingRoll = startedRollCount != rollCount
    val rolling = pendingRoll || rollProgress.value < 1f
    val canRoll = suit == null || (rerollEnabled && !rolling)
    val roll = {
        rollCount++
        onRoll()
    }
    // The buttons already carry the roll for TalkBack: the face is a pointer shortcut only.
    val faceClick = Modifier.clickable(enabled = canRoll, onClick = roll)
    val dimensions = if (compact) CompactDie else RegularDie
    val buttonModifier = Modifier
        .heightIn(min = 56.dp)
        .let { base -> if (compact) base else base.widthIn(min = 200.dp) }

    val result: @Composable (Modifier) -> Unit
    val action: @Composable () -> Unit
    if (suit == null) {
        result = { resultModifier ->
            Box(modifier = resultModifier.testTag("die_face_blank").clearAndSetSemantics {}) {
                DieFace(dimensions = dimensions, background = MaterialTheme.colorScheme.surfaceVariant, modifier = faceClick) {
                    Icon(
                        painter = painterResource(R.drawable.ic_casino),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(dimensions.blankIcon)
                    )
                }
            }
        }
        action = {
            Button(onClick = roll, modifier = buttonModifier.testTag("die_roll_button")) {
                Icon(
                    painter = painterResource(R.drawable.ic_casino),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = stringResource(R.string.die_roll))
            }
        }
    } else {
        val progress = if (pendingRoll) 0f else rollProgress.value
        // Faces run backwards from the drawn suit so the last face shown is the result.
        val faceIndex = (DIE_ROLL_FACES - 1 - (progress * DIE_ROLL_FACES).toInt()).coerceAtLeast(0)
        val face = if (rolling) {
            CardSuit.entries[(suit.ordinal + faceIndex) % CardSuit.entries.size]
        } else {
            suit
        }
        val suitName = stringResource(suit.nameRes)
        val resultDescription = if (rolling) {
            stringResource(R.string.die_rolling_content_description)
        } else {
            stringResource(R.string.die_result_content_description, suitName)
        }
        // Sized in dp so the face keeps its size; the name below scales with the font.
        val pictogramFontSize = with(LocalDensity.current) { dimensions.pictogram.toSp() }
        val dieFace: @Composable () -> Unit = {
            DieFace(dimensions = dimensions, background = face.tint, modifier = faceClick) {
                Text(
                    text = stringResource(face.pictogramRes),
                    fontSize = pictogramFontSize,
                    lineHeight = pictogramFontSize,
                    color = face.color,
                    modifier = Modifier.graphicsLayer {
                        rotationZ = (1f - progress) * 720f
                        val bounce = 1f + 0.3f * (1f - progress)
                        scaleX = bounce
                        scaleY = bounce
                    }
                )
            }
        }
        val name: @Composable (Modifier) -> Unit = { nameModifier ->
            Text(
                text = if (rolling) "" else suitName,
                style = if (compact) MaterialTheme.typography.titleLarge else MaterialTheme.typography.headlineSmall,
                color = suit.color,
                textAlign = if (compact) TextAlign.Start else TextAlign.Center,
                modifier = nameModifier.testTag("die_result_name")
            )
        }
        result = { resultModifier ->
            val groupModifier = resultModifier
                .testTag(if (rolling) "die_rolling" else "die_result")
                .clearAndSetSemantics { contentDescription = resultDescription }
            if (compact) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = groupModifier
                ) {
                    dieFace()
                    name(Modifier.weight(1f))
                }
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = groupModifier.fillMaxWidth()) {
                    dieFace()
                    Spacer(modifier = Modifier.height(8.dp))
                    name(Modifier.fillMaxWidth())
                }
            }
        }
        action = {
            OutlinedButton(
                onClick = roll,
                enabled = rerollEnabled && !rolling,
                modifier = buttonModifier.testTag("die_reroll_button")
            ) {
                Text(text = stringResource(R.string.die_reroll))
            }
        }
    }

    if (compact) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = modifier
        ) {
            result(Modifier.weight(1f))
            action()
        }
    } else {
        Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
            result(Modifier)
            Spacer(modifier = Modifier.height(if (suit == null) 16.dp else 8.dp))
            action()
        }
    }
}

/** The die itself: a rounded square holding the face's content. */
@Composable
private fun DieFace(
    dimensions: DieDimensions,
    background: Color,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val shape = RoundedCornerShape(dimensions.corner)
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(dimensions.face)
            .clip(shape)
            .background(background)
            .border(2.dp, MaterialTheme.colorScheme.outlineVariant, shape)
            .then(modifier)
    ) {
        content()
    }
}

package com.rvetiveau.rvscorecount.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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

private val DieFaceSize = 168.dp
private val DiePictogramSize = 104.dp

/**
 * Stateless die roll control (AC 2-4, 6, 10): no [kotlin.random.Random] and no ViewModel access
 * here. A large die face sits above the action: `suit == null` shows a blank face and « Lancer »;
 * a drawn suit shows its pictogram on the suit's tint, its French name and « Relancer », gated
 * only by [rerollEnabled]. Tapping the face rolls too, whenever the button would.
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
    rerollEnabled: Boolean = true
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

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        if (suit == null) {
            Box(modifier = Modifier.testTag("die_face_blank").clearAndSetSemantics {}) {
                DieFace(background = MaterialTheme.colorScheme.surfaceVariant, modifier = faceClick) {
                    Icon(
                        painter = painterResource(R.drawable.ic_casino),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(72.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = roll,
                modifier = Modifier
                    .widthIn(min = 200.dp)
                    .heightIn(min = 56.dp)
                    .testTag("die_roll_button")
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_casino),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = stringResource(R.string.die_roll))
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
            val pictogramFontSize = with(LocalDensity.current) { DiePictogramSize.toSp() }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(if (rolling) "die_rolling" else "die_result")
                    .clearAndSetSemantics { contentDescription = resultDescription }
            ) {
                DieFace(background = face.tint, modifier = faceClick) {
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
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (rolling) "" else suitName,
                    style = MaterialTheme.typography.headlineSmall,
                    color = suit.color,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("die_result_name")
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = roll,
                enabled = rerollEnabled && !rolling,
                modifier = Modifier
                    .widthIn(min = 200.dp)
                    .heightIn(min = 56.dp)
                    .testTag("die_reroll_button")
            ) {
                Text(text = stringResource(R.string.die_reroll))
            }
        }
    }
}

/** The die itself: a large rounded square holding the face's content. */
@Composable
private fun DieFace(
    background: Color,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val shape = RoundedCornerShape(28.dp)
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(DieFaceSize)
            .clip(shape)
            .background(background)
            .border(2.dp, MaterialTheme.colorScheme.outlineVariant, shape)
            .then(modifier)
    ) {
        content()
    }
}

package com.rvetiveau.rvscorecount.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import com.rvetiveau.rvscorecount.R
import com.rvetiveau.rvscorecount.domain.model.CardSuit
import com.rvetiveau.rvscorecount.ui.theme.color
import com.rvetiveau.rvscorecount.ui.theme.nameRes
import com.rvetiveau.rvscorecount.ui.theme.pictogramRes

/** Duration of the roll animation played after each tap on « Lancer » or « Relancer ». */
internal const val DIE_ROLL_ANIMATION_MILLIS = 800

/** Number of suit faces shown while the die rolls, the last one being the drawn suit. */
private const val DIE_ROLL_FACES = 9

/**
 * Stateless die roll control (AC 2-4, 6, 10): no [kotlin.random.Random] and no ViewModel access
 * here. `suit == null` renders the « Lancer » call to action; a drawn suit renders its pictogram,
 * French name and a « Relancer » action gated only by [rerollEnabled].
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
    val roll = {
        rollCount++
        onRoll()
    }

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        if (suit == null) {
            Button(
                onClick = roll,
                modifier = Modifier
                    .fillMaxWidth()
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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(if (rolling) "die_rolling" else "die_result")
                    .clearAndSetSemantics { contentDescription = resultDescription }
            ) {
                Text(
                    text = stringResource(face.pictogramRes),
                    style = MaterialTheme.typography.headlineMedium,
                    color = face.color,
                    modifier = Modifier.graphicsLayer {
                        rotationZ = (1f - progress) * 720f
                        val bounce = 1f + 0.3f * (1f - progress)
                        scaleX = bounce
                        scaleY = bounce
                    }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (rolling) "" else suitName,
                    style = MaterialTheme.typography.bodyLarge,
                    color = suit.color,
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
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .testTag("die_reroll_button")
            ) {
                Text(text = stringResource(R.string.die_reroll))
            }
        }
    }
}

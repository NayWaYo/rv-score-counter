package com.rvetiveau.rvscorecount.ui.components

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import com.rvetiveau.rvscorecount.R
import com.rvetiveau.rvscorecount.domain.model.CardSuit
import com.rvetiveau.rvscorecount.ui.theme.color

/**
 * Stateless die roll control (AC 2-4, 6, 10): no [kotlin.random.Random] and no ViewModel access
 * here. `suit == null` renders the « Lancer » call to action; a drawn suit renders its pictogram,
 * French name and a « Relancer » action gated only by [rerollEnabled]. No animation, sound or
 * vibration is added.
 */
@Composable
fun DieRoller(
    suit: CardSuit?,
    onRoll: () -> Unit,
    modifier: Modifier = Modifier,
    rerollEnabled: Boolean = true
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        if (suit == null) {
            Button(
                onClick = onRoll,
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
            val suitName = stringResource(suit.nameRes)
            val resultDescription = stringResource(R.string.die_result_content_description, suitName)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("die_result")
                    .clearAndSetSemantics { contentDescription = resultDescription }
            ) {
                Text(
                    text = stringResource(suit.pictogramRes),
                    style = MaterialTheme.typography.headlineMedium,
                    color = suit.color
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = suitName,
                    style = MaterialTheme.typography.bodyLarge,
                    color = suit.color,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("die_result_name")
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = onRoll,
                enabled = rerollEnabled,
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

private val CardSuit.pictogramRes: Int
    get() = when (this) {
        CardSuit.SPADE -> R.string.suit_spade_pictogram
        CardSuit.HEART -> R.string.suit_heart_pictogram
        CardSuit.CLUB -> R.string.suit_club_pictogram
        CardSuit.DIAMOND -> R.string.suit_diamond_pictogram
    }

private val CardSuit.nameRes: Int
    get() = when (this) {
        CardSuit.SPADE -> R.string.suit_spade
        CardSuit.HEART -> R.string.suit_heart
        CardSuit.CLUB -> R.string.suit_club
        CardSuit.DIAMOND -> R.string.suit_diamond
    }

package com.rvetiveau.rvscorecount.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.rvetiveau.rvscorecount.R
import com.rvetiveau.rvscorecount.domain.model.SevenChampion

/** Display model of the « Champion du 40 » statistic (ties included). */
data class SevenChampionUi(
    val names: List<String>,
    val count: Int
)

fun SevenChampion.toUi() = SevenChampionUi(names = participants.map { it.name }, count = count)

/** « Champion du 40 : Bob (3 fois) » — who took the 7 of the die's suit the most often. */
@Composable
fun SevenChampionLine(champion: SevenChampionUi, modifier: Modifier = Modifier) {
    Text(
        text = stringResource(R.string.seven_champion, champion.names.joinToString(", "), champion.count),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = modifier
    )
}

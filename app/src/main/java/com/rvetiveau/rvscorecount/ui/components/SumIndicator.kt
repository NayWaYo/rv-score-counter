package com.rvetiveau.rvscorecount.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.rvetiveau.rvscorecount.R

/**
 * Stateless running total (AC 5): informative only in Story 3.2 — neutral color always, no
 * validation message. Story 3.3 will add success/error states on top of the same total.
 */
@Composable
fun SumIndicator(currentSum: Long, modifier: Modifier = Modifier) {
    Text(
        text = stringResource(R.string.round_entry_sum_label, currentSum),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = modifier.testTag("round_entry_sum_indicator")
    )
}

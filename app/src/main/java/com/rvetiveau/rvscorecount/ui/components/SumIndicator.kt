package com.rvetiveau.rvscorecount.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.rvetiveau.rvscorecount.R
import com.rvetiveau.rvscorecount.ui.screens.round.SumIndicatorState
import com.rvetiveau.rvscorecount.ui.theme.RvTheme

/**
 * Stateless running total whose text and color are fully derived by the ViewModel state.
 */
@Composable
fun SumIndicator(
    currentSum: Long,
    state: SumIndicatorState,
    totalPointsPerRound: Int,
    modifier: Modifier = Modifier
) {
    val text = when (state) {
        SumIndicatorState.ERROR -> stringResource(R.string.round_entry_sum_error_message, currentSum, totalPointsPerRound)
        SumIndicatorState.NEUTRAL,
        SumIndicatorState.SUCCESS -> stringResource(R.string.round_entry_sum_label, currentSum)
    }
    val color = when (state) {
        SumIndicatorState.NEUTRAL -> MaterialTheme.colorScheme.onBackground
        SumIndicatorState.SUCCESS -> RvTheme.colors.success
        SumIndicatorState.ERROR -> MaterialTheme.colorScheme.error
    }
    Text(
        text = text,
        style = MaterialTheme.typography.bodyLarge,
        color = color,
        modifier = modifier.testTag("round_entry_sum_indicator")
    )
}

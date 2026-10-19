package com.rvetiveau.rvscorecount.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.rvetiveau.rvscorecount.R
import java.util.UUID

/** Room for three digits and the precomputed field's calculation icon. */
private val ScoreFieldWidth = 136.dp

/**
 * Stateless free-order score field (AC 2, 3, 6, 7): the composable filters obviously invalid
 * characters for a responsive IME, but [RoundEntryViewModel.onScoreChanged] remains the sole
 * authority — `KeyboardType.Number` is only a hint the active IME may ignore.
 */
@Composable
fun ScoreField(
    participantId: UUID,
    participantName: String,
    value: String,
    onValueChange: (String) -> Unit,
    imeAction: ImeAction,
    onImeAction: (ImeAction) -> Unit,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null,
    isPrecomputed: Boolean = false,
    isError: Boolean = false
) {
    val accessibilityLabel = if (isPrecomputed) {
        stringResource(R.string.score_field_precomputed_content_description, participantName)
    } else {
        stringResource(R.string.score_field_content_description, participantName)
    }
    val containerColor = if (isPrecomputed) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }

    // Name and field share one row so every Participant fits above the keyboard.
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Text(
            text = participantName,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier
                .weight(1f)
                .semantics { hideFromAccessibility() }
                .testTag("score_field_label_$participantId")
        )
        OutlinedTextField(
            value = value,
            onValueChange = { proposed ->
                val isDigitsOnly = proposed.isEmpty() || (proposed.all(Char::isDigit) && proposed.toIntOrNull() != null)
                if (isDigitsOnly) onValueChange(proposed)
            },
            singleLine = true,
            isError = isError,
            shape = RoundedCornerShape(8.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = containerColor,
                unfocusedContainerColor = containerColor,
                disabledContainerColor = containerColor,
                errorContainerColor = containerColor
            ),
            trailingIcon = if (isPrecomputed) {
                {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("score_field_calculate_icon_$participantId")
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_calculate),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            } else {
                null
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = imeAction),
            keyboardActions = KeyboardActions(
                onNext = { onImeAction(ImeAction.Next) },
                onDone = { onImeAction(ImeAction.Done) }
            ),
            textStyle = MaterialTheme.typography.bodyLarge,
            modifier = Modifier
                .width(ScoreFieldWidth)
                .heightIn(min = 56.dp)
                .let { base -> if (focusRequester != null) base.focusRequester(focusRequester) else base }
                .semantics { contentDescription = accessibilityLabel }
                .testTag("score_field_$participantId")
        )
    }
}

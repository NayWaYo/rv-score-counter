package com.rvetiveau.rvscorecount.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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

/** Room for three digits and the suggestion's calculation icon. */
private val ScoreFieldWidth = 136.dp

/**
 * Stateless free-order score field (AC 2, 3, 6, 7): the composable filters obviously invalid
 * characters for a responsive IME, but [RoundEntryViewModel.onScoreChanged] remains the sole
 * authority — `KeyboardType.Number` is only a hint the active IME may ignore.
 *
 * While the field is empty, [suggestion] shows greyed as a placeholder on an accent tint, with a
 * calculation icon that enters it; typing replaces it.
 *
 * With [onToggleSign] (Jeu libre, Story 11.3), the field also accepts a leading minus and a +/−
 * button before it switches the sign, number keyboards rarely offering one.
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
    suggestion: Int? = null,
    isError: Boolean = false,
    onScan: (() -> Unit)? = null,
    onToggleSign: (() -> Unit)? = null
) {
    val shownSuggestion = suggestion?.takeIf { value.isEmpty() }
    val accessibilityLabel = if (shownSuggestion != null) {
        stringResource(R.string.score_field_suggested_content_description, participantName, shownSuggestion)
    } else {
        stringResource(R.string.score_field_content_description, participantName)
    }
    val signLabel = stringResource(R.string.score_field_sign, participantName)
    val containerColor = if (shownSuggestion != null) {
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
        if (onToggleSign != null) {
            TextButton(
                onClick = onToggleSign,
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier
                    .size(48.dp)
                    .semantics { contentDescription = signLabel }
                    .testTag("score_field_sign_$participantId")
            ) {
                Text(
                    text = stringResource(R.string.score_field_sign_symbol),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.semantics { hideFromAccessibility() }
                )
            }
        }
        OutlinedTextField(
            value = value,
            onValueChange = { proposed ->
                val digits = if (onToggleSign != null) proposed.removePrefix("-") else proposed
                val isNumber = proposed.isEmpty() || (digits.all(Char::isDigit) && (digits.isEmpty() || digits.toIntOrNull() != null))
                if (isNumber) onValueChange(proposed)
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
            placeholder = shownSuggestion?.let { score ->
                {
                    Text(
                        text = score.toString(),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.testTag("score_field_suggestion_$participantId")
                    )
                }
            },
            trailingIcon = shownSuggestion?.let { score ->
                {
                    IconButton(
                        onClick = { onValueChange(score.toString()) },
                        modifier = Modifier.testTag("score_field_use_suggestion_$participantId")
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_calculate),
                            contentDescription = stringResource(R.string.score_field_use_suggestion, score, participantName),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
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
        if (onScan != null) {
            IconButton(
                onClick = onScan,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("score_field_scan_$participantId")
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_photo_camera),
                    contentDescription = stringResource(R.string.score_field_scan, participantName),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

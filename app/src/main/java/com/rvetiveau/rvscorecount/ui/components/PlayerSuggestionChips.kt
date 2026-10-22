package com.rvetiveau.rvscorecount.ui.components

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.rvetiveau.rvscorecount.R

/** One-tap known names at Création de Partie (players, or games of the Jeu libre), scrolling sideways; nothing when [names] is empty. */
@Composable
fun PlayerSuggestionChips(
    names: List<String>,
    onNameSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    @StringRes descriptionRes: Int = R.string.player_suggestion_content_description,
    tagPrefix: String = "player_suggestion"
) {
    if (names.isEmpty()) return
    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .testTag("${tagPrefix}s"),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(names, key = { it }) { name ->
            val description = stringResource(descriptionRes, name)
            SuggestionChip(
                onClick = { onNameSelected(name) },
                label = { Text(text = name, style = MaterialTheme.typography.bodyLarge) },
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag("${tagPrefix}_$name")
                    .semantics { contentDescription = description }
            )
        }
    }
}

package com.rvetiveau.rvscorecount.ui.screens.setup

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.rvetiveau.rvscorecount.R
import com.rvetiveau.rvscorecount.ui.components.HistoryButton
import com.rvetiveau.rvscorecount.ui.components.PlayerSuggestionChips
import com.rvetiveau.rvscorecount.ui.components.ScreenHeader
import java.util.UUID

/**
 * Stateless Création de Partie screen (AC 1-5, 7, 8): name entry, ordered Participant list with
 * drag-to-reorder and removal, and the (not-yet-wired) "Commencer" action. No Hilt, repository,
 * concrete GameEngine or NavController access is allowed here.
 */
@Composable
fun SetupScreen(
    state: SetupUiState,
    onNameChanged: (String) -> Unit,
    onAddParticipant: () -> Unit,
    onRemoveParticipant: (UUID) -> Unit,
    onMoveParticipant: (Int, Int) -> Unit,
    onStartParty: () -> Unit,
    modifier: Modifier = Modifier,
    onHistorySelected: () -> Unit = {},
    onSuggestionSelected: (String) -> Unit = {}
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp)
    ) {
        ScreenHeader(
            title = stringResource(R.string.setup_title),
            titleTag = "setup_title",
            modifier = Modifier.padding(vertical = 8.dp)
        ) {
            HistoryButton(onClick = onHistorySelected, modifier = Modifier.testTag("setup_history_action"))
        }

        NameInputRow(
            nameInput = state.nameInput,
            enabled = state.canAdd,
            isError = state.messageRes == R.string.setup_error_empty_name,
            onNameChanged = onNameChanged,
            onAddParticipant = onAddParticipant
        )

        PlayerSuggestionChips(
            names = state.nameSuggestions,
            onNameSelected = onSuggestionSelected,
            modifier = Modifier.padding(top = 8.dp)
        )

        val messageRes = state.messageRes ?: R.string.setup_max_players_reached.takeIf { state.isMaxReached }
        if (messageRes != null) {
            val messageText = if (messageRes == R.string.setup_max_players_reached) {
                stringResource(messageRes, state.maxPlayers)
            } else {
                stringResource(messageRes)
            }
            Text(
                text = messageText,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier
                    .padding(top = 4.dp)
                    .testTag("setup_input_message")
            )
        }

        if (state.creationErrorRes != null) {
            Text(
                text = stringResource(state.creationErrorRes),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier
                    .padding(top = 4.dp)
                    .testTag("setup_creation_error_message")
            )
        }

        Text(
            text = stringResource(
                R.string.setup_participant_count,
                state.participants.size,
                state.minPlayers,
                state.maxPlayers
            ),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .padding(vertical = 8.dp)
                .testTag("setup_participant_count")
        )

        ParticipantList(
            participants = state.participants,
            onMove = onMoveParticipant,
            onRemove = onRemoveParticipant,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        )

        Button(
            onClick = onStartParty,
            enabled = state.canStart,
            modifier = Modifier
                .padding(vertical = 16.dp)
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .testTag("setup_start_button")
        ) {
            Text(text = stringResource(R.string.setup_start_button))
        }
    }
}

@Composable
private fun NameInputRow(
    nameInput: String,
    enabled: Boolean,
    isError: Boolean,
    onNameChanged: (String) -> Unit,
    onAddParticipant: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = nameInput,
            onValueChange = onNameChanged,
            enabled = enabled,
            isError = isError,
            singleLine = true,
            label = { Text(stringResource(R.string.setup_name_hint)) },
            modifier = Modifier
                .weight(1f)
                .testTag("setup_name_field")
        )
        Spacer(modifier = Modifier.width(8.dp))
        Button(
            onClick = onAddParticipant,
            enabled = enabled,
            modifier = Modifier
                .heightIn(min = 56.dp)
                .testTag("setup_add_button")
        ) {
            Text(text = stringResource(R.string.setup_add_button))
        }
    }
}

@Composable
private fun ParticipantList(
    participants: List<ParticipantDraft>,
    onMove: (Int, Int) -> Unit,
    onRemove: (UUID) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    var draggedIndex by remember { mutableIntStateOf(-1) }
    var dragOffsetPx by remember { mutableFloatStateOf(0f) }

    LazyColumn(
        state = listState,
        modifier = modifier.testTag("setup_participant_list"),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        itemsIndexed(participants, key = { _, participant -> participant.id.toString() }) { index, participant ->
            val isDragged = index == draggedIndex
            val currentIndex by rememberUpdatedState(index)
            val handleDescription = stringResource(
                R.string.setup_reorder_handle_content_description,
                participant.name,
                index + 1,
                participants.size
            )
            val dragModifier = Modifier.pointerInput(participant.id, participants.size) {
                detectDragGesturesAfterLongPress(
                    onDragStart = {
                        draggedIndex = currentIndex
                        dragOffsetPx = 0f
                    },
                    onDragEnd = {
                        draggedIndex = -1
                        dragOffsetPx = 0f
                    },
                    onDragCancel = {
                        draggedIndex = -1
                        dragOffsetPx = 0f
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        dragOffsetPx += dragAmount.y
                        val from = draggedIndex
                        val visibleItems = listState.layoutInfo.visibleItemsInfo
                        val draggedItem = visibleItems.firstOrNull { it.index == from }
                        if (draggedItem != null) {
                            val draggedCenter = draggedItem.offset + draggedItem.size / 2f + dragOffsetPx
                            val targetItem = visibleItems.firstOrNull { item ->
                                item.index != from &&
                                    draggedCenter >= item.offset &&
                                    draggedCenter <= item.offset + item.size
                            }
                            if (targetItem != null) {
                                onMove(from, targetItem.index)
                                dragOffsetPx += draggedItem.offset - targetItem.offset
                                draggedIndex = targetItem.index
                            }
                        }
                    }
                )
            }

            ParticipantRow(
                participant = participant,
                isDealer = index == 0,
                handleDescription = handleDescription,
                dragModifier = dragModifier,
                onRemove = { onRemove(participant.id) },
                modifier = Modifier
                    .graphicsLayer { translationY = if (isDragged) dragOffsetPx else 0f }
                    .zIndex(if (isDragged) 1f else 0f)
            )
        }
    }
}

@Composable
private fun ParticipantRow(
    participant: ParticipantDraft,
    isDealer: Boolean,
    handleDescription: String,
    dragModifier: Modifier,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    val deleteDescription = stringResource(R.string.setup_remove_participant_content_description, participant.name)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = dragModifier
                .size(48.dp)
                .testTag("setup_participant_handle_${participant.id}")
                .semantics { contentDescription = handleDescription },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_drag_handle),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = participant.name,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .weight(1f)
                .testTag("setup_participant_name_${participant.id}")
        )
        if (isDealer) {
            Spacer(modifier = Modifier.width(8.dp))
            DealerBadge()
        }
        Spacer(modifier = Modifier.width(8.dp))
        IconButton(
            onClick = onRemove,
            modifier = Modifier
                .size(48.dp)
                .testTag("setup_participant_delete_${participant.id}")
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_delete),
                contentDescription = deleteDescription,
                tint = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun DealerBadge(modifier: Modifier = Modifier) {
    val description = stringResource(R.string.setup_dealer_badge_content_description)
    Box(
        modifier = modifier
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .testTag("setup_dealer_badge")
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = stringResource(R.string.setup_dealer_badge),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

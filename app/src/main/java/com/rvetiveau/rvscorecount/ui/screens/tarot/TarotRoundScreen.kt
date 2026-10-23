package com.rvetiveau.rvscorecount.ui.screens.tarot

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.keepScreenOn
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rvetiveau.rvscorecount.R
import com.rvetiveau.rvscorecount.domain.model.Chelem
import com.rvetiveau.rvscorecount.domain.model.Poignee
import com.rvetiveau.rvscorecount.domain.model.TarotContract
import com.rvetiveau.rvscorecount.domain.model.TarotSide
import com.rvetiveau.rvscorecount.ui.components.ScreenHeader
import com.rvetiveau.rvscorecount.ui.navigation.NavigationEvent
import com.rvetiveau.rvscorecount.ui.screens.belote.Centered
import java.util.UUID

/** What the saisie of a Manche of Tarot reports to its ViewModel. */
data class TarotRoundActions(
    val onTakerSelected: (UUID) -> Unit = {},
    val onContractSelected: (TarotContract) -> Unit = {},
    val onPartnerSelected: (TarotPartnerChoice) -> Unit = {},
    val onBoutsSelected: (Int) -> Unit = {},
    val onPointsChanged: (String) -> Unit = {},
    val onPetitAuBoutSelected: (TarotSide?) -> Unit = {},
    val onAttackPoigneeSelected: (Poignee?) -> Unit = {},
    val onDefensePoigneeSelected: (Poignee?) -> Unit = {},
    val onChelemSelected: (Chelem?) -> Unit = {},
    val onValidate: () -> Unit = {}
)

/** Compose/Hilt adapter for the saisie or correction of a Manche of Tarot (AD-4). */
@Composable
fun TarotRoundRoute(
    partyId: UUID,
    correctedRoundNumber: Int?,
    onNavigate: (NavigationEvent) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TarotRoundViewModel = hiltViewModel()
) {
    LaunchedEffect(partyId, correctedRoundNumber) { viewModel.initialize(partyId, correctedRoundNumber) }

    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val currentOnNavigate by rememberUpdatedState(onNavigate)

    LaunchedEffect(viewModel) {
        viewModel.navigationEvents.collect { event -> currentOnNavigate(event) }
    }

    TarotRoundScreen(
        state = state,
        actions = TarotRoundActions(
            onTakerSelected = viewModel::onTakerSelected,
            onContractSelected = viewModel::onContractSelected,
            onPartnerSelected = viewModel::onPartnerSelected,
            onBoutsSelected = viewModel::onBoutsSelected,
            onPointsChanged = viewModel::onPointsChanged,
            onPetitAuBoutSelected = viewModel::onPetitAuBoutSelected,
            onAttackPoigneeSelected = viewModel::onAttackPoigneeSelected,
            onDefensePoigneeSelected = viewModel::onDefensePoigneeSelected,
            onChelemSelected = viewModel::onChelemSelected,
            onValidate = viewModel::onValidate
        ),
        modifier = modifier.keepScreenOn()
    )
}

/**
 * Stateless saisie, or correction, of a Manche of Tarot (Story 13.3): the contract first, then the
 * bonuses, the outcome and every Joueur's points shown before « Valider la manche ».
 */
@Composable
fun TarotRoundScreen(state: TarotRoundUiState, modifier: Modifier = Modifier, actions: TarotRoundActions = TarotRoundActions()) {
    when (state) {
        TarotRoundUiState.Loading -> Centered(modifier) {
            CircularProgressIndicator(modifier = Modifier.testTag("tarot_round_loading"))
        }
        TarotRoundUiState.Unavailable -> Centered(modifier) {
            Text(text = stringResource(R.string.round_entry_load_error), modifier = Modifier.testTag("tarot_round_unavailable"))
        }
        is TarotRoundUiState.Loaded -> LoadedRound(state, modifier, actions)
    }
}

@Composable
private fun LoadedRound(state: TarotRoundUiState.Loaded, modifier: Modifier, actions: TarotRoundActions) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
            .padding(16.dp)
    ) {
        ScreenHeader(
            title = if (state.isCorrection) {
                stringResource(R.string.belote_round_correction_title, state.roundNumber)
            } else {
                stringResource(R.string.belote_round_title, state.roundNumber)
            },
            titleTag = "tarot_round_title",
            modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
        )
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            Section(R.string.tarot_round_taker_title) {
                Choices(
                    options = state.players,
                    selected = state.players.firstOrNull { it.participantId == state.takerId },
                    label = { it.name },
                    tag = { "tarot_round_taker_${it.participantId}" },
                    onSelected = { actions.onTakerSelected(it.participantId) }
                )
            }
            Section(R.string.tarot_round_contract_title) {
                Choices(
                    options = TarotContract.entries,
                    selected = state.contract,
                    label = { stringResource(it.labelRes) },
                    tag = { "tarot_round_contract_${it.name}" },
                    onSelected = actions.onContractSelected
                )
            }
            if (state.callsPartner) {
                Section(R.string.tarot_round_partner_title) {
                    val options = listOf<TarotPartnerChoice>(TarotPartnerChoice.Alone) +
                        state.players.filter { it.participantId != state.takerId }.map { TarotPartnerChoice.Player(it.participantId) }
                    Choices(
                        options = options,
                        selected = state.partner,
                        label = { choice ->
                            when (choice) {
                                TarotPartnerChoice.Alone -> stringResource(R.string.tarot_round_alone)
                                is TarotPartnerChoice.Player -> state.players.first { it.participantId == choice.participantId }.name
                            }
                        },
                        tag = { choice ->
                            when (choice) {
                                TarotPartnerChoice.Alone -> "tarot_round_partner_alone"
                                is TarotPartnerChoice.Player -> "tarot_round_partner_${choice.participantId}"
                            }
                        },
                        onSelected = actions.onPartnerSelected
                    )
                }
            }
            Section(R.string.tarot_round_bouts_title) {
                Choices(
                    options = (0..3).toList(),
                    selected = state.bouts,
                    label = { it.toString() },
                    tag = { "tarot_round_bouts_$it" },
                    onSelected = actions.onBoutsSelected
                )
            }
            PointsField(state, actions.onPointsChanged)
            Text(text = stringResource(R.string.tarot_round_bonuses_title), style = MaterialTheme.typography.titleMedium)
            Section(R.string.tarot_round_petit_title) {
                Choices(
                    options = listOf(null) + TarotSide.entries,
                    selected = state.petitAuBout,
                    label = { side -> stringResource(side?.labelRes ?: R.string.tarot_none) },
                    tag = { "tarot_round_petit_${it?.name ?: "NONE"}" },
                    onSelected = actions.onPetitAuBoutSelected
                )
            }
            Section(R.string.tarot_round_attack_poignee_title) {
                PoigneeChoices(state.attackPoignee, "tarot_round_attack_poignee", actions.onAttackPoigneeSelected)
            }
            Section(R.string.tarot_round_defense_poignee_title) {
                PoigneeChoices(state.defensePoignee, "tarot_round_defense_poignee", actions.onDefensePoigneeSelected)
            }
            Section(R.string.tarot_round_chelem_title) {
                Choices(
                    options = listOf(null) + Chelem.entries,
                    selected = state.chelem,
                    label = { chelem -> stringResource(chelem?.labelRes ?: R.string.tarot_none) },
                    tag = { "tarot_round_chelem_${it?.name ?: "NONE"}" },
                    onSelected = actions.onChelemSelected
                )
            }
            Preview(state)
            state.errorRes?.let { error ->
                Text(
                    text = stringResource(error),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.testTag("tarot_round_error")
                )
            }
        }
        Button(
            onClick = actions.onValidate,
            enabled = state.canValidate,
            modifier = Modifier
                .padding(top = 16.dp)
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .testTag("tarot_round_validate")
        ) {
            Text(
                text = stringResource(if (state.isCorrection) R.string.belote_round_save_correction else R.string.belote_round_validate)
            )
        }
    }
}

@Composable
private fun Section(titleRes: Int, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(text = stringResource(titleRes), style = MaterialTheme.typography.titleSmall)
        content()
    }
}

/** One choice among [options], as chips that wrap; tapping the selected one keeps it. */
@Composable
private fun <T> Choices(
    options: List<T>,
    selected: T?,
    label: @Composable (T) -> String,
    tag: (T) -> String,
    onSelected: (T) -> Unit
) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { option ->
            FilterChip(
                selected = option == selected,
                onClick = { onSelected(option) },
                label = { Text(text = label(option)) },
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag(tag(option))
            )
        }
    }
}

@Composable
private fun PoigneeChoices(selected: Poignee?, tagPrefix: String, onSelected: (Poignee?) -> Unit) {
    Choices(
        options = listOf(null) + Poignee.entries,
        selected = selected,
        label = { poignee -> stringResource(poignee?.labelRes ?: R.string.tarot_none_feminine) },
        tag = { "${tagPrefix}_${it?.name ?: "NONE"}" },
        onSelected = onSelected
    )
}

/** The attack's points; the defense's and the points to make are shown once known. */
@Composable
private fun PointsField(state: TarotRoundUiState.Loaded, onPointsChanged: (String) -> Unit) {
    val points = state.points
    val hint = when {
        state.isPointsOutOfRange -> stringResource(R.string.tarot_round_points_error)
        points != null && state.bouts != null && state.requiredPoints != null -> stringResource(
            R.string.tarot_round_points_hint,
            state.totalPoints - points,
            state.bouts,
            state.requiredPoints
        )
        else -> stringResource(R.string.tarot_round_points_hint_no_bouts)
    }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        OutlinedTextField(
            value = state.pointsText,
            onValueChange = onPointsChanged,
            label = { Text(stringResource(R.string.tarot_round_points_label)) },
            singleLine = true,
            isError = state.isPointsOutOfRange,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
            modifier = Modifier
                .width(200.dp)
                .testTag("tarot_round_points")
        )
        Text(
            text = hint,
            style = MaterialTheme.typography.bodySmall,
            color = if (state.isPointsOutOfRange) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.testTag("tarot_round_points_hint")
        )
    }
}

/** The outcome and every Joueur's points, or what is still missing to compute them. */
@Composable
private fun Preview(state: TarotRoundUiState.Loaded) {
    val preview = state.preview
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("tarot_round_preview")
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(12.dp)) {
            if (preview == null) {
                Text(
                    text = stringResource(if (state.callsPartner) R.string.tarot_round_incomplete_five else R.string.tarot_round_incomplete),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text(
                    text = stringResource(
                        if (preview.made) R.string.tarot_round_preview_made else R.string.tarot_round_preview_down,
                        preview.margin,
                        preview.requiredPoints,
                        preview.perDefender
                    ),
                    style = MaterialTheme.typography.titleSmall,
                    color = if (preview.made) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    modifier = Modifier.testTag("tarot_round_outcome")
                )
                preview.points.forEach { (name, points) ->
                    Text(
                        text = stringResource(R.string.tarot_round_points_line, name, signed(points)),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.testTag("tarot_round_points_of_$name")
                    )
                }
            }
        }
    }
}

private fun signed(points: Int): String = if (points > 0) "+$points" else points.toString()

private val TarotSide.labelRes: Int
    get() = when (this) {
        TarotSide.ATTACK -> R.string.tarot_side_attack
        TarotSide.DEFENSE -> R.string.tarot_side_defense
    }

private val Poignee.labelRes: Int
    get() = when (this) {
        Poignee.SIMPLE -> R.string.tarot_poignee_simple
        Poignee.DOUBLE -> R.string.tarot_poignee_double
        Poignee.TRIPLE -> R.string.tarot_poignee_triple
    }

private val Chelem.labelRes: Int
    get() = when (this) {
        Chelem.ANNOUNCED_MADE -> R.string.tarot_chelem_announced_made
        Chelem.UNANNOUNCED_MADE -> R.string.tarot_chelem_unannounced_made
        Chelem.ANNOUNCED_FAILED -> R.string.tarot_chelem_announced_failed
        Chelem.DEFENSE_MADE -> R.string.tarot_chelem_defense_made
    }

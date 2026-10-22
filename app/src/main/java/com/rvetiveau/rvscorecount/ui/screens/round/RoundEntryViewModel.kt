package com.rvetiveau.rvscorecount.ui.screens.round

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rvetiveau.rvscorecount.domain.engine.GameEngine
import com.rvetiveau.rvscorecount.domain.model.CardSuit
import com.rvetiveau.rvscorecount.domain.model.Participant
import com.rvetiveau.rvscorecount.domain.model.PartyState
import com.rvetiveau.rvscorecount.domain.model.computeStandings
import com.rvetiveau.rvscorecount.domain.model.sevenChampion
import com.rvetiveau.rvscorecount.domain.repository.PartyRepository
import com.rvetiveau.rvscorecount.domain.scan.TrumpReader
import com.rvetiveau.rvscorecount.domain.usecase.CorrectRoundOutcome
import com.rvetiveau.rvscorecount.domain.usecase.CorrectRoundUseCase
import com.rvetiveau.rvscorecount.domain.usecase.GetAvailableGamesUseCase
import com.rvetiveau.rvscorecount.domain.usecase.RollDieUseCase
import com.rvetiveau.rvscorecount.domain.usecase.ValidateRoundOutcome
import com.rvetiveau.rvscorecount.domain.usecase.ValidateRoundUseCase
import com.rvetiveau.rvscorecount.ui.components.toStandingRows
import com.rvetiveau.rvscorecount.ui.navigation.NavigationEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

private const val SUIT_KEY = "suit"
private const val SCORE_KEY_PREFIX = "score_"
private const val SEVEN_HOLDER_KEY = "seven_holder"
private const val PANEL_KEY = "panel"

/**
 * Holds the die roll and score entry draft in memory only (AC 9, 12) until « Valider la Manche »
 * hands it to [ValidateRoundUseCase] (Story 3.4). Participants are Room-backed and validated
 * strictly (AC 1, 10); an unknown/corrupt saved value falls back to "not rolled"/"not entered"
 * rather than a default.
 */
@HiltViewModel
class RoundEntryViewModel @Inject constructor(
    private val partyRepository: PartyRepository,
    private val rollDieUseCase: RollDieUseCase,
    private val savedStateHandle: SavedStateHandle,
    private val getAvailableGamesUseCase: GetAvailableGamesUseCase,
    private val validateRoundUseCase: ValidateRoundUseCase,
    private val correctRoundUseCase: CorrectRoundUseCase,
    private val trumpReader: TrumpReader
) : ViewModel() {

    private val restoredSuit: CardSuit? = savedStateHandle.get<String>(SUIT_KEY)?.let { name ->
        runCatching { CardSuit.valueOf(name) }.getOrNull()
    }

    private val _uiState = MutableStateFlow(RoundEntryUiState(suit = restoredSuit))
    val uiState: StateFlow<RoundEntryUiState> = _uiState.asStateFlow()

    private val _navigationEvents = Channel<NavigationEvent>(Channel.BUFFERED)
    val navigationEvents: Flow<NavigationEvent> = _navigationEvents.receiveAsFlow()

    /** Each element asks the screen to open the camera for a photo of trumps (Story 9.1). */
    private val _photoRequests = Channel<Unit>(Channel.BUFFERED)
    val photoRequests: Flow<Unit> = _photoRequests.receiveAsFlow()

    private var initializedPartyId: UUID? = null
    private var initializedRoundNumber: Int? = null
    private var gameEngine: GameEngine? = null

    /** Whose trumps the photo being taken shows; null while the camera is not open for a scan. */
    private var photoParticipantId: UUID? = null
    private var readingJob: Job? = null

    /** Idempotent: a recomposition never re-triggers the Room read for the same partyId and Manche.
     * With [correctedRoundNumber], the form reopens that validated Manche with its stored scores,
     * 7 holder and suit, for the Participants who played it only. */
    fun initialize(partyId: UUID, correctedRoundNumber: Int? = null) {
        if (initializedPartyId == partyId && initializedRoundNumber == correctedRoundNumber) return
        initializedPartyId = partyId
        initializedRoundNumber = correctedRoundNumber
        _uiState.value = _uiState.value.copy(
            isLoading = true,
            loadError = false,
            content = null,
            correctedRoundNumber = correctedRoundNumber
        )

        viewModelScope.launch {
            try {
                val party = partyRepository.getPartyById(partyId)
                if (party == null || party.id != partyId || party.status != PartyState.IN_PROGRESS) {
                    failLoading()
                    return@launch
                }

                val participants = partyRepository.getParticipantsByPartyId(partyId)
                val positionsAreContinuous =
                    participants.map { it.listPosition }.sorted() == participants.indices.toList()
                val aggregateIsConsistent = participants.isNotEmpty() &&
                    positionsAreContinuous &&
                    participants.all { it.partyId == partyId } &&
                    participants.map { it.id }.distinct().size == participants.size

                if (!aggregateIsConsistent) {
                    failLoading()
                    return@launch
                }

                val engine = getAvailableGamesUseCase().firstOrNull { it.gameId == party.gameId }
                if (engine == null) {
                    gameEngine = null
                    failLoading()
                    return@launch
                }
                gameEngine = engine

                val ordered = participants.sortedBy { it.listPosition }
                if (correctedRoundNumber != null) {
                    loadCorrection(partyId, correctedRoundNumber, ordered, engine)
                    return@launch
                }

                var hasRestoredScore = false
                val restoredScores = ordered.associate { participant ->
                    val restored = restoredScore(participant.id)
                    if (restored != null) hasRestoredScore = true
                    participant.id to restored.orEmpty()
                }
                val restoredContent = RoundEntryContent(
                    participants = ordered.map { RoundEntryParticipant(it.id, it.name) },
                    scores = restoredScores,
                    totalPointsPerRound = engine.totalPointsPerRound,
                    sevenPoints = engine.suitSevenPoints,
                    sevenHolderId = restoredSevenHolderId(ordered).takeIf { engine.suitSevenPoints != null },
                    summary = loadSummary(partyId, participants, party.currentDealerIndex),
                    faceValueCards = engine.faceValueCards
                )
                val content = withSuggestion(restoredContent, engine)

                // A draft in progress reopens the score form; otherwise only the die is shown.
                val restoredPanel = savedStateHandle.get<String>(PANEL_KEY)
                    ?.let { name -> runCatching { RoundEntryPanel.valueOf(name) }.getOrNull() }
                    ?: if (hasRestoredScore) RoundEntryPanel.SCORES else RoundEntryPanel.NONE

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    loadError = false,
                    content = content,
                    panel = restoredPanel
                )
            } catch (cancellation: CancellationException) {
                initializedPartyId = null
                throw cancellation
            } catch (_: Exception) {
                failLoading()
            }
        }
    }

    /** The validated Manche [roundNumber] in the score form, its suit in place of the die. A draft
     * kept across process death wins over the stored value; a stored score or 7 holder that no
     * Participant of the Partie owns is a load error. */
    private suspend fun loadCorrection(
        partyId: UUID,
        roundNumber: Int,
        ordered: List<Participant>,
        engine: GameEngine
    ) {
        val round = partyRepository.getRoundsByPartyId(partyId).firstOrNull { it.roundNumber == roundNumber }
        val storedPoints = round?.let { partyRepository.getScoresByRoundId(it.id) }
            ?.associate { it.participantId to it.points }
        val players = ordered.filter { storedPoints?.containsKey(it.id) == true }
        if (round == null || storedPoints.isNullOrEmpty() || players.size != storedPoints.size ||
            (round.sevenHolderId != null && round.sevenHolderId !in storedPoints)
        ) {
            failLoading()
            return
        }

        val scores = players.associate { player ->
            player.id to (restoredScore(player.id) ?: storedPoints.getValue(player.id).toString())
        }
        val content = RoundEntryContent(
            participants = players.map { RoundEntryParticipant(it.id, it.name) },
            scores = scores,
            totalPointsPerRound = engine.totalPointsPerRound,
            sevenPoints = engine.suitSevenPoints,
            sevenHolderId = (restoredSevenHolderId(players) ?: round.sevenHolderId)
                .takeIf { engine.suitSevenPoints != null },
            faceValueCards = engine.faceValueCards
        )
        _uiState.value = _uiState.value.copy(
            suit = round.suit,
            isLoading = false,
            loadError = false,
            content = withSuggestion(content, engine),
            panel = RoundEntryPanel.SCORES
        )
    }

    private fun failLoading() {
        initializedPartyId = null
        _uiState.value = _uiState.value.copy(isLoading = false, loadError = true, content = null)
    }

    /** The saved draft of [participantId], or null when absent or corrupt (never coerced). */
    private fun restoredScore(participantId: UUID): String? =
        savedStateHandle.get<String>("$SCORE_KEY_PREFIX$participantId")
            ?.takeIf { it.isNotEmpty() && it.all(Char::isDigit) && it.toIntOrNull() != null }

    private fun restoredSevenHolderId(players: List<Participant>): UUID? =
        savedStateHandle.get<String>(SEVEN_HOLDER_KEY)
            ?.let { saved -> players.firstOrNull { it.id.toString() == saved }?.id }

    fun retry(partyId: UUID, correctedRoundNumber: Int? = null) {
        if (_uiState.value.loadError) initialize(partyId, correctedRoundNumber)
    }

    /** Same intent for « Lancer » and « Relancer » (Story 3.1): always draws exactly once per
     * tap, preserving any already-loaded participants and drafts (AC 8). */
    fun onRoll() {
        if (!_uiState.value.rollEnabled) return
        val suit = rollDieUseCase()
        savedStateHandle[SUIT_KEY] = suit.name
        _uiState.value = _uiState.value.copy(suit = suit)
    }

    /** Shows [panel] under the die, or hides it when it is already shown; one panel at a time. */
    fun onPanelSelected(panel: RoundEntryPanel) {
        if (_uiState.value.content == null) return
        val next = if (_uiState.value.panel == panel) RoundEntryPanel.NONE else panel
        savedStateHandle[PANEL_KEY] = next.name
        _uiState.value = _uiState.value.copy(panel = next)
    }

    /** Records who took the 7 of the die's suit; ignored when the game has none or the id is unknown.
     * The scores that come from a scan follow: the 7's points move to the new holder (Story 9.1). */
    fun onSevenHolderSelected(participantId: UUID) {
        if (_uiState.value.isSaving) return
        val content = _uiState.value.content ?: return
        if (content.sevenPoints == null || content.participants.none { it.id == participantId }) return
        _uiState.value = _uiState.value.copy(content = content.copy(sevenHolderId = participantId), saveError = false)
        savedStateHandle[SEVEN_HOLDER_KEY] = participantId.toString()

        val engine = gameEngine ?: return
        content.scannedTrumps.forEach { (scannedId, trumps) ->
            val score = engine.scoreOfCards(trumps, tookSuitSeven = scannedId == participantId)
            setScore(scannedId, score.toString(), scannedTrumps = trumps)
        }
        _uiState.value.scan?.let { scan -> _uiState.value = _uiState.value.copy(scan = withScore(scan)) }
    }

    /** Updates only the targeted Participant's draft (AC 3, 4); unknown ids and invalid raw
     * values (signs, letters, decimals, Int overflow) are ignored rather than transformed. A score
     * typed by hand no longer comes from a scan: its trumps are forgotten (Story 9.1). */
    fun onScoreChanged(participantId: UUID, rawValue: String) = setScore(participantId, rawValue, scannedTrumps = null)

    private fun setScore(participantId: UUID, rawValue: String, scannedTrumps: Set<Int>?) {
        if (_uiState.value.isSaving) return
        val content = _uiState.value.content ?: return
        val engine = gameEngine ?: return
        if (content.participants.none { it.id == participantId }) return
        val isValid = rawValue.isEmpty() || (rawValue.all(Char::isDigit) && rawValue.toIntOrNull() != null)
        if (!isValid) return

        val trumps = if (scannedTrumps == null) {
            content.scannedTrumps - participantId
        } else {
            content.scannedTrumps + (participantId to scannedTrumps)
        }
        val nextContent = withSuggestion(
            content.copy(scores = content.scores + (participantId to rawValue), scannedTrumps = trumps),
            engine
        )
        _uiState.value = _uiState.value.copy(content = nextContent, saveError = false)

        val key = "$SCORE_KEY_PREFIX$participantId"
        if (rawValue.isEmpty()) savedStateHandle.remove<String>(key) else savedStateHandle[key] = rawValue
    }

    /** Opens the camera for a photo of [participantId]'s trumps, when the game can count them. */
    fun onScanRequested(participantId: UUID) {
        val state = _uiState.value
        val content = state.content ?: return
        if (state.isSaving || content.faceValueCards == null || content.participants.none { it.id == participantId }) return
        photoParticipantId = participantId
        _photoRequests.trySend(Unit)
    }

    /** Back from the camera; [photoPath] is null when no photo was taken, which changes nothing. */
    fun onPhotoTaken(photoPath: String?) {
        val participantId = photoParticipantId ?: return
        photoParticipantId = null
        val range = _uiState.value.content?.faceValueCards ?: return
        if (photoPath == null) return

        _uiState.value = _uiState.value.copy(scan = TrumpScan(participantId))
        readingJob?.cancel()
        readingJob = viewModelScope.launch {
            val trumps = try {
                trumpReader.read(photoPath, range)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                null
            }
            val scan = _uiState.value.scan?.takeIf { it.participantId == participantId && it.isReading } ?: return@launch
            val read = scan.copy(
                isReading = false,
                selected = trumps.orEmpty(),
                readFailed = trumps == null,
                nothingRecognized = trumps?.isEmpty() == true
            )
            _uiState.value = _uiState.value.copy(scan = withScore(read))
        }
    }

    fun onScanTrumpToggled(trump: Int) {
        val scan = _uiState.value.scan?.takeUnless { it.isReading } ?: return
        if (_uiState.value.content?.faceValueCards?.contains(trump) != true) return
        val selected = if (trump in scan.selected) scan.selected - trump else scan.selected + trump
        _uiState.value = _uiState.value.copy(scan = withScore(scan.copy(selected = selected)))
    }

    /** « Reprendre la photo »: the confirmation stays as it is until a new photo replaces it. */
    fun onScanRetake() {
        val scan = _uiState.value.scan?.takeUnless { it.isReading } ?: return
        photoParticipantId = scan.participantId
        _photoRequests.trySend(Unit)
    }

    fun onScanDismissed() {
        readingJob?.cancel()
        photoParticipantId = null
        _uiState.value = _uiState.value.copy(scan = null)
    }

    /** Writes the scanned score; refused while a checked trump is already another Participant's. */
    fun onScanConfirmed() {
        val scan = _uiState.value.scan?.takeUnless { it.isReading } ?: return
        val content = _uiState.value.content ?: return
        val takenByOthers = content.trumpsTakenByOthers(scan.participantId)
        if (scan.selected.any { it in takenByOthers }) return
        setScore(scan.participantId, scan.score.toString(), scannedTrumps = scan.selected)
        _uiState.value = _uiState.value.copy(scan = null)
    }

    private fun withScore(scan: TrumpScan): TrumpScan {
        val engine = gameEngine ?: return scan
        val includesSeven = _uiState.value.content?.sevenHolderId == scan.participantId
        return scan.copy(score = engine.scoreOfCards(scan.selected, includesSeven), includesSeven = includesSeven)
    }

    /** Persists the Manche through [ValidateRoundUseCase] (Story 3.4, AC 1-7), or the correction
     * through [CorrectRoundUseCase], then returns to PartyScreen. Ignored unless
     * [RoundEntryUiState.canValidate]; any failure, including a sum mismatch reported by the
     * engine, keeps the drafts and shows [RoundEntryUiState.saveError]. */
    fun onValidate() {
        val state = _uiState.value
        val partyId = initializedPartyId ?: return
        val suit = state.suit ?: return
        val scores = state.content?.toScores() ?: return
        if (!state.canValidate) return
        val sevenHolderId = state.content.sevenHolderId
        val correctedRoundNumber = state.correctedRoundNumber
        _uiState.value = state.copy(isSaving = true, saveError = false)

        viewModelScope.launch {
            val saved = if (correctedRoundNumber != null) {
                correctRoundUseCase(partyId, correctedRoundNumber, scores, sevenHolderId)
                    .getOrNull() is CorrectRoundOutcome.Corrected
            } else {
                validateRoundUseCase(partyId, suit, scores, sevenHolderId)
                    .getOrNull() is ValidateRoundOutcome.Validated
            }
            if (saved) {
                _navigationEvents.send(NavigationEvent.NavigateBack)
            } else {
                _uiState.value = _uiState.value.copy(isSaving = false, saveError = true)
            }
        }
    }

    /** Classement of the Partie so far; an inconsistent score or 7 holder throws
     * IllegalArgumentException, which [initialize] reports as a load error. */
    private suspend fun loadSummary(
        partyId: UUID,
        participants: List<Participant>,
        currentDealerIndex: Int
    ): RoundEntrySummary {
        val rounds = partyRepository.getRoundsByPartyId(partyId)
        sevenChampion(participants, rounds)
        val standings = computeStandings(participants, rounds.flatMap { partyRepository.getScoresByRoundId(it.id) })
        val dealerId = participants.firstOrNull { it.listPosition == currentDealerIndex }?.id
        return RoundEntrySummary(standings = standings.toStandingRows(dealerId = dealerId))
    }

    /** Recomputed from the drafts on every change, so it follows each digit typed in another field. */
    private fun withSuggestion(content: RoundEntryContent, engine: GameEngine): RoundEntryContent {
        val targetId = content.participants
            .map { it.id }
            .filter { content.scores[it].isNullOrEmpty() }
            .singleOrNull()
            ?.takeIf { content.participants.size > 1 }
            ?: return content.copy(suggestion = null)
        val enteredScores = content.participants
            .asSequence()
            .filterNot { it.id == targetId }
            .associate { participant -> participant.id to checkNotNull(content.scores[participant.id]?.toIntOrNull()) }
        val score = engine.precomputeLastScore(enteredScores)
        return content.copy(suggestion = ScoreSuggestion(targetId, score).takeIf { score >= 0 })
    }
}

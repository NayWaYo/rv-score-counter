package com.rvetiveau.rvscorecount.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import com.rvetiveau.rvscorecount.ui.theme.RvScoreCounterTheme
import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Instrumented tests for the pure [ScoreField] (AC 2, 3, 6, 7): numeric filtering, IME action
 * wiring and the Participant label — no ViewModel or SavedStateHandle involved. `KeyboardType.Number`
 * is only an IME hint, so the digit filter below is the behavior actually under test. */
class ScoreFieldTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val participantId = UUID.randomUUID()

    private fun setContent(
        value: String = "",
        imeAction: ImeAction = ImeAction.Next,
        isPrecomputed: Boolean = false,
        isError: Boolean = false,
        onValueChange: (String) -> Unit = {},
        onImeAction: (ImeAction) -> Unit = {}
    ) {
        composeTestRule.setContent {
            RvScoreCounterTheme {
                Box(modifier = Modifier.padding(32.dp)) {
                    ScoreField(
                        participantId = participantId,
                        participantName = "Alice",
                        value = value,
                        onValueChange = onValueChange,
                        imeAction = imeAction,
                        onImeAction = onImeAction,
                        isPrecomputed = isPrecomputed,
                        isError = isError
                    )
                }
            }
        }
    }

    @Test
    fun showsTheParticipantNameAsLabel() {
        setContent()

        composeTestRule.onNodeWithTag("score_field_label_$participantId").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Score de Alice").assertIsDisplayed()
    }

    @Test
    fun showsTheCalculationIconAndDedicatedDescriptionWhenPrecomputed() {
        setContent(value = "250", isPrecomputed = true)

        composeTestRule.onNodeWithTag(
            "score_field_calculate_icon_$participantId",
            useUnmergedTree = true
        ).assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Score calculé automatiquement pour Alice, modifiable")
            .assertIsDisplayed()
    }

    @Test
    fun exposesTheMaterialErrorState() {
        setContent(value = "251", isError = true)

        composeTestRule.onNodeWithTag("score_field_$participantId")
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Error))
    }

    @Test
    fun acceptsDigitsIncludingZero() {
        var latest: String? = null
        setContent(onValueChange = { latest = it })

        composeTestRule.onNodeWithTag("score_field_$participantId").performTextInput("0")

        assertEquals("0", latest)
    }

    @Test
    fun rejectsASignWithoutCallingOnValueChange() {
        var called = false
        setContent(onValueChange = { called = true })

        composeTestRule.onNodeWithTag("score_field_$participantId").performTextInput("-")

        assertTrue(!called)
    }

    @Test
    fun rejectsALetterWithoutCallingOnValueChange() {
        var called = false
        setContent(onValueChange = { called = true })

        composeTestRule.onNodeWithTag("score_field_$participantId").performTextInput("a")

        assertTrue(!called)
    }

    @Test
    fun rejectsADecimalSeparatorWithoutCallingOnValueChange() {
        var called = false
        setContent(value = "1", onValueChange = { called = true })

        composeTestRule.onNodeWithTag("score_field_$participantId").performTextInput(".")

        assertTrue(!called)
    }

    @Test
    fun acceptsAValidReplacement() {
        var latest: String? = null
        setContent(value = "1", onValueChange = { latest = it })

        composeTestRule.onNodeWithTag("score_field_$participantId").performTextReplacement("250")

        assertEquals("250", latest)
    }

    @Test
    fun rejectsAnIntOverflowReplacement() {
        var called = false
        setContent(onValueChange = { called = true })

        composeTestRule.onNodeWithTag("score_field_$participantId")
            .performTextReplacement("99999999999")

        assertTrue(!called)
    }

    @Test
    fun keepsTheParticipantLabelVisibleWithLargeFontScale() {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(density = 1f, fontScale = 2f)) {
                RvScoreCounterTheme {
                    Box(modifier = Modifier.padding(32.dp)) {
                        ScoreField(
                            participantId = participantId,
                            participantName = "Alice au prénom particulièrement long",
                            value = "",
                            onValueChange = {},
                            imeAction = ImeAction.Done,
                            onImeAction = {}
                        )
                    }
                }
            }
        }

        composeTestRule.onNodeWithTag("score_field_label_$participantId").assertIsDisplayed()
    }

    @Test
    fun triggersOnImeActionNextFromTheKeyboardAction() {
        var triggered: ImeAction? = null
        setContent(imeAction = ImeAction.Next, onImeAction = { triggered = it })

        composeTestRule.onNodeWithTag("score_field_$participantId").performImeAction()

        assertEquals(ImeAction.Next, triggered)
    }

    @Test
    fun triggersOnImeActionDoneFromTheKeyboardAction() {
        var triggered: ImeAction? = null
        setContent(imeAction = ImeAction.Done, onImeAction = { triggered = it })

        composeTestRule.onNodeWithTag("score_field_$participantId").performImeAction()

        assertEquals(ImeAction.Done, triggered)
    }

    @Test
    fun placesTheNameBesideTheFieldOnOneRow() {
        setContent()

        val label = composeTestRule.onNodeWithTag("score_field_label_$participantId").getUnclippedBoundsInRoot()
        val field = composeTestRule.onNodeWithTag("score_field_$participantId").getUnclippedBoundsInRoot()

        assertTrue(label.right <= field.left)
        assertTrue(label.top < field.bottom && field.top < label.bottom)
    }
}

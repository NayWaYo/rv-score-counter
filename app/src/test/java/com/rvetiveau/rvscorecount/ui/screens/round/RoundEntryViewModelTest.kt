package com.rvetiveau.rvscorecount.ui.screens.round

import androidx.lifecycle.SavedStateHandle
import com.rvetiveau.rvscorecount.domain.model.CardSuit
import com.rvetiveau.rvscorecount.domain.usecase.RollDieUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RoundEntryViewModelTest {

    private class FakeRollDieUseCase(private vararg val results: CardSuit) : RollDieUseCase() {
        var invocationCount = 0
            private set

        override fun invoke(random: kotlin.random.Random): CardSuit =
            results[invocationCount.coerceAtMost(results.lastIndex)].also { invocationCount++ }
    }

    private fun viewModel(
        savedStateHandle: SavedStateHandle = SavedStateHandle(),
        rollDieUseCase: RollDieUseCase = FakeRollDieUseCase(CardSuit.SPADE)
    ) = RoundEntryViewModel(rollDieUseCase, savedStateHandle)

    @Test
    fun `initial state has no suit and does not roll implicitly`() {
        val vm = viewModel()

        assertNull(vm.uiState.value.suit)
    }

    @Test
    fun `onRoll invokes the use case exactly once and publishes its result`() {
        val rollDieUseCase = FakeRollDieUseCase(CardSuit.HEART)
        val vm = viewModel(rollDieUseCase = rollDieUseCase)

        vm.onRoll()

        assertEquals(1, rollDieUseCase.invocationCount)
        assertEquals(CardSuit.HEART, vm.uiState.value.suit)
    }

    @Test
    fun `onRoll persists the suit name to SavedStateHandle`() {
        val savedStateHandle = SavedStateHandle()
        val vm = viewModel(savedStateHandle)

        vm.onRoll()

        assertEquals(vm.uiState.value.suit!!.name, savedStateHandle.get<String>("suit"))
    }

    @Test
    fun `restores each of the four saved suit values`() {
        CardSuit.entries.forEach { suit ->
            val vm = viewModel(SavedStateHandle(mapOf("suit" to suit.name)))
            assertEquals(suit, vm.uiState.value.suit)
        }
    }

    @Test
    fun `an unknown saved value falls back to not rolled instead of a default suit`() {
        val vm = viewModel(SavedStateHandle(mapOf("suit" to "UNKNOWN_SUIT")))

        assertNull(vm.uiState.value.suit)
    }

    @Test
    fun `reroll invokes the use case once again and replaces the result`() {
        val rollDieUseCase = FakeRollDieUseCase(CardSuit.SPADE, CardSuit.DIAMOND)
        val vm = viewModel(rollDieUseCase = rollDieUseCase)

        vm.onRoll()
        vm.onRoll()

        assertEquals(2, rollDieUseCase.invocationCount)
        assertEquals(CardSuit.DIAMOND, vm.uiState.value.suit)
    }
}

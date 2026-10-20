package com.rvetiveau.rvscorecount.ui.navigation

import com.rvetiveau.rvscorecount.domain.repository.AppearanceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AppearanceViewModelTest {

    private class FakeAppearanceRepository(initial: Boolean?) : AppearanceRepository {
        override val darkTheme = MutableStateFlow(initial)
        override fun setDarkTheme(dark: Boolean) {
            darkTheme.value = dark
        }
    }

    @Test
    fun `no picked theme follows the phone`() {
        val vm = AppearanceViewModel(FakeAppearanceRepository(initial = null))

        assertNull(vm.darkTheme.value)
    }

    @Test
    fun `the switch picks the opposite of the theme shown`() {
        val repository = FakeAppearanceRepository(initial = null)
        val vm = AppearanceViewModel(repository)

        vm.onToggle(currentlyDark = true)
        assertEquals(false, vm.darkTheme.value)

        vm.onToggle(currentlyDark = false)
        assertEquals(true, vm.darkTheme.value)
    }

    @Test
    fun `a picked theme is exposed as is`() {
        val vm = AppearanceViewModel(FakeAppearanceRepository(initial = false))

        val darkTheme: StateFlow<Boolean?> = vm.darkTheme
        assertEquals(false, darkTheme.value)
    }
}

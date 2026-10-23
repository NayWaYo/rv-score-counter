package com.rvetiveau.rvscorecount.ui.screens.backup

import com.rvetiveau.rvscorecount.domain.model.BackupContent
import com.rvetiveau.rvscorecount.domain.repository.BackupRepository
import com.rvetiveau.rvscorecount.domain.usecase.ExportBackupUseCase
import com.rvetiveau.rvscorecount.domain.usecase.ObserveBackupContentUseCase
import java.io.IOException
import java.time.LocalDate
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BackupViewModelTest {

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private class FakeBackupRepository(
        val content: Flow<BackupContent> = MutableStateFlow(BackupContent(players = 4, parties = 7)),
        var failure: Exception? = null,
        var gate: CompletableDeferred<Unit>? = null
    ) : BackupRepository {
        val destinations = mutableListOf<String>()

        override fun observeContent(): Flow<BackupContent> = content

        override suspend fun export(destination: String) {
            destinations += destination
            gate?.await()
            failure?.let { throw it }
        }
    }

    private fun viewModel(repository: FakeBackupRepository) =
        BackupViewModel(ObserveBackupContentUseCase(repository), ExportBackupUseCase(repository))

    private fun BackupViewModel.loaded() = uiState.value as BackupUiState.Loaded

    @Test
    fun `announces what the backup holds and follows the database`() = runTest {
        val content = MutableStateFlow(BackupContent(players = 4, parties = 7))
        val vm = viewModel(FakeBackupRepository(content))
        assertEquals(BackupContent(4, 7), vm.loaded().content)

        content.value = BackupContent(players = 5, parties = 8)
        assertEquals(BackupContent(5, 8), vm.loaded().content)
    }

    @Test
    fun `a database that cannot be read says so`() = runTest {
        val vm = viewModel(FakeBackupRepository(flow { throw IOException("disk") }))
        assertEquals(BackupUiState.LoadError, vm.uiState.value)
    }

    @Test
    fun `exports to the chosen document and confirms it`() = runTest {
        val repository = FakeBackupRepository()
        val vm = viewModel(repository)

        vm.onExportDestinationChosen("content://downloads/backup.json")

        assertEquals(listOf("content://downloads/backup.json"), repository.destinations)
        assertEquals(ExportResult.EXPORTED, vm.loaded().exportResult)
        assertFalse(vm.loaded().isExporting)
    }

    @Test
    fun `a cancelled file picker does nothing`() = runTest {
        val repository = FakeBackupRepository()
        val vm = viewModel(repository)

        vm.onExportDestinationChosen(null)

        assertTrue(repository.destinations.isEmpty())
        assertNull(vm.loaded().exportResult)
    }

    @Test
    fun `a failed write is reported, and the next export clears it`() = runTest {
        val repository = FakeBackupRepository(failure = IOException("full"))
        val vm = viewModel(repository)

        vm.onExportDestinationChosen("content://a")
        assertEquals(ExportResult.FAILED, vm.loaded().exportResult)

        repository.failure = null
        vm.onExportDestinationChosen("content://b")
        assertEquals(ExportResult.EXPORTED, vm.loaded().exportResult)
    }

    @Test
    fun `exporting is shown and a second export waits for the first`() = runTest {
        val gate = CompletableDeferred<Unit>()
        val repository = FakeBackupRepository(gate = gate)
        val vm = viewModel(repository)

        vm.onExportDestinationChosen("content://a")
        assertTrue(vm.loaded().isExporting)
        vm.onExportDestinationChosen("content://b")
        assertEquals(listOf("content://a"), repository.destinations)

        gate.complete(Unit)
        assertFalse(vm.loaded().isExporting)
        assertEquals(ExportResult.EXPORTED, vm.loaded().exportResult)
    }

    @Test
    fun `the proposed file name carries the day's date`() {
        assertEquals("rv-score-counter-2026-10-23.json", backupFileName(LocalDate.of(2026, 10, 23)))
    }
}

package com.rvetiveau.rvscorecount.ui.screens.backup

import com.rvetiveau.rvscorecount.domain.model.BackupContent
import com.rvetiveau.rvscorecount.domain.model.BackupReading
import com.rvetiveau.rvscorecount.domain.repository.BackupRepository
import com.rvetiveau.rvscorecount.domain.usecase.ExportBackupUseCase
import com.rvetiveau.rvscorecount.domain.usecase.ObserveBackupContentUseCase
import com.rvetiveau.rvscorecount.domain.usecase.ReadBackupUseCase
import com.rvetiveau.rvscorecount.domain.usecase.RestoreBackupUseCase
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

    private val saved = BackupReading.Valid(createdAt = 1_761_000_000_000, content = BackupContent(players = 6, parties = 12))

    private class FakeBackupRepository(
        val content: MutableStateFlow<BackupContent> = MutableStateFlow(BackupContent(players = 4, parties = 7)),
        val failingContent: Flow<BackupContent>? = null,
        var exportFailure: Exception? = null,
        var reading: BackupReading = BackupReading.NotABackup,
        var readFailure: Exception? = null,
        var restoreFailure: Exception? = null,
        var gate: CompletableDeferred<Unit>? = null
    ) : BackupRepository {
        val exported = mutableListOf<String>()
        val read = mutableListOf<String>()
        val restored = mutableListOf<String>()

        override fun observeContent(): Flow<BackupContent> = failingContent ?: content

        override suspend fun export(destination: String) {
            exported += destination
            gate?.await()
            exportFailure?.let { throw it }
        }

        override suspend fun read(source: String): BackupReading {
            read += source
            gate?.await()
            readFailure?.let { throw it }
            return reading
        }

        override suspend fun restore(source: String): BackupReading {
            restored += source
            gate?.await()
            restoreFailure?.let { throw it }
            (reading as? BackupReading.Valid)?.let { content.value = it.content }
            return reading
        }
    }

    private fun viewModel(repository: FakeBackupRepository) = BackupViewModel(
        ObserveBackupContentUseCase(repository),
        ExportBackupUseCase(repository),
        ReadBackupUseCase(repository),
        RestoreBackupUseCase(repository)
    )

    private fun BackupViewModel.loaded() = uiState.value as BackupUiState.Loaded

    @Test
    fun `announces what the backup holds and follows the database`() = runTest {
        val repository = FakeBackupRepository()
        val vm = viewModel(repository)
        assertEquals(BackupContent(4, 7), vm.loaded().content)

        repository.content.value = BackupContent(players = 5, parties = 8)
        assertEquals(BackupContent(5, 8), vm.loaded().content)
    }

    @Test
    fun `a database that cannot be read says so`() = runTest {
        val vm = viewModel(FakeBackupRepository(failingContent = flow { throw IOException("disk") }))
        assertEquals(BackupUiState.LoadError, vm.uiState.value)
    }

    @Test
    fun `exports to the chosen document and confirms it`() = runTest {
        val repository = FakeBackupRepository()
        val vm = viewModel(repository)

        vm.onExportDestinationChosen("content://downloads/backup.json")

        assertEquals(listOf("content://downloads/backup.json"), repository.exported)
        assertEquals(BackupMessage.EXPORTED, vm.loaded().message)
        assertNull(vm.loaded().operation)
    }

    @Test
    fun `a cancelled file picker does nothing`() = runTest {
        val repository = FakeBackupRepository()
        val vm = viewModel(repository)

        vm.onExportDestinationChosen(null)
        vm.onRestoreSourceChosen(null)

        assertTrue(repository.exported.isEmpty() && repository.read.isEmpty())
        assertEquals(BackupUiState.Loaded(BackupContent(4, 7)), vm.uiState.value)
    }

    @Test
    fun `a failed write is reported, and the next export clears it`() = runTest {
        val repository = FakeBackupRepository(exportFailure = IOException("full"))
        val vm = viewModel(repository)

        vm.onExportDestinationChosen("content://a")
        assertEquals(BackupMessage.EXPORT_FAILED, vm.loaded().message)

        repository.exportFailure = null
        vm.onExportDestinationChosen("content://b")
        assertEquals(BackupMessage.EXPORTED, vm.loaded().message)
    }

    @Test
    fun `one operation at a time`() = runTest {
        val gate = CompletableDeferred<Unit>()
        val repository = FakeBackupRepository(gate = gate)
        val vm = viewModel(repository)

        vm.onExportDestinationChosen("content://a")
        assertEquals(BackupOperation.EXPORTING, vm.loaded().operation)
        vm.onExportDestinationChosen("content://b")
        vm.onRestoreSourceChosen("content://c")
        assertEquals(listOf("content://a"), repository.exported)
        assertTrue(repository.read.isEmpty())

        gate.complete(Unit)
        assertNull(vm.loaded().operation)
        assertEquals(BackupMessage.EXPORTED, vm.loaded().message)
    }

    @Test
    fun `a valid backup asks for confirmation without changing anything`() = runTest {
        val gate = CompletableDeferred<Unit>()
        val repository = FakeBackupRepository(reading = saved, gate = gate)
        val vm = viewModel(repository)

        vm.onRestoreSourceChosen("content://drive/backup.json")
        assertEquals(BackupOperation.READING, vm.loaded().operation)
        gate.complete(Unit)

        assertEquals(PendingRestore("content://drive/backup.json", saved.createdAt, saved.content), vm.loaded().pendingRestore)
        assertNull(vm.loaded().operation)
        assertTrue(repository.restored.isEmpty())
    }

    @Test
    fun `replacing restores the chosen backup and shows its content`() = runTest {
        val repository = FakeBackupRepository(reading = saved)
        val vm = viewModel(repository)

        vm.onRestoreSourceChosen("content://backup")
        vm.onRestoreConfirmed()

        assertEquals(listOf("content://backup"), repository.restored)
        assertNull(vm.loaded().pendingRestore)
        assertEquals(BackupMessage.RESTORED, vm.loaded().message)
        assertEquals(BackupContent(6, 12), vm.loaded().content)
    }

    @Test
    fun `cancelling the confirmation restores nothing`() = runTest {
        val repository = FakeBackupRepository(reading = saved)
        val vm = viewModel(repository)

        vm.onRestoreSourceChosen("content://backup")
        vm.onRestoreDismissed()
        vm.onRestoreConfirmed()

        assertNull(vm.loaded().pendingRestore)
        assertTrue(repository.restored.isEmpty())
        assertNull(vm.loaded().message)
    }

    @Test
    fun `a refused file says why and asks nothing`() = runTest {
        val repository = FakeBackupRepository()
        val vm = viewModel(repository)
        listOf(
            BackupReading.NotABackup to BackupMessage.NOT_A_BACKUP,
            BackupReading.NewerFormat to BackupMessage.NEWER_FORMAT,
            BackupReading.Damaged to BackupMessage.DAMAGED,
            BackupReading.Unreadable to BackupMessage.UNREADABLE
        ).forEach { (reading, message) ->
            repository.reading = reading
            vm.onRestoreSourceChosen("content://file")
            assertEquals(message, vm.loaded().message)
            assertNull(vm.loaded().pendingRestore)
        }
        repository.readFailure = SecurityException("revoked")
        vm.onRestoreSourceChosen("content://file")
        assertEquals(BackupMessage.UNREADABLE, vm.loaded().message)
        assertTrue(repository.restored.isEmpty())
    }

    @Test
    fun `a failed restore is reported`() = runTest {
        val repository = FakeBackupRepository(reading = saved, restoreFailure = IllegalStateException("constraint"))
        val vm = viewModel(repository)

        vm.onRestoreSourceChosen("content://backup")
        vm.onRestoreConfirmed()

        assertEquals(BackupMessage.RESTORE_FAILED, vm.loaded().message)
        assertEquals(BackupContent(4, 7), vm.loaded().content)
    }

    @Test
    fun `a file that changed before replacing is refused then`() = runTest {
        val repository = FakeBackupRepository(reading = saved)
        val vm = viewModel(repository)

        vm.onRestoreSourceChosen("content://backup")
        repository.reading = BackupReading.Damaged
        vm.onRestoreConfirmed()

        assertEquals(BackupMessage.DAMAGED, vm.loaded().message)
    }

    @Test
    fun `the proposed file name carries the day's date`() {
        assertEquals("rv-score-counter-2026-10-23.json", backupFileName(LocalDate.of(2026, 10, 23)))
    }
}

package com.rvetiveau.rvscorecount.data.scan

import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

/**
 * Reference photos of Tarot trumps held in a fan, taken by the user on 2026-10-22 (Story 9.1, AC 7).
 * Each file under `assets/scan/` is named after the trumps it shows (`03_3-6-10-14.webp`); some also
 * show trumps lying on the table behind the fan, which must not be read.
 */
class MlKitTrumpReaderTest {

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val reader = MlKitTrumpReader()

    @Test
    fun readsExactlyTheTrumpsOfEveryReferencePhoto() = runTest(timeout = kotlin.time.Duration.parse("5m")) {
        val photos = instrumentation.context.assets.list(DIRECTORY).orEmpty().sorted()
        val results = photos.associateWith { name ->
            val read = reader.read(copyToCache(name).path, 1..20)
            expected(name) to read
        }

        val wrong = results.filterValues { (expected, read) -> expected != read }
        assertEquals("${photos.size} photos, wrong: $wrong", emptyMap<String, Pair<Set<Int>, Set<Int>>>(), wrong)
    }

    @Test
    fun deletesThePhotoOnceRead() = runTest {
        val photo = copyToCache(instrumentation.context.assets.list(DIRECTORY).orEmpty().sorted().first())

        reader.read(photo.path, 1..20)

        assertFalse(photo.exists())
    }

    private fun expected(name: String): Set<Int> =
        name.substringAfter('_').substringBefore('.').split('-').map(String::toInt).toSet()

    /** The reader deletes what it reads: it gets a copy. */
    private fun copyToCache(name: String): File {
        val file = File(instrumentation.targetContext.cacheDir, "reference-$name")
        instrumentation.context.assets.open("$DIRECTORY/$name").use { input ->
            file.outputStream().use { output -> input.copyTo(output) }
        }
        return file
    }

    private companion object {
        const val DIRECTORY = "scan"
    }
}

package com.aktarjabed.inbusiness.utils.pdf

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * Every shared invoice writes a new PDF into `cacheDir/invoices`. The cache must stay
 * bounded without ever deleting a document that may still be shared with another app.
 */
class PdfCacheManagerTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val now = 1_700_000_000_000L
    private val oneDay = 24 * 60 * 60 * 1000L

    private fun pdf(name: String, ageMillis: Long, dir: File): File =
        File(dir, "$name.pdf").apply {
            writeText("pdf")
            setLastModified(now - ageMillis)
        }

    @Test
    fun keepsNewestFilesAndPrunesOnlyOldOnes() {
        val dir = temporaryFolder.newFolder()
        val files = (1..30).map { index ->
            // index 1 is the newest (1 hour old), index 30 the oldest (30 days old).
            pdf("Invoice_$index", index * oneDay / 2, dir)
        }

        val pruned = PdfCacheManager.filesToPrune(files, now, maxFiles = 20, maxAgeMillis = 7 * oneDay)

        assertEquals(10, pruned.size)
        // Everything removed must be older than the retention window.
        assertTrue(pruned.all { now - it.lastModified() > 7 * oneDay })
        assertTrue(pruned.none { it.name in files.take(20).map { f -> f.name } })
    }

    @Test
    fun neverPrunesFreshFilesEvenWhenTheDirectoryIsLarge() {
        val dir = temporaryFolder.newFolder()
        val files = (1..100).map { index -> pdf("Invoice_$index", ageMillis = 60_000L, dir) }

        val pruned = PdfCacheManager.filesToPrune(files, now, maxFiles = 20, maxAgeMillis = 7 * oneDay)

        assertTrue("Fresh documents must survive bulk pruning", pruned.isEmpty())
    }

    @Test
    fun ignoresNonPdfFilesAndDirectories() {
        val dir = temporaryFolder.newFolder()
        val stale = pdf("Invoice_old", 30 * oneDay, dir)
        val textFile = File(dir, "notes.txt").apply {
            writeText("keep me")
            setLastModified(now - 90 * oneDay)
        }
        val nested = File(dir, "subdir").apply { mkdirs() }

        val pruned = PdfCacheManager.filesToPrune(listOf(stale, textFile, nested), now, maxFiles = 0, maxAgeMillis = oneDay)

        assertEquals(listOf(stale), pruned)
        assertTrue(textFile.exists())
        assertTrue(nested.exists())
    }

    @Test
    fun pruneDeletesOnlyStalePdfsOnDisk() {
        val dir = temporaryFolder.newFolder()
        // More files than the retention window keeps, so the oldest ones become eligible.
        val stale = (1..5).map { pdf("Invoice_stale_$it", 30 * oneDay, dir) }
        val fresh = (1..20).map { pdf("Invoice_fresh_$it", 60_000L, dir) }
        val textFile = File(dir, "keep.txt").apply { writeText("x") }

        PdfCacheManager.prune(dir, now)

        assertTrue("stale pdfs should be evicted", stale.none { it.exists() })
        assertTrue("fresh pdfs should be kept", fresh.all { it.exists() })
        assertTrue("unrelated files should be kept", textFile.exists())
    }

    @Test
    fun smallCacheIsNeverPruned() {
        val dir = temporaryFolder.newFolder()
        val oldButFew = pdf("Invoice_only_one", 365 * oneDay, dir)

        PdfCacheManager.prune(dir, now)

        assertTrue(
            "A cache below the file-count limit is already bounded and must not be touched",
            oldButFew.exists()
        )
    }

    @Test
    fun emptyInputIsHandled() {
        assertTrue(PdfCacheManager.filesToPrune(emptyList(), now).isEmpty())
        assertTrue(PdfCacheManager.filesToPrune(listOf(), now, maxFiles = 0).isEmpty())
    }
}

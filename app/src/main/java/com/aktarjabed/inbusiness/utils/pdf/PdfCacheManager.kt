package com.aktarjabed.inbusiness.utils.pdf

import java.io.File

/**
 * Keeps the generated-invoice cache directory bounded.
 *
 * Every "Share PDF" tap writes a new file named
 * `Invoice_<number>_<timestamp>.pdf` into `cacheDir/invoices`; without pruning the
 * directory grows forever on a long-lived install.
 *
 * The policy is deliberately conservative — it can only ever delete `*.pdf` files from
 * the dedicated cache folder, and it always protects:
 *
 *  - the newest [PdfConstants.MAX_CACHED_PDFS] documents (never delete what the user
 *    most likely just shared), and
 *  - everything younger than [PdfConstants.PDF_CACHE_MAX_AGE_MILLIS].
 *
 * The selection logic is a pure function so it can be unit tested without Android.
 */
object PdfCacheManager {

    fun filesToPrune(
        files: List<File>,
        nowMillis: Long,
        maxFiles: Int = PdfConstants.MAX_CACHED_PDFS,
        maxAgeMillis: Long = PdfConstants.PDF_CACHE_MAX_AGE_MILLIS
    ): List<File> {
        val candidates = files.filter { it.isFile && it.name.endsWith(".pdf", ignoreCase = true) }
        if (candidates.isEmpty()) return emptyList()

        val keep = candidates
            .sortedByDescending { it.lastModified() }
            .take(maxFiles.coerceAtLeast(0))
            .toSet()

        return candidates
            .filterNot { it in keep }
            .filter { nowMillis - it.lastModified() > maxAgeMillis }
    }

    /** Prunes [directory]; safe to call before every write. */
    fun prune(directory: File, nowMillis: Long = System.currentTimeMillis()) {
        val files = directory.listFiles() ?: return
        filesToPrune(files.toList(), nowMillis).forEach { stale ->
            // Best effort: a file that is still shared with another app may fail to delete.
            runCatching { stale.delete() }
        }
    }
}

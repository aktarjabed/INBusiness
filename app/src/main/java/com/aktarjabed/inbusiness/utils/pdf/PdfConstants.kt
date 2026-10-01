package com.aktarjabed.inbusiness.utils.pdf

object PdfConstants {
    const val DEALS_IN = "Deals in: Seeds, Fertilizers, and other Agro Products"
    const val BUSINESS_NAME_SHORT = "J.A. Agro Inputs & Trading"

    /** Generated PDFs are cache artifacts; only the newest [MAX_CACHED_PDFS] are kept. */
    const val MAX_CACHED_PDFS = 20

    /** Generated PDFs older than this are pruned (milliseconds). */
    const val PDF_CACHE_MAX_AGE_MILLIS = 7L * 24 * 60 * 60 * 1000

    /**
     * Hard limit on rendered pages. `PdfDocument` keeps every page in memory, so a
     * pathological (or accidentally pasted) multi-kilobyte item description must not be
     * able to exhaust the heap. Content past the limit is truncated, not reflowed.
     */
    const val MAX_PAGES = 50

    /** Maximum characters of an item description actually rendered into the PDF. */
    const val MAX_DESCRIPTION_CHARS = 2000
}

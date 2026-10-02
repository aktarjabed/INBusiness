package com.aktarjabed.inbusiness.domain.invoice

import com.aktarjabed.inbusiness.data.dao.InvoiceItemSuggestion

/**
 * Identity of a line for "what did this product last sell for" purposes.
 *
 * Catalog lines are keyed by `productId`. Ad-hoc lines (no linked product) are keyed by
 * their normalized description, matching how product suggestions group them. The two
 * namespaces are prefixed so a description that happens to look like an id ("5") can never
 * collide with product 5.
 */
fun InvoiceItemSuggestion.historyKey(): String =
    productId?.let { "id:$it" } ?: "desc:${description.trim().lowercase()}"

/**
 * Collapses invoice history to the most recent line per [historyKey].
 *
 * The input must be ordered newest-first (see `InvoiceDao.getHistoricalInvoiceItems`):
 * the first occurrence of a key wins, which is what makes this equivalent to the previous
 * "latest id per product" SQL subquery.
 */
fun List<InvoiceItemSuggestion>.collapseHistoryToLatest(): List<InvoiceItemSuggestion> =
    distinctBy { it.historyKey() }

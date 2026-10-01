package com.aktarjabed.inbusiness.domain.usecase

import com.aktarjabed.inbusiness.data.entities.Invoice
import com.aktarjabed.inbusiness.data.entities.InvoiceItem
import com.aktarjabed.inbusiness.data.repository.InvoiceRepository
import javax.inject.Inject

/**
 * Loads an invoice snapshot together with its line items for display.
 *
 * Business isolation is enforced inside [InvoiceRepository]: every lookup is scoped to
 * the active business id, so an invoice id from another business resolves to "not found".
 */
class GetInvoiceForPreviewUseCase @Inject constructor(
    private val invoiceRepository: InvoiceRepository
) {
    suspend operator fun invoke(invoiceId: String): PreviewResult {
        val invoice = invoiceRepository.getInvoiceById(invoiceId)
            ?: return PreviewResult.Error("Invoice not found or access denied")

        val items = invoiceRepository.getInvoiceItems(invoiceId)

        return PreviewResult.Success(invoice, items)
    }
}

sealed class PreviewResult {
    data class Success(val invoice: Invoice, val items: List<InvoiceItem>) : PreviewResult()
    data class Error(val message: String) : PreviewResult()
}

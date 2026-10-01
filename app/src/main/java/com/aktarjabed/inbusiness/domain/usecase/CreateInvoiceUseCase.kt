package com.aktarjabed.inbusiness.domain.usecase

import com.aktarjabed.inbusiness.data.entities.InvoiceItem
import com.aktarjabed.inbusiness.data.repository.InvoiceRepository
import com.aktarjabed.inbusiness.domain.invoice.InvoiceCreationResult
import com.aktarjabed.inbusiness.domain.invoice.SupplyType
import javax.inject.Inject

/**
 * Thin entry point for invoice creation.
 *
 * Validation, quota consumption, sequence allocation, stock deduction, payment
 * recording and idempotency all happen atomically inside
 * [InvoiceRepository.createInvoice]; this class only keeps the presentation layer
 * decoupled from the repository.
 */
class CreateInvoiceUseCase @Inject constructor(
    private val invoiceRepository: InvoiceRepository
) {
    suspend operator fun invoke(
        customerName: String,
        customerGSTIN: String?,
        buyerAddress: String,
        supplyType: SupplyType,
        items: List<InvoiceItem>,
        idempotencyKey: String? = null,
        amountPaid: Double = 0.0,
        paymentMethod: String = "NONE"
    ): InvoiceCreationResult {
        return invoiceRepository.createInvoice(
            customerName = customerName,
            customerGSTIN = customerGSTIN,
            buyerAddress = buyerAddress,
            supplyType = supplyType,
            items = items,
            idempotencyKey = idempotencyKey,
            amountPaid = amountPaid,
            paymentMethod = paymentMethod
        )
    }
}

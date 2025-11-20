package com.aktarjabed.inbusiness.domain.usecase

import com.aktarjabed.inbusiness.data.entities.Invoice
import com.aktarjabed.inbusiness.data.entities.InvoiceItem
import com.aktarjabed.inbusiness.data.remote.NicClient
import com.aktarjabed.inbusiness.data.remote.models.IRNData
import com.aktarjabed.inbusiness.di.IoDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Use case for generating IRN using NIC e-invoicing API
 */
class GenerateIRNUseCase @Inject constructor(
    private val nicClient: NicClient,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
    suspend operator fun invoke(
        invoice: Invoice,
        items: List<InvoiceItem>
    ): Result<IRNData> = withContext(ioDispatcher) {
        try {
            nicClient.generateIRN(invoice, items)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
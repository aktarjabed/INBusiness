package com.aktarjabed.inbusiness.domain.invoice

import com.aktarjabed.inbusiness.data.entities.InvoiceItem
import org.junit.Assert.assertEquals
import org.junit.Test

class CalculateInvoiceTotalsUseCaseTest {
    private val useCase = CalculateInvoiceTotalsUseCase()

    @Test
    fun roundsInitialPaymentToCurrencyPrecision() {
        val result = useCase(
            items = listOf(
                InvoiceItem(
                    description = "Seed",
                    quantity = 1.0,
                    pricePerUnit = 100.0,
                    gstPercentage = 0.0
                )
            ),
            supplyType = SupplyType.INTRA_STATE,
            amountPaid = 0.005
        )

        assertEquals(0.01, result.amountPaid, 0.0001)
        assertEquals(99.99, result.balanceDue, 0.0001)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsSubCentInitialPaymentThatRoundsToZero() {
        useCase(
            items = listOf(
                InvoiceItem(
                    description = "Seed",
                    quantity = 1.0,
                    pricePerUnit = 100.0,
                    gstPercentage = 0.0
                )
            ),
            supplyType = SupplyType.INTRA_STATE,
            amountPaid = 0.004
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsNonFiniteInitialPayment() {
        useCase(
            items = listOf(
                InvoiceItem(
                    description = "Seed",
                    quantity = 1.0,
                    pricePerUnit = 100.0,
                    gstPercentage = 0.0
                )
            ),
            supplyType = SupplyType.INTRA_STATE,
            amountPaid = Double.NaN
        )
    }
}

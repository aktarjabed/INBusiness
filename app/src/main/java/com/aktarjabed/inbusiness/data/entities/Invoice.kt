package com.aktarjabed.inbusiness.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.aktarjabed.inbusiness.data.converters.Converters
import java.time.Instant
import java.util.UUID

enum class InvoiceType { TAX_INVOICE, PROFORMA, CREDIT_NOTE }
enum class InvoiceStatus { DRAFT, SENT, PAID, CANCELLED, OVERDUE }
enum class PaymentMode { CASH, CARD, BANK_TRANSFER, UPI }

@Entity(tableName = "invoices")
@TypeConverters(Converters::class)
data class Invoice(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val invoiceNumber: String = "",
    val invoiceType: InvoiceType = InvoiceType.TAX_INVOICE,
    val status: InvoiceStatus = InvoiceStatus.DRAFT,

    val customerName: String = "",
    val customerGstin: String = "",
    val customerAddress: String = "",
    val customerPhone: String = "",
    val customerEmail: String = "",

    val supplierName: String = "My Business",
    val supplierGstin: String = "",
    val supplierAddress: String = "",
    val supplierPhone: String = "",
    val supplierEmail: String = "",

    val subtotal: Double = 0.0,
    val cgstAmount: Double = 0.0,
    val sgstAmount: Double = 0.0,
    val igstAmount: Double = 0.0,
    val cessAmount: Double = 0.0,
    val totalAmount: Double = 0.0,
    val roundOff: Double = 0.0,

    val paidAmount: Double = 0.0,
    val balanceAmount: Double = 0.0,
    val paymentMode: PaymentMode? = null,
    val paymentDate: Instant? = null,

    val irn: String? = null,
    val ackNo: String? = null,
    val ackDate: Instant? = null,
    val qrCodeData: String? = null,
    val eInvoiceStatus: String? = null,

    val invoiceDate: Instant = Instant.now(),
    val dueDate: Instant = Instant.now().plusSeconds(30L * 24 * 60 * 60),
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now(),
    val createdBy: String = "",
    val notes: String = "",
    val termsAndConditions: String = ""
)

@Entity(tableName = "invoice_items")
data class InvoiceItem(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val invoiceId: String?,
    val itemName: String,
    val hsnCode: String,
    val quantity: Double,
    val rate: Double,
    val totalAmount: Double
)
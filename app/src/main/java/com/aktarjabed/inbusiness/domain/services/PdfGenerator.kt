package com.aktarjabed.inbusiness.domain.services

import android.content.Context
import com.aktarjabed.inbusiness.data.entities.Invoice
import com.aktarjabed.inbusiness.data.entities.InvoiceItem
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Cell
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Table
import com.itextpdf.layout.properties.TextAlignment
import com.itextpdf.layout.properties.UnitValue
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PdfGenerator @Inject constructor(@ApplicationContext private val context: Context) {

    fun generateInvoicePdf(invoice: Invoice, items: List<InvoiceItem>): File {
        val pdfFile = File(context.getExternalFilesDir("invoices"), "invoice_${invoice.invoiceNumber}.pdf")
        val writer = PdfWriter(pdfFile)
        val pdf = com.itextpdf.kernel.pdf.PdfDocument(writer)
        val document = Document(pdf)

        // Title
        document.add(Paragraph("Tax Invoice").setTextAlignment(TextAlignment.CENTER).setBold().setFontSize(20f))

        // Supplier & Customer Details
        val detailsTable = Table(UnitValue.createPercentArray(floatArrayOf(1f, 1f))).useAllAvailableWidth()
        detailsTable.addCell(createCell("Supplier:", bold = true))
        detailsTable.addCell(createCell("Customer:", bold = true))
        detailsTable.addCell(createCell(invoice.supplierName))
        detailsTable.addCell(createCell(invoice.customerName))
        detailsTable.addCell(createCell("GSTIN: ${invoice.supplierGstin}"))
        detailsTable.addCell(createCell("GSTIN: ${invoice.customerGstin}"))
        document.add(detailsTable)

        document.add(Paragraph("\n"))

        // Items Table
        val itemsTable = Table(UnitValue.createPercentArray(floatArrayOf(3f, 1f, 1f, 1f, 1f))).useAllAvailableWidth()
        itemsTable.addHeaderCell(createCell("Item", bold = true))
        itemsTable.addHeaderCell(createCell("HSN", bold = true))
        itemsTable.addHeaderCell(createCell("Qty", bold = true))
        itemsTable.addHeaderCell(createCell("Rate", bold = true))
        itemsTable.addHeaderCell(createCell("Amount", bold = true))

        items.forEach { item ->
            itemsTable.addCell(createCell(item.itemName))
            itemsTable.addCell(createCell(item.hsnCode))
            itemsTable.addCell(createCell(item.quantity.toString()))
            itemsTable.addCell(createCell(item.rate.toString()))
            itemsTable.addCell(createCell(item.totalAmount.toString()))
        }
        document.add(itemsTable)

        document.add(Paragraph("\n"))

        // Totals Table
        val totalsTable = Table(UnitValue.createPercentArray(floatArrayOf(1f, 1f))).useAllAvailableWidth()
        totalsTable.addCell(createCell("Subtotal:"))
        totalsTable.addCell(createCell(invoice.subtotal.toString(), alignment = TextAlignment.RIGHT))
        if (invoice.cgstAmount > 0) {
            totalsTable.addCell(createCell("CGST:"))
            totalsTable.addCell(createCell(invoice.cgstAmount.toString(), alignment = TextAlignment.RIGHT))
        }
        if (invoice.sgstAmount > 0) {
            totalsTable.addCell(createCell("SGST:"))
            totalsTable.addCell(createCell(invoice.sgstAmount.toString(), alignment = TextAlignment.RIGHT))
        }
        if (invoice.igstAmount > 0) {
            totalsTable.addCell(createCell("IGST:"))
            totalsTable.addCell(createCell(invoice.igstAmount.toString(), alignment = TextAlignment.RIGHT))
        }
        totalsTable.addCell(createCell("Total:", bold = true))
        totalsTable.addCell(createCell(invoice.totalAmount.toString(), bold = true, alignment = TextAlignment.RIGHT))
        document.add(totalsTable)

        document.close()
        return pdfFile
    }

    private fun createCell(text: String, bold: Boolean = false, alignment: TextAlignment = TextAlignment.LEFT): Cell {
        val cell = Cell().add(Paragraph(text))
        cell.setTextAlignment(alignment)
        if (bold) cell.setBold()
        return cell
    }
}
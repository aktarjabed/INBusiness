package com.aktarjabed.inbusiness.data.local.database

import androidx.room.TypeConverter
import com.aktarjabed.inbusiness.data.local.entities.Invoice
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object Converters {
    private val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

    @TypeConverter
    fun fromLocalDateTime(dateTime: LocalDateTime?): String? {
        return dateTime?.format(formatter)
    }

    @TypeConverter
    fun toLocalDateTime(dateTimeString: String?): LocalDateTime? {
        return dateTimeString?.let { LocalDateTime.parse(it, formatter) }
    }

    @TypeConverter
    fun fromInvoiceStatus(status: Invoice.InvoiceStatus): String {
        return status.name
    }

    @TypeConverter
    fun toInvoiceStatus(status: String): Invoice.InvoiceStatus {
        return Invoice.InvoiceStatus.valueOf(status)
    }
}
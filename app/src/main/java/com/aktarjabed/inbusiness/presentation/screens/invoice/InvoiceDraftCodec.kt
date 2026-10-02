package com.aktarjabed.inbusiness.presentation.screens.invoice

/**
 * Serialises the in-progress invoice items into a single `String` so the draft survives process
 * death via `SavedStateHandle`.
 *
 * `SavedStateHandle` only accepts Bundle-compatible values, and [InvoiceItemInput] is a plain data
 * class, so it cannot be stored directly. This is a deliberately small, dependency-free codec
 * instead of `@Parcelize`/kotlinx-serialization: it needs no build change and, unlike a
 * delimiter-joined format, it cannot be corrupted by user text — descriptions and unit types are
 * length-prefixed, so semicolons, colons, newlines and emoji in a product name round-trip exactly.
 *
 * Format (version 1):
 * ```
 * 1; <count>; (<len>:<description> <len>:<unitType> <qty>; <price>; <gst>; <productId|-> ;)…
 * ```
 * Numbers are terminated by `;`; strings are length-prefixed and therefore carry no terminator.
 *
 * [decode] never throws: a truncated, corrupt or future-versioned draft is reported as "no draft",
 * because losing a half-typed form is strictly better than crashing the invoice screen on launch.
 */
object InvoiceDraftCodec {

    private const val VERSION = 1
    private const val FIELD_TERMINATOR = ';'
    private const val LENGTH_SEPARATOR = ':'
    private const val NULL_PRODUCT = "-"

    /** Sanity bound on a decoded draft; anything above this is treated as corruption. */
    private const val MAX_ITEMS = 500

    fun encode(items: List<InvoiceItemInput>): String {
        val builder = StringBuilder()
        builder.append(VERSION).append(FIELD_TERMINATOR)
        builder.append(items.size).append(FIELD_TERMINATOR)
        for (item in items) {
            appendString(builder, item.description)
            appendString(builder, item.unitType)
            builder.append(item.quantity).append(FIELD_TERMINATOR)
            builder.append(item.pricePerUnit).append(FIELD_TERMINATOR)
            builder.append(item.gstPercentage).append(FIELD_TERMINATOR)
            builder.append(item.productId?.toString() ?: NULL_PRODUCT).append(FIELD_TERMINATOR)
        }
        return builder.toString()
    }

    fun decode(encoded: String?): List<InvoiceItemInput> {
        if (encoded.isNullOrEmpty()) return emptyList()
        return runCatching { parse(encoded) }.getOrElse { emptyList() }
    }

    private fun appendString(builder: StringBuilder, value: String) {
        builder.append(value.length).append(LENGTH_SEPARATOR).append(value)
    }

    private fun parse(encoded: String): List<InvoiceItemInput> {
        val reader = Reader(encoded)
        // An unknown version (older or future format) is discarded rather than misread.
        if (reader.readInt() != VERSION) return emptyList()

        val count = reader.readInt()
        require(count in 0..MAX_ITEMS) { "implausible draft item count: $count" }

        return List(count) {
            InvoiceItemInput(
                description = reader.readString(),
                unitType = reader.readString(),
                quantity = reader.readDouble(),
                pricePerUnit = reader.readDouble(),
                gstPercentage = reader.readDouble(),
                productId = reader.readNullableLong()
            )
        }
    }

    private class Reader(private val source: String) {
        private var index = 0

        fun readInt(): Int {
            val raw = readUntil(FIELD_TERMINATOR)
            return raw.toIntOrNull() ?: throw IllegalArgumentException("not an int: '$raw'")
        }

        fun readDouble(): Double {
            val raw = readUntil(FIELD_TERMINATOR)
            return raw.toDoubleOrNull() ?: throw IllegalArgumentException("not a double: '$raw'")
        }

        fun readNullableLong(): Long? {
            val raw = readUntil(FIELD_TERMINATOR)
            if (raw == NULL_PRODUCT) return null
            return raw.toLongOrNull() ?: throw IllegalArgumentException("not a long: '$raw'")
        }

        fun readString(): String {
            val rawLength = readUntil(LENGTH_SEPARATOR)
            val length = rawLength.toIntOrNull() ?: throw IllegalArgumentException("not a length: '$rawLength'")
            require(length >= 0 && index + length <= source.length) { "string exceeds the draft" }
            val value = source.substring(index, index + length)
            index += length
            return value
        }

        private fun readUntil(terminator: Char): String {
            val end = source.indexOf(terminator, index)
            require(end >= 0) { "missing '$terminator' at $index" }
            val raw = source.substring(index, end)
            index = end + 1
            return raw
        }
    }
}

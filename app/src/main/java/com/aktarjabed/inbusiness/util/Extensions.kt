package com.aktarjabed.inbusiness.util

import android.content.Context
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

// Date/Time Extensions
fun Instant.toLocalDate(): String {
    val formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy")
        .withZone(ZoneId.systemDefault())
    return formatter.format(this)
}

fun Instant.toLocalDateTime(): String {
    val formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm")
        .withZone(ZoneId.systemDefault())
    return formatter.format(this)
}

fun Instant.toNicFormat(): String {
    val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
        .withZone(ZoneId.systemDefault())
    return formatter.format(this)
}

// Currency Extensions
fun Double.toRupees(): String = "₹${"%.2f".format(Locale.US, this)}"

fun Double.toRupeesWhole(): String = "₹${this.toInt()}"

fun String.toDoubleOrZero(): Double = this.toDoubleOrNull() ?: 0.0

// String Extensions
fun String.isValidGSTIN(): Boolean {
    if (this.length != 15) return false

    // GSTIN format: 22AAAAA0000A1Z5
    val gstinRegex = """^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[1-9A-Z]{1}Z[0-9A-Z]{1}$""".toRegex()
    return this.matches(gstinRegex)
}

fun String.extractStateCode(): String = this.take(2)

fun String.maskSensitive(): String {
    if (this.length < 4) return "****"
    return this.take(2) + "*".repeat(this.length - 4) + this.takeLast(2)
}

// Context Extensions
fun Context.toast(message: String, duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(this, message, duration).show()
}

@Composable
fun rememberToast(): (String) -> Unit {
    val context = LocalContext.current
    return { message -> context.toast(message) }
}

// Number Extensions
fun Int.isEven(): Boolean = this % 2 == 0

fun Int.isOdd(): Boolean = this % 2 != 0

// Collection Extensions
fun <T> List<T>.second(): T? = this.getOrNull(1)

fun <T> List<T>.secondOrNull(): T? = this.getOrNull(1)

// Validation Extensions
fun String.isValidEmail(): Boolean {
    val emailRegex = """^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Z|a-z]{2,}$""".toRegex()
    return this.matches(emailRegex)
}

fun String.isValidPhone(): Boolean {
    val phoneRegex = """^[6-9]\d{9}$""".toRegex()
    return this.matches(phoneRegex)
}

fun String.isValidPincode(): Boolean {
    val pincodeRegex = """^\d{6}$""".toRegex()
    return this.matches(pincodeRegex)
}

// HSN Code validation
fun String.isValidHSN(): Boolean {
    // HSN can be 4, 6, or 8 digits
    return this.matches("""^\d{4}$""".toRegex()) ||
           this.matches("""^\d{6}$""".toRegex()) ||
           this.matches("""^\d{8}$""".toRegex())
}
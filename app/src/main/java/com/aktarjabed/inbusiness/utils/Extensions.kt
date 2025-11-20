package com.aktarjabed.inbusiness.utils

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

fun Instant.toLocalDate(): String =
    DateTimeFormatter.ofPattern("dd-MM-yyyy")
        .withZone(ZoneId.systemDefault())
        .format(this)

fun Double.toRupees(): String = "₹%.2f".format(this)
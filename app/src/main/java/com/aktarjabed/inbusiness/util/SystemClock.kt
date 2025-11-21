package com.aktarjabed.inbusiness.util

import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
open class SystemClock @Inject constructor() {

    open fun todayEpochDay(): Long = LocalDate.now().toEpochDay()

    open fun today(): LocalDate = LocalDate.now()

    open fun monthStartEpochDay(): Long {
        val today = LocalDate.now()
        return today.withDayOfMonth(1).toEpochDay()
    }

    open fun nowMillis(): Long = System.currentTimeMillis()
}
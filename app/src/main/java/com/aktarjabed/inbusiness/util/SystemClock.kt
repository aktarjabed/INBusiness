package com.aktarjabed.inbusiness.util

import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
open class SystemClock @Inject constructor() {
    private val clock: Clock = Clock.systemUTC()

    fun todayEpochDay(): Long = LocalDate.now(clock).toEpochDay()

    fun today(): LocalDate = LocalDate.now(clock)

    fun monthStartEpochDay(): Long {
        val today = LocalDate.now(clock)
        return today.withDayOfMonth(1).toEpochDay()
    }

    fun nowMillis(): Long = clock.millis()
}

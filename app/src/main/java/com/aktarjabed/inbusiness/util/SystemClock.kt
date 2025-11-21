package com.aktarjabed.inbusiness.util

import java.time.LocalDate
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SystemClock @Inject constructor() {
    fun now(): LocalDateTime = LocalDateTime.now()
    fun today(): LocalDate = LocalDate.now()
    fun todayEpochDay(): Long = LocalDate.now().toEpochDay()
    fun monthStartEpochDay(): Long = LocalDate.now().withDayOfMonth(1).toEpochDay()
}
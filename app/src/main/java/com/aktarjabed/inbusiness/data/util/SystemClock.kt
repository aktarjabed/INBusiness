package com.aktarjabed.inbusiness.data.util

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SystemClock @Inject constructor() {

    fun now(): LocalDateTime = LocalDateTime.now()

    fun today(): LocalDate = LocalDate.now()

    fun todayEpochDay(): Long = LocalDate.now().toEpochDay()

    fun monthStartEpochDay(): Long =
        LocalDate.now().withDayOfMonth(1).toEpochDay()

    fun currentTimeMillis(): Long = System.currentTimeMillis()

    fun toEpochMilli(dateTime: LocalDateTime): Long =
        dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
}
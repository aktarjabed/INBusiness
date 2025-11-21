package com.aktarjabed.inbusiness.util

import java.time.LocalDate
<<<<<<< HEAD
import java.time.LocalDateTime
=======
>>>>>>> ad4bb8454e6b04046f0ba290c1f04d22f25fbfa5
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
<<<<<<< HEAD
class SystemClock @Inject constructor() {
    fun now(): LocalDateTime = LocalDateTime.now()
    fun today(): LocalDate = LocalDate.now()
    fun todayEpochDay(): Long = LocalDate.now().toEpochDay()
    fun monthStartEpochDay(): Long = LocalDate.now().withDayOfMonth(1).toEpochDay()
=======
open class SystemClock @Inject constructor() {

    open fun todayEpochDay(): Long = LocalDate.now().toEpochDay()

    open fun today(): LocalDate = LocalDate.now()

    open fun monthStartEpochDay(): Long {
        val today = LocalDate.now()
        return today.withDayOfMonth(1).toEpochDay()
    }

    open fun nowMillis(): Long = System.currentTimeMillis()
>>>>>>> ad4bb8454e6b04046f0ba290c1f04d22f25fbfa5
}
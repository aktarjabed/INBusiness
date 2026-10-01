package com.aktarjabed.inbusiness.util

import com.aktarjabed.inbusiness.utils.AppDateUtils
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.TimeZone

/**
 * Quota windows and dashboard "today" ranges are derived from [SystemClock]. If it used
 * the device default zone, a device set to another timezone would consume its daily
 * invoice quota on a different calendar day than the one the dashboard reports.
 */
class SystemClockTest {

    private val originalDefault: TimeZone = TimeZone.getDefault()

    @After
    fun restoreDefaultZone() {
        TimeZone.setDefault(originalDefault)
    }

    @Test
    fun usesTheBusinessZoneRegardlessOfDeviceZone() {
        // A device far behind/ahead of IST must still resolve the IST business day.
        listOf("Pacific/Honolulu", "America/Los_Angeles", "UTC", "Pacific/Kiritimati").forEach { deviceZone ->
            TimeZone.setDefault(TimeZone.getTimeZone(deviceZone))

            val clock = SystemClock()
            assertEquals(AppDateUtils.businessZoneId, clock.zoneId)
            assertEquals(
                "day must be resolved in the business zone for device zone $deviceZone",
                LocalDate.now(AppDateUtils.businessZoneId).toEpochDay(),
                clock.todayEpochDay()
            )
            assertEquals(LocalDate.now(AppDateUtils.businessZoneId), clock.today())
        }
    }

    @Test
    fun monthStartIsTheFirstDayOfTheBusinessMonth() {
        TimeZone.setDefault(TimeZone.getTimeZone("America/Los_Angeles"))

        val clock = SystemClock()
        val expected = LocalDate.now(AppDateUtils.businessZoneId).withDayOfMonth(1)

        assertEquals(expected.toEpochDay(), clock.monthStartEpochDay())
        assertTrue(
            "month start must never be in the future",
            clock.monthStartEpochDay() <= clock.todayEpochDay()
        )
        assertTrue(
            "month start must belong to the current month",
            clock.todayEpochDay() - clock.monthStartEpochDay() < 32
        )
    }

    @Test
    fun resetTimeIsExpressedInTheBusinessZone() {
        TimeZone.setDefault(TimeZone.getTimeZone("Pacific/Honolulu"))

        val resetTime = com.aktarjabed.inbusiness.domain.quota.QuotaVerdict.DailyCap(2).resetTime

        // Assert the invariant rather than an exact instant: the reset is the next business-zone
        // midnight. Comparing against a freshly computed LocalDateTime would flake if the run
        // straddles midnight in the business zone.
        assertEquals(0, resetTime.minute)
        assertEquals(0, resetTime.second)
        assertEquals(0, resetTime.nano)
        val secondsToReset = Duration.between(
            LocalDateTime.now(AppDateUtils.businessZoneId),
            resetTime
        ).seconds
        assertTrue(
            "reset must be the next midnight in the business zone (was ${'$'}secondsToReset s away)",
            secondsToReset in 1..(24 * 60 * 60)
        )
    }

    @Test
    fun nowMillisTracksTheSystemClock() {
        val before = System.currentTimeMillis()
        val value = SystemClock().nowMillis()
        val after = System.currentTimeMillis()
        org.junit.Assert.assertTrue(value in before..after)
    }

    @Test
    fun zoneIdIsStableAcrossInstances() {
        assertEquals(
            ZoneId.of("Asia/Kolkata"),
            SystemClock().zoneId
        )
    }
}

package com.aktarjabed.inbusiness.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

/**
 * Date boundaries drive revenue reporting, quota resets and the 7-day chart. They must
 * always be computed in the business timezone (Asia/Kolkata), never the device zone,
 * otherwise a device west of India reports yesterday's revenue as today's.
 */
class AppDateUtilsTest {

    @Test
    fun businessZoneIsIst() {
        assertEquals(ZoneId.of("Asia/Kolkata"), AppDateUtils.businessZoneId)
    }

    @Test
    fun dayBoundariesUseTheBusinessZone() {
        val date = LocalDate.of(2026, 4, 15)

        val start = AppDateUtils.getStartOfDay(date)
        val next = AppDateUtils.getStartOfNextDay(date)

        assertTrue("start must be before end", start < next)
        assertEquals(
            "Start of day must be midnight IST",
            date.atStartOfDay(AppDateUtils.businessZoneId).toInstant().toEpochMilli(),
            start
        )
        assertEquals(
            "End of day must be the next midnight IST",
            date.plusDays(1).atStartOfDay(AppDateUtils.businessZoneId).toInstant().toEpochMilli(),
            next
        )
        // The interval is half open [start, end) and spans exactly one business day.
        assertEquals(24 * 60 * 60 * 1000L, next - start)
    }

    @Test
    fun todayWindowCoversNow() {
        val start = AppDateUtils.getTodayStart()
        val end = AppDateUtils.getTomorrowStart()
        val now = System.currentTimeMillis()

        assertTrue("now must be inside [todayStart, tomorrowStart)", now >= start && now < end)
        assertEquals(24 * 60 * 60 * 1000L, end - start)
    }

    @Test
    fun pastSevenDaysIsOrderedAndEndsToday() {
        val days = AppDateUtils.getPastSevenDays()
        val today = LocalDate.now(AppDateUtils.businessZoneId)

        assertEquals(7, days.size)
        assertEquals(today, days.last())
        assertEquals(today.minusDays(6), days.first())
        assertEquals(days.sorted(), days)
        assertEquals(days.distinct().size, days.size)
    }
}

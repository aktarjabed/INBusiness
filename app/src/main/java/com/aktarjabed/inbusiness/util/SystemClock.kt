package com.aktarjabed.inbusiness.util

import com.aktarjabed.inbusiness.utils.AppDateUtils
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Clock used for every business-day decision (quota windows, "today" boundaries).
 *
 * All values are resolved in the authoritative [AppDateUtils.businessZoneId]
 * (Asia/Kolkata), never in the device default zone. Quota days, dashboard
 * windows and PDF dates must agree, otherwise a device travelling across
 * timezones would see quota resets and revenue totals from different calendar
 * days.
 */
@Singleton
open class SystemClock @Inject constructor() {

    open val zoneId: ZoneId get() = AppDateUtils.businessZoneId

    open fun todayEpochDay(): Long = today().toEpochDay()

    open fun today(): LocalDate = LocalDate.now(zoneId)

    open fun monthStartEpochDay(): Long {
        val today = today()
        return today.withDayOfMonth(1).toEpochDay()
    }

    open fun nowMillis(): Long = System.currentTimeMillis()
}

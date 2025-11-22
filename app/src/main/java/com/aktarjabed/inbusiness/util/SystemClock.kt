package com.aktarjabed.inbusiness.util

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Wrapper around Android's SystemClock for testability
 */
@Singleton
class SystemClock @Inject constructor() {

    /**
     * Returns milliseconds since boot, including time spent in sleep
     */
    fun elapsedRealtime(): Long {
        return android.os.SystemClock.elapsedRealtime()
    }

    /**
     * Returns milliseconds running in the current thread
     */
    fun currentThreadTimeMillis(): Long {
        return android.os.SystemClock.currentThreadTimeMillis()
    }

    /**
     * Returns nanoseconds since boot, including time spent in sleep
     */
    fun elapsedRealtimeNanos(): Long {
        return android.os.SystemClock.elapsedRealtimeNanos()
    }

    /**
     * Waits a given number of milliseconds (of uptimeMillis) before returning
     */
    fun sleep(ms: Long) {
        android.os.SystemClock.sleep(ms)
    }

    /**
     * Returns milliseconds since boot, not counting time spent in deep sleep
     */
    fun uptimeMillis(): Long {
        return android.os.SystemClock.uptimeMillis()
    }

    /**
     * Returns current system time in milliseconds
     */
    fun currentTimeMillis(): Long {
        return System.currentTimeMillis()
    }
}
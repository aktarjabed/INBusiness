package com.aktarjabed.inbusiness.util

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeviceClassifier @Inject constructor() {

    fun getDeviceTier(context: Context): DeviceTier {
        val ramMB = getTotalRAM(context)
        val cpuCores = Runtime.getRuntime().availableProcessors()

        return when {
            ramMB >= 6144 && cpuCores >= 8 -> DeviceTier.HIGH_END
            ramMB >= 4096 && cpuCores >= 6 -> DeviceTier.MID_RANGE
            else -> DeviceTier.LOW_END
        }
    }

    private fun getTotalRAM(context: Context): Long {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        return memoryInfo.totalMem / (1024 * 1024)
    }
}

enum class DeviceTier {
    LOW_END, MID_RANGE, HIGH_END
}
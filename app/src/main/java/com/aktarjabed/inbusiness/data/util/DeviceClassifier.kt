package com.aktarjabed.inbusiness.data.util

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeviceClassifier @Inject constructor() {

    fun getDeviceTier(context: Context): DeviceTier {
        return try {
            val ramMB = getTotalRAM(context)
            val cpuCores = Runtime.getRuntime().availableProcessors()
            val sdkInt = Build.VERSION.SDK_INT

            when {
                ramMB >= 6144 && cpuCores >= 8 && sdkInt >= 31 -> DeviceTier.HIGH_END
                ramMB >= 4096 && cpuCores >= 6 -> DeviceTier.MID_RANGE
                else -> DeviceTier.LOW_END
            }
        } catch (e: Exception) {
            Timber.e(e, "Error classifying device")
            DeviceTier.MID_RANGE // Safe default
        }
    }

    private fun getTotalRAM(context: Context): Long {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        return memoryInfo.totalMem / (1024 * 1024) // Convert to MB
    }

    fun getDeviceInfo(context: Context): DeviceInfo {
        return DeviceInfo(
            manufacturer = Build.MANUFACTURER,
            model = Build.MODEL,
            androidVersion = Build.VERSION.RELEASE,
            sdkInt = Build.VERSION.SDK_INT,
            ramMB = getTotalRAM(context),
            cpuCores = Runtime.getRuntime().availableProcessors(),
            tier = getDeviceTier(context)
        )
    }
}

enum class DeviceTier {
    LOW_END,
    MID_RANGE,
    HIGH_END
}

data class DeviceInfo(
    val manufacturer: String,
    val model: String,
    val androidVersion: String,
    val sdkInt: Int,
    val ramMB: Long,
    val cpuCores: Int,
    val tier: DeviceTier
)
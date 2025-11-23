package com.aktarjabed.inbusiness.domain.device

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeviceClassifier @Inject constructor() {

    /**
     * Determines device tier based on hardware capabilities
     * Uses RAM, CPU cores, and Android version as proxies for device age/quality
     */
    fun getDeviceTier(context: Context): DeviceTier {
        val ramMB = getTotalMemoryMB(context)
        val cpuCores = Runtime.getRuntime().availableProcessors()
        val androidVersion = Build.VERSION.SDK_INT

        // Scoring system
        val ramScore = when {
            ramMB >= 6000 -> 3  // 6GB+
            ramMB >= 4000 -> 2  // 4GB+
            ramMB >= 2000 -> 1  // 2GB+
            else -> 0           // <2GB
        }

        val cpuScore = when {
            cpuCores >= 8 -> 3  // Octa-core+
            cpuCores >= 6 -> 2  // Hexa-core
            cpuCores >= 4 -> 1  // Quad-core
            else -> 0           // Dual-core or less
        }

        val osScore = when {
            androidVersion >= 33 -> 3  // Android 13+ (2022+)
            androidVersion >= 31 -> 2  // Android 12 (2021)
            androidVersion >= 29 -> 1  // Android 10 (2019-2020)
            else -> 0                  // Android 9 or older
        }

        val totalScore = ramScore + cpuScore + osScore

        return when {
            totalScore >= 7 -> DeviceTier.HIGH_END    // Premium device
            totalScore >= 4 -> DeviceTier.MID_RANGE   // Decent device
            else -> DeviceTier.LOW_END                // Budget/old device
        }
    }

    private fun getTotalMemoryMB(context: Context): Long {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        return memoryInfo.totalMem / (1024 * 1024) // Convert to MB
    }

    /**
     * Get human-readable device info for analytics
     */
    fun getDeviceInfo(context: Context): DeviceInfo {
        val ramMB = getTotalMemoryMB(context)
        val cpuCores = Runtime.getRuntime().availableProcessors()

        return DeviceInfo(
            manufacturer = Build.MANUFACTURER,
            model = Build.MODEL,
            androidVersion = Build.VERSION.RELEASE,
            sdkInt = Build.VERSION.SDK_INT,
            ramMB = ramMB,
            cpuCores = cpuCores,
            tier = getDeviceTier(context)
        )
    }
}

enum class DeviceTier {
    LOW_END,      // Maps to FREE tier
    MID_RANGE,    // Maps to BASIC tier
    HIGH_END      // Maps to PRO tier (for future auto-upgrade trials)
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

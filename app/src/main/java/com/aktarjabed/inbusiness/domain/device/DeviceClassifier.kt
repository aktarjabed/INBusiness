package com.aktarjabed.inbusiness.domain.device

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeviceClassifier @Inject constructor() {
<<<<<<< HEAD

=======

>>>>>>> ad4bb8454e6b04046f0ba290c1f04d22f25fbfa5
    fun getDeviceTier(context: Context): DeviceTier {
        val ramMB = getTotalMemoryMB(context)
        val cpuCores = Runtime.getRuntime().availableProcessors()
        val androidVersion = Build.VERSION.SDK_INT
<<<<<<< HEAD

=======

>>>>>>> ad4bb8454e6b04046f0ba290c1f04d22f25fbfa5
        val ramScore = when {
            ramMB >= 6000 -> 3
            ramMB >= 4000 -> 2
            ramMB >= 2000 -> 1
            else -> 0
        }
<<<<<<< HEAD

=======

>>>>>>> ad4bb8454e6b04046f0ba290c1f04d22f25fbfa5
        val cpuScore = when {
            cpuCores >= 8 -> 3
            cpuCores >= 6 -> 2
            cpuCores >= 4 -> 1
            else -> 0
        }
<<<<<<< HEAD

=======

>>>>>>> ad4bb8454e6b04046f0ba290c1f04d22f25fbfa5
        val osScore = when {
            androidVersion >= 33 -> 3
            androidVersion >= 31 -> 2
            androidVersion >= 29 -> 1
            else -> 0
        }
<<<<<<< HEAD

        val totalScore = ramScore + cpuScore + osScore

=======

        val totalScore = ramScore + cpuScore + osScore

>>>>>>> ad4bb8454e6b04046f0ba290c1f04d22f25fbfa5
        return when {
            totalScore >= 7 -> DeviceTier.HIGH_END
            totalScore >= 4 -> DeviceTier.MID_RANGE
            else -> DeviceTier.LOW_END
        }
    }
<<<<<<< HEAD

=======

>>>>>>> ad4bb8454e6b04046f0ba290c1f04d22f25fbfa5
    private fun getTotalMemoryMB(context: Context): Long {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        return memoryInfo.totalMem / (1024 * 1024)
    }
}

enum class DeviceTier {
    LOW_END,
    MID_RANGE,
    HIGH_END
}
package com.aktarjabed.inbusiness.data.remote.config

import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RemoteConfigRepository @Inject constructor() {

    private val remoteConfig: FirebaseRemoteConfig = FirebaseRemoteConfig.getInstance()

    init {
        val configSettings = FirebaseRemoteConfigSettings.Builder()
            .setMinimumFetchIntervalInSeconds(3600) // 1 hour
            .build()

        remoteConfig.setConfigSettingsAsync(configSettings)

        // Set defaults
        remoteConfig.setDefaultsAsync(
            mapOf(
                "google_web_client_id" to "",
                "microsoft_client_id" to "",
                "razorpay_key_id" to "",
                "maintenance_mode" to false,
                "free_daily_limit" to 2L,
                "launch_ends" to "2026-12-31",
                "force_update_version" to 0L
            )
        )
    }

    suspend fun fetch(): Boolean {
        return try {
            remoteConfig.fetchAndActivate().await()
            Timber.d("Remote config fetched successfully")
            true
        } catch (e: Exception) {
            Timber.e(e, "Failed to fetch remote config")
            false
        }
    }

    fun getGoogleWebClientId(): String =
        remoteConfig.getString("google_web_client_id")

    fun getMicrosoftClientId(): String =
        remoteConfig.getString("microsoft_client_id")

    fun getRazorpayKeyId(): String =
        remoteConfig.getString("razorpay_key_id")

    fun isMaintenanceMode(): Boolean =
        remoteConfig.getBoolean("maintenance_mode")

    fun getFreeDailyLimit(): Long =
        remoteConfig.getLong("free_daily_limit")

    fun getLaunchEnds(): String =
        remoteConfig.getString("launch_ends")

    fun getForceUpdateVersion(): Long =
        remoteConfig.getLong("force_update_version")
}
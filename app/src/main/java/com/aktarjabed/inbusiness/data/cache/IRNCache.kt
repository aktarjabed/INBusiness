package com.aktarjabed.inbusiness.data.cache

import android.content.Context
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.aktarjabed.inbusiness.data.remote.models.IRNData
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours

@Singleton
class IRNCache @Inject constructor(
    @ApplicationContext private val context: Context,
    private val gson: Gson
) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPreferences = EncryptedSharedPreferences.create(
        context,
        PREFS_NAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun cacheIRN(irn: String, data: IRNData) {
        try {
            val json = gson.toJson(data)
            sharedPreferences.edit().apply {
                putString(KEY_PREFIX + irn, json)
                putLong(KEY_PREFIX + irn + TIMESTAMP_SUFFIX, System.currentTimeMillis())
                apply()
            }
            Log.d(TAG, "Cached IRN: $irn")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to cache IRN", e)
        }
    }

    fun getIRN(irn: String): IRNData? {
        return try {
            val json = sharedPreferences.getString(KEY_PREFIX + irn, null)
            json?.let { gson.fromJson(it, IRNData::class.java) }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to retrieve IRN from cache", e)
            null
        }
    }

    fun isStale(irn: String, maxAge: Duration = DEFAULT_MAX_AGE): Boolean {
        val timestamp = sharedPreferences.getLong(
            KEY_PREFIX + irn + TIMESTAMP_SUFFIX,
            0L
        )

        if (timestamp == 0L) return true

        val age = System.currentTimeMillis() - timestamp
        return age > maxAge.inWholeMilliseconds
    }

    fun clearCache() {
        sharedPreferences.edit().clear().apply()
        Log.d(TAG, "IRN cache cleared")
    }

    companion object {
        private const val TAG = "IRNCache"
        private const val PREFS_NAME = "irn_cache_encrypted"
        private const val KEY_PREFIX = "irn_"
        private const val TIMESTAMP_SUFFIX = "_ts"
        private val DEFAULT_MAX_AGE = 24.hours
    }
}
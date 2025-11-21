package com.aktarjabed.inbusiness.data.util

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import timber.log.Timber
import java.security.KeyStore
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

object KeyProvider {
    private const val KEYSTORE_ALIAS = "inbusiness_master_key"
    private const val PREFS_NAME = "secure_prefs"
    private const val KEY_DATABASE_PASSWORD = "db_password"

    private lateinit var context: Context
    private var masterKey: MasterKey? = null

    fun initialize(appContext: Context) {
        context = appContext.applicationContext
        try {
            masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            Timber.d("KeyProvider initialized successfully")
        } catch (e: Exception) {
            Timber.e(e, "Failed to initialize KeyProvider")
            throw SecurityException("Failed to initialize encryption", e)
        }
    }

    fun getDatabasePassword(context: Context): String {
        return try {
            val encryptedPrefs = EncryptedSharedPreferences.create(
                context,
                PREFS_NAME,
                getMasterKey(context),
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )

            // Get or generate password
            encryptedPrefs.getString(KEY_DATABASE_PASSWORD, null) ?: run {
                val newPassword = generateSecurePassword()
                encryptedPrefs.edit().putString(KEY_DATABASE_PASSWORD, newPassword).apply()
                newPassword
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to get database password")
            throw SecurityException("Failed to get database password", e)
        }
    }

    private fun getMasterKey(context: Context): MasterKey {
        return masterKey ?: MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
    }

    private fun generateSecurePassword(): String {
        val charset = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*"
        return (1..32)
            .map { charset.random() }
            .joinToString("")
    }

    fun generateEncryptionKey(): SecretKey {
        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            "AndroidKeyStore"
        )

        val keyGenParameterSpec = KeyGenParameterSpec.Builder(
            KEYSTORE_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()

        keyGenerator.init(keyGenParameterSpec)
        return keyGenerator.generateKey()
    }

    fun getEncryptionKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore")
        keyStore.load(null)

        return if (keyStore.containsAlias(KEYSTORE_ALIAS)) {
            keyStore.getKey(KEYSTORE_ALIAS, null) as SecretKey
        } else {
            generateEncryptionKey()
        }
    }
}
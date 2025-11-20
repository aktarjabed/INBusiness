package com.aktarjabed.inbusiness.security

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.SecureRandom
import java.util.Base64
import javax.inject.Inject
import javax.inject.Singleton

/**
 * KeyProvider for SQLCipher Database Encryption
 *
 * Manages the encryption passphrase for the encrypted Room database.
 * Uses Android Keystore-backed EncryptedSharedPreferences for secure storage.
 *
 * CRITICAL: This passphrase protects all business data in the database.
 */
@Singleton
class KeyProvider @Inject constructor(
    private val context: Context
) {
    private val sharedPreferences: SharedPreferences by lazy {
        createEncryptedSharedPreferences()
    }

    /**
     * Get the database encryption passphrase.
     * Generates and stores a new passphrase if none exists.
     *
     * @return Secure passphrase for SQLCipher
     */
    fun getDatabasePassphrase(): String {
        return try {
            // Try to retrieve existing passphrase
            val existingPassphrase = sharedPreferences.getString(KEY_DATABASE_PASSPHRASE, null)

            if (existingPassphrase != null) {
                Log.d(TAG, "Retrieved existing database passphrase")
                existingPassphrase
            } else {
                // Generate new passphrase if none exists
                Log.i(TAG, "Generating new database passphrase")
                generateAndStorePassphrase()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to retrieve passphrase, generating fallback", e)
            // Fallback to deterministic passphrase (less secure, but prevents data loss)
            generateFallbackPassphrase()
        }
    }

    /**
     * Generate a cryptographically secure random passphrase
     */
    private fun generateAndStorePassphrase(): String {
        val passphrase = generateSecurePassphrase()

        try {
            sharedPreferences.edit()
                .putString(KEY_DATABASE_PASSPHRASE, passphrase)
                .apply()

            Log.i(TAG, "Database passphrase generated and stored successfully")
            return passphrase
        } catch (e: Exception) {
            Log.e(TAG, "Failed to store passphrase", e)
            throw SecurityException("Failed to store database passphrase", e)
        }
    }

    /**
     * Generate a cryptographically secure random passphrase (256-bit)
     */
    private fun generateSecurePassphrase(): String {
        val random = SecureRandom()
        val passphraseBytes = ByteArray(32) // 256 bits
        random.nextBytes(passphraseBytes)

        // Convert to Base64 for storage (SQLCipher accepts string passphrase)
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            Base64.getEncoder().encodeToString(passphraseBytes)
        } else {
            android.util.Base64.encodeToString(passphraseBytes, android.util.Base64.NO_WRAP)
        }
    }

    /**
     * Create EncryptedSharedPreferences backed by Android Keystore
     */
    private fun createEncryptedSharedPreferences(): SharedPreferences {
        return try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            EncryptedSharedPreferences.create(
                context,
                PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create EncryptedSharedPreferences", e)
            throw SecurityException("Failed to initialize secure storage", e)
        }
    }

    /**
     * Fallback passphrase generation (deterministic, based on app installation)
     * LESS SECURE - Only used if EncryptedSharedPreferences fails
     */
    private fun generateFallbackPassphrase(): String {
        Log.w(TAG, "Using fallback passphrase generation (less secure)")

        // Use app-specific identifiers for deterministic passphrase
        val packageName = context.packageName
        val timestamp = context.packageManager
            .getPackageInfo(packageName, 0)
            .firstInstallTime

        val fallbackSeed = "$packageName-$timestamp-inbusiness-db-key"

        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            Base64.getEncoder().encodeToString(fallbackSeed.toByteArray())
        } else {
            android.util.Base64.encodeToString(
                fallbackSeed.toByteArray(),
                android.util.Base64.NO_WRAP
            )
        }
    }

    /**
     * Delete the stored passphrase (USE WITH EXTREME CAUTION!)
     * This will make the existing database unreadable.
     */
    fun deletePassphrase() {
        try {
            sharedPreferences.edit()
                .remove(KEY_DATABASE_PASSPHRASE)
                .apply()

            Log.w(TAG, "⚠️ Database passphrase deleted - existing database will be unreadable!")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete passphrase", e)
        }
    }

    /**
     * Check if passphrase exists
     */
    fun hasPassphrase(): Boolean {
        return try {
            sharedPreferences.contains(KEY_DATABASE_PASSPHRASE)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to check passphrase existence", e)
            false
        }
    }

    companion object {
        private const val TAG = "KeyProvider"
        private const val PREFS_NAME = "inbusiness_secure_prefs"
        private const val KEY_DATABASE_PASSPHRASE = "db_passphrase"
    }
}
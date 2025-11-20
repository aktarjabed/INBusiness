package com.aktarjabed.inbusiness.security

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import java.security.SecureRandom

object KeyProvider {

    private const val PREF_FILE_NAME = "secure_prefs"
    private const val DB_PASSPHRASE_KEY = "db_passphrase"

    fun getDatabasePassphrase(context: Context): ByteArray {
        val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)

        val sharedPreferences = EncryptedSharedPreferences.create(
            PREF_FILE_NAME,
            masterKeyAlias,
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )

        var passphrase = sharedPreferences.getString(DB_PASSPHRASE_KEY, null)

        if (passphrase == null) {
            passphrase = generateRandomPassphrase()
            sharedPreferences.edit().putString(DB_PASSPHRASE_KEY, passphrase).apply()
        }

        return passphrase.toByteArray()
    }

    private fun generateRandomPassphrase(length: Int = 32): String {
        val random = SecureRandom()
        val bytes = ByteArray(length)
        random.nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
package com.aktarjabed.inbusiness.domain.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class EncryptionManager(private val context: Context) {

    private val keyStore: KeyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply {
        load(null)
    }

    init {
        // Ensure master key exists
        if (!keyStore.containsAlias(MASTER_KEY_ALIAS)) {
            generateMasterKey()
        }
    }

    /**
     * Encrypt sensitive data using Android Keystore
     */
    fun encrypt(plaintext: String): EncryptedData {
        try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val secretKey = getOrCreateKey()

            cipher.init(Cipher.ENCRYPT_MODE, secretKey)

            val iv = cipher.iv
            val encryptedBytes = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))

            return EncryptedData(
                ciphertext = Base64.encodeToString(encryptedBytes, Base64.NO_WRAP),
                iv = Base64.encodeToString(iv, Base64.NO_WRAP)
            )
        } catch (e: Exception) {
            Log.e(TAG, "Encryption failed", e)
            throw SecurityException("Encryption failed: ${e.message}", e)
        }
    }

    /**
     * Decrypt encrypted data
     */
    fun decrypt(encryptedData: EncryptedData): String {
        try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val secretKey = getOrCreateKey()

            val iv = Base64.decode(encryptedData.iv, Base64.NO_WRAP)
            val encryptedBytes = Base64.decode(encryptedData.ciphertext, Base64.NO_WRAP)

            val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)

            val decryptedBytes = cipher.doFinal(encryptedBytes)
            return String(decryptedBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            Log.e(TAG, "Decryption failed", e)
            throw SecurityException("Decryption failed: ${e.message}", e)
        }
    }

    /**
     * Generate a secure random encryption key
     */
    fun generateSecureKey(length: Int = 32): ByteArray {
        val key = ByteArray(length)
        java.security.SecureRandom().nextBytes(key)
        return key
    }

    private fun getOrCreateKey(): SecretKey {
        return if (keyStore.containsAlias(MASTER_KEY_ALIAS)) {
            keyStore.getKey(MASTER_KEY_ALIAS, null) as SecretKey
        } else {
            generateMasterKey()
        }
    }

    private fun generateMasterKey(): SecretKey {
        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            ANDROID_KEYSTORE
        )

        val keyGenParameterSpec = KeyGenParameterSpec.Builder(
            MASTER_KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .setUserAuthenticationRequired(false)
            .setRandomizedEncryptionRequired(true)
            .build()

        keyGenerator.init(keyGenParameterSpec)
        val key = keyGenerator.generateKey()

        Log.d(TAG, "Generated new master key")
        return key
    }

    /**
     * Delete all encryption keys (use when user logs out)
     */
    fun deleteAllKeys() {
        try {
            if (keyStore.containsAlias(MASTER_KEY_ALIAS)) {
                keyStore.deleteEntry(MASTER_KEY_ALIAS)
                Log.d(TAG, "Deleted master key")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete keys", e)
        }
    }

    data class EncryptedData(
        val ciphertext: String,
        val iv: String
    )

    companion object {
        private const val TAG = "EncryptionManager"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val MASTER_KEY_ALIAS = "inbusiness_master_key"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_TAG_LENGTH = 128
    }
}
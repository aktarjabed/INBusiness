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
        if (!keyStore.containsAlias(KEY_ALIAS)) {
            generateKey()
        }
    }

    fun encrypt(plaintext: String): String {
        try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, getSecretKey())

            val iv = cipher.iv
            val encryptedBytes = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))

            val combined = iv + encryptedBytes
            return Base64.encodeToString(combined, Base64.NO_WRAP)
        } catch (e: Exception) {
            Log.e(TAG, "Encryption failed", e)
            throw EncryptionException("Failed to encrypt data", e)
        }
    }

    fun decrypt(ciphertext: String): String {
        try {
            val combined = Base64.decode(ciphertext, Base64.NO_WRAP)

            val iv = combined.copyOfRange(0, 12)
            val encryptedBytes = combined.copyOfRange(12, combined.size)

            val cipher = Cipher.getInstance(TRANSFORMATION)
            val spec = GCMParameterSpec(128, iv)
            cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), spec)

            val decryptedBytes = cipher.doFinal(encryptedBytes)
            return String(decryptedBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            Log.e(TAG, "Decryption failed", e)
            throw EncryptionException("Failed to decrypt data", e)
        }
    }

    private fun generateKey() {
        try {
            val keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                ANDROID_KEYSTORE
            )

            val keyGenParameterSpec = KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .setUserAuthenticationRequired(false)
                .build()

            keyGenerator.init(keyGenParameterSpec)
            keyGenerator.generateKey()

            Log.d(TAG, "Encryption key generated successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Key generation failed", e)
            throw EncryptionException("Failed to generate encryption key", e)
        }
    }

    private fun getSecretKey(): SecretKey {
        return keyStore.getKey(KEY_ALIAS, null) as SecretKey
            ?: throw EncryptionException("Encryption key not found")
    }

    companion object {
        private const val TAG = "EncryptionManager"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "INBusinessEncryptionKey"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
    }
}

class EncryptionException(message: String, cause: Throwable? = null) : Exception(message, cause)
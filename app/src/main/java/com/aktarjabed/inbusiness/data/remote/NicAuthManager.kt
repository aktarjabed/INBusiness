package com.aktarjabed.inbusiness.data.remote

import android.util.Base64
import android.util.Log
import com.aktarjabed.inbusiness.BuildConfig
import com.aktarjabed.inbusiness.data.remote.models.AuthData
import com.aktarjabed.inbusiness.data.remote.models.AuthRequest
import com.aktarjabed.inbusiness.di.IoDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.security.KeyFactory
import java.security.PublicKey
import java.security.SecureRandom
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NicAuthManager @Inject constructor(
    private val nicApi: NicApi,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
    private val mutex = Mutex()
    private var cachedAuth: CachedAuth? = null

    suspend fun getAuthToken(): Result<String> = withContext(ioDispatcher) {
        mutex.withLock {
            // Return cached token if valid
            cachedAuth?.let { cached ->
                if (System.currentTimeMillis() < cached.expiryTime) {
                    Log.d(TAG, "Using cached auth token")
                    return@withContext Result.success(cached.authData.authToken)
                }
            }

            // Generate new token
            authenticateWithNIC()
        }
    }

    suspend fun getAuthData(): Result<AuthData> = withContext(ioDispatcher) {
        mutex.withLock {
            cachedAuth?.let { cached ->
                if (System.currentTimeMillis() < cached.expiryTime) {
                    return@withContext Result.success(cached.authData)
                }
            }

            authenticateWithNIC().map { cachedAuth!!.authData }
        }
    }

    private suspend fun authenticateWithNIC(): Result<String> = runCatching {
        Log.d(TAG, "Authenticating with NIC...")

        // Step 1: Generate random 32-byte AppKey
        val appKey = ByteArray(32).also { SecureRandom().nextBytes(it) }
        val appKeyBase64 = Base64.encodeToString(appKey, Base64.NO_WRAP)

        // Step 2: Build credentials JSON
        val credentials = JSONObject().apply {
            put("UserName", BuildConfig.NIC_USERNAME)
            put("Password", BuildConfig.NIC_PASSWORD)
            put("AppKey", appKeyBase64)
            put("ForceRefreshAccessToken", true)
        }.toString()

        // Step 3: Encrypt credentials with NIC public key
        val encryptedData = encryptWithPublicKey(credentials)
        val encryptedDataBase64 = Base64.encodeToString(encryptedData, Base64.NO_WRAP)

        // Step 4: Call authentication API
        val response = nicApi.authenticate(AuthRequest(encryptedDataBase64))

        if (response.status != "1") {
            val errorMsg = response.errorDetails?.firstOrNull()?.errorMessage
                ?: "Authentication failed"
            throw NicException(errorMsg)
        }

        val authData = response.data ?: throw NicException("No auth data received")

        // Step 5: Decrypt SEK with AppKey
        val sek = decryptSEK(authData.sek, appKey)

        // Cache the authentication data
        cachedAuth = CachedAuth(
            authData = authData,
            sek = sek,
            expiryTime = System.currentTimeMillis() + TOKEN_VALIDITY_MS
        )

        Log.d(TAG, "Authentication successful. Token expires at: ${cachedAuth!!.expiryTime}")
        authData.authToken
    }

    private fun encryptWithPublicKey(data: String): ByteArray {
        val publicKey = loadPublicKey(NIC_PUBLIC_KEY)
        val cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding")
        cipher.init(Cipher.ENCRYPT_MODE, publicKey)
        return cipher.doFinal(data.toByteArray(Charsets.UTF_8))
    }

    private fun loadPublicKey(pem: String): PublicKey {
        val cleanKey = pem
            .replace("-----BEGIN PUBLIC KEY-----", "")
            .replace("-----END PUBLIC KEY-----", "")
            .replace(Regex("\\s"), "")

        val decoded = Base64.decode(cleanKey, Base64.DEFAULT)
        val keySpec = X509EncodedKeySpec(decoded)
        val keyFactory = KeyFactory.getInstance("RSA")
        return keyFactory.generatePublic(keySpec)
    }

    private fun decryptSEK(encryptedSek: String, appKey: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/ECB/PKCS7Padding", "BC")
        val secretKey: SecretKey = SecretKeySpec(appKey, "AES")
        cipher.init(Cipher.DECRYPT_MODE, secretKey)
        return cipher.doFinal(Base64.decode(encryptedSek, Base64.NO_WRAP))
    }

    internal fun getSEK(): ByteArray? = cachedAuth?.sek

    fun clearCache() {
        cachedAuth = null
        Log.d(TAG, "Auth cache cleared")
    }

    private data class CachedAuth(
        val authData: AuthData,
        val sek: ByteArray,
        val expiryTime: Long
    )

    companion object {
        private const val TAG = "NicAuthManager"
        private const val TOKEN_VALIDITY_MS = 6 * 60 * 60 * 1000L // 6 hours

        // NIC Sandbox Public Key - REPLACE with actual key from NIC portal
        private const val NIC_PUBLIC_KEY = """
-----BEGIN PUBLIC KEY-----
MIICIjANBgkqhkiG9w0BAQEFAAOCAg8AMIICCgKCAgEAx6pKvAjGN7+7rqFt5Txi
6a6Z9Iz3l7xPRvLH+jELqHKnFfNLxLlT/MQ0F8HX5F6VVVCPRiQQZYV1aPQvZvNy
dGRBzN0Y4JRWiGEYLwOdQc8GbMVvDUiZpZZ3P3mJ+ThpJVeEDzGo8RGqGZD0n7Z+
OkELAYF7BZdD9l9qLwZQl7OwxVMCCGBJTmKSxCrKeLzjN5WYkPHZhLf7eTlELJbG
v3vRfL7OMVEjGp8sXhHJ0LVEGEaLpF7U0GKTlXLN0HdJZKCqVoIEQQqh8cGFLpzQ
SLpKp6yKnQdPQUzPdvVnVsPJpHNKyLj2Ld7cL0qMQqKGKF7GHbFPLvQZqHNZ+mjY
tXPqYnNq8XVc9LdYZGPqK7pL6nHqMJ0YcQMzGLpGHKq7pYLJGPLvPqMHZLnKqYnJ
pVqLnJqMqKnLqJpGqHLnPqYnJqKqHnLqJpGqHLnPqYnJqKqHnLqJpGqHLnPqYnJq
KqHnLqJpGqHLnPqYnJqKqHnLqJpGqHLnPqYnJqKqHnLqJpGqHLnPqYnJqKqHnLqJ
pGqHLnPqYnJqKqHnLqJpGqHLnPqYnJqKqHnLqJpGqHLnPqYnJqKqHnLqJpGqHLnP
qYnJqKqHnLqJpGqHLnPqYnJqKqHnLqJpGqHLnPqYnJqKqHnLqJpGqHLnPqYnJqKq
HnLqJpGqHLnPqYnJqKqHnLqJpGqHLnPqYnJqKqHnLqJpGqHLnPqYnJqKqHnLqJpG
qYnJqKqHnLqJpGqHLnPqYnJqKqHnLqJpGqHLnPqYnJqKqHnLqJpGqHLnPqYnJqKq
HnLqJpGqHLnPqCAwEAAQ==
-----END PUBLIC KEY-----
        """
    }
}

class NicException(message: String) : Exception(message)
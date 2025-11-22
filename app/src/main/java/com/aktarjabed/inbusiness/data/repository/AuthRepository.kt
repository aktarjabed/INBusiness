package com.aktarjabed.inbusiness.data.repository

import android.app.Activity
import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.aktarjabed.inbusiness.data.local.dao.UserDao
import com.aktarjabed.inbusiness.data.local.dao.UserQuotaDao
import com.aktarjabed.inbusiness.data.local.entities.UserEntity
import com.aktarjabed.inbusiness.data.local.entities.UserQuotaEntity
import com.aktarjabed.inbusiness.data.remote.config.RemoteConfigRepository
import com.aktarjabed.inbusiness.domain.model.User
import com.aktarjabed.inbusiness.util.DeviceClassifier
import com.aktarjabed.inbusiness.util.SystemClock
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val firebaseAuth: FirebaseAuth,
    private val userDao: UserDao,
    private val quotaDao: UserQuotaDao,
    private val deviceClassifier: DeviceClassifier,
    private val systemClock: SystemClock,
    private val remoteConfig: RemoteConfigRepository
) {
    private val credentialManager = CredentialManager.create(context)

    val currentUserFlow: Flow<User?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            try {
                auth.currentUser?.let { firebaseUser ->
                    val user = firebaseUser.toUser()
                    val entity = UserEntity(
                        uid = user.id,
                        email = user.email,
                        phone = user.phoneNumber,
                        displayName = user.displayName,
                        photoUrl = user.photoUrl,
                        provider = user.provider.name,
                        isEmailVerified = user.isEmailVerified,
                        createdAt = System.currentTimeMillis(),
                        lastLoginAt = System.currentTimeMillis()
                    )

                    kotlinx.coroutines.launch {
                        try {
                            userDao.insertUser(entity)
                            userDao.updateLastLogin(entity.uid)

                            if (quotaDao.getQuota(entity.uid) == null) {
                                createDefaultQuota(entity.uid)
                            }
                        } catch (e: Exception) {
                            Timber.e(e, "Error saving user data")
                        }
                    }

                    trySend(user)
                } ?: trySend(null)
            } catch (e: Exception) {
                Timber.e(e, "Auth state listener error")
                trySend(null)
            }
        }

        firebaseAuth.addAuthStateListener(listener)
        awaitClose { firebaseAuth.removeAuthStateListener(listener) }
    }

    suspend fun signInWithGoogle(activity: Activity): Result<User> {
        return try {
            val webClientId = remoteConfig.getGoogleWebClientId()
            if (webClientId.isBlank()) {
                return Result.failure(Exception("Google Web Client ID not configured"))
            }

            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(webClientId)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val result = credentialManager.getCredential(activity, request)
            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(result.credential.data)

            val googleCredential = GoogleAuthProvider.getCredential(
                googleIdTokenCredential.idToken,
                null
            )

            val authResult = firebaseAuth.signInWithCredential(googleCredential).await()
            val user = authResult.user?.toUser()
                ?: return Result.failure(Exception("User is null"))

            Timber.i("Google sign-in successful: ${user.email}")
            Result.success(user)

        } catch (e: GetCredentialException) {
            Timber.e(e, "Google sign-in failed")
            Result.failure(Exception("Google sign-in cancelled or failed"))
        } catch (e: Exception) {
            Timber.e(e, "Google sign-in error")
            Result.failure(e)
        }
    }

    fun sendOTP(
        activity: Activity,
        phoneNumber: String,
        onCodeSent: (String) -> Unit,
        onError: (Exception) -> Unit
    ) {
        try {
            val options = PhoneAuthOptions.newBuilder(firebaseAuth)
                .setPhoneNumber(phoneNumber)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(activity)
                .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                    override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                        Timber.d("Auto-verification completed")
                        firebaseAuth.signInWithCredential(credential)
                    }

                    override fun onVerificationFailed(e: FirebaseException) {
                        Timber.e(e, "OTP verification failed")
                        onError(e)
                    }

                    override fun onCodeSent(
                        verificationId: String,
                        token: PhoneAuthProvider.ForceResendingToken
                    ) {
                        Timber.i("OTP sent successfully")
                        onCodeSent(verificationId)
                    }
                })
                .build()

            PhoneAuthProvider.verifyPhoneNumber(options)
        } catch (e: Exception) {
            Timber.e(e, "Error sending OTP")
            onError(e)
        }
    }

    suspend fun verifyOTP(verificationId: String, code: String): Result<User> {
        return try {
            val credential = PhoneAuthProvider.getCredential(verificationId, code)
            val authResult = firebaseAuth.signInWithCredential(credential).await()
            val user = authResult.user?.toUser()
                ?: return Result.failure(Exception("User is null"))

            Timber.i("OTP verified successfully")
            Result.success(user)
        } catch (e: Exception) {
            Timber.e(e, "OTP verification failed")
            Result.failure(e)
        }
    }

    suspend fun signInWithEmail(email: String, password: String): Result<User> {
        return try {
            val authResult = firebaseAuth.signInWithEmailAndPassword(email, password).await()
            val user = authResult.user?.toUser()
                ?: return Result.failure(Exception("User is null"))

            Timber.i("Email sign-in successful")
            Result.success(user)
        } catch (e: Exception) {
            Timber.e(e, "Email sign-in failed")
            Result.failure(e)
        }
    }

    suspend fun signOut() {
        try {
            firebaseAuth.signOut()
            Timber.i("Sign-out successful")
        } catch (e: Exception) {
            Timber.e(e, "Sign-out error")
        }
    }

    private suspend fun createDefaultQuota(userId: String) {
        try {
            val today = systemClock.todayEpochDay()
            val deviceTier = deviceClassifier.getDeviceTier(context)

            val quota = UserQuotaEntity(
                userId = userId,
                tier = "FREE",
                dailyUsed = 0,
                lastResetEpochDay = today,
                monthlyUsed = 0,
                lastMonthlyResetEpochDay = systemClock.monthStartEpochDay(),
                watermark = true,
                retentionDays = 30,
                freeExpiryEpochDay = today + 365,
                deviceTier = deviceTier.name
            )

            quotaDao.insertOrReplace(quota)
            Timber.d("Default quota created for user: $userId")
        } catch (e: Exception) {
            Timber.e(e, "Failed to create default quota")
        }
    }

    private fun FirebaseUser.toUser(): User {
        val provider = when {
            providerData.any { it.providerId == "google.com" } -> User.Provider.GOOGLE
            providerData.any { it.providerId == "microsoft.com" } -> User.Provider.MICROSOFT
            providerData.any { it.providerId == "phone" } -> User.Provider.PHONE
            else -> User.Provider.EMAIL
        }

        return User(
            id = uid,
            email = email,
            phoneNumber = phoneNumber,
            displayName = displayName,
            photoUrl = photoUrl?.toString(),
            provider = provider,
            isEmailVerified = isEmailVerified
        )
    }
}
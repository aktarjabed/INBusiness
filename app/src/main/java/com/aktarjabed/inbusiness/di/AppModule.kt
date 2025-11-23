package com.aktarjabed.inbusiness.di

import android.content.Context
import com.aktarjabed.inbusiness.data.remote.RemoteConfigRepository
import com.aktarjabed.inbusiness.domain.device.DeviceClassifier
import com.aktarjabed.inbusiness.security.EncryptionManager
import com.aktarjabed.inbusiness.util.CircuitBreaker
import com.aktarjabed.inbusiness.util.NetworkMonitor
import com.aktarjabed.inbusiness.util.SystemClock
import com.google.firebase.auth.FirebaseAuth
import com.razorpay.Checkout
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import javax.inject.Qualifier
import javax.inject.Singleton
import kotlin.time.Duration.Companion.seconds

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideEncryptionManager(
        @ApplicationContext context: Context
    ): EncryptionManager = EncryptionManager(context)

    @Provides
    @Singleton
    fun provideCircuitBreaker(): CircuitBreaker = CircuitBreaker(
        failureThreshold = 3,
        resetTimeout = 60.seconds,
        halfOpenMaxAttempts = 2
    )

    @Provides
    @Singleton
    fun provideNetworkMonitor(
        @ApplicationContext context: Context
    ): NetworkMonitor = NetworkMonitor(context)

    @Provides
    @Singleton
    fun provideDeviceClassifier(): DeviceClassifier = DeviceClassifier()

    @Provides
    @Singleton
    fun provideSystemClock(): SystemClock = SystemClock()

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    @Provides
    @Singleton
    fun provideRemoteConfig(): RemoteConfigRepository = RemoteConfigRepository()

    @Provides
    fun provideCheckout(): Checkout {
        Checkout.preload(null)
        return Checkout()
    }

    @Provides
    @IoDispatcher
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO

    @Provides
    @MainDispatcher
    fun provideMainDispatcher(): CoroutineDispatcher = Dispatchers.Main

    @Provides
    @DefaultDispatcher
    fun provideDefaultDispatcher(): CoroutineDispatcher = Dispatchers.Default
}

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class MainDispatcher

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DefaultDispatcher

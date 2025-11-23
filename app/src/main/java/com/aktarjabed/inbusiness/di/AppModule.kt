package com.aktarjabed.inbusiness.di

import android.content.Context
import com.aktarjabed.inbusiness.data.remote.config.RemoteConfigRepository
import com.aktarjabed.inbusiness.security.EncryptionManager
import com.aktarjabed.inbusiness.util.DeviceClassifier
import com.aktarjabed.inbusiness.util.SystemClock
import com.google.firebase.auth.FirebaseAuth
import com.razorpay.Checkout
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

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
    fun provideSystemClock(): SystemClock = SystemClock()

    @Provides
    @Singleton
    fun provideDeviceClassifier(): DeviceClassifier = DeviceClassifier()

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
}
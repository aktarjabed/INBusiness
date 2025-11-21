package com.aktarjabed.inbusiness.di

import android.content.Context
<<<<<<< HEAD
import com.aktarjabed.inbusiness.data.local.dao.*
import com.aktarjabed.inbusiness.data.local.database.AppDatabase
import com.aktarjabed.inbusiness.security.EncryptionManager
=======
import com.aktarjabed.inbusiness.data.database.AppDatabase
import com.aktarjabed.inbusiness.data.dao.BusinessDao
import com.aktarjabed.inbusiness.data.dao.InvoiceDao
import com.aktarjabed.inbusiness.data.dao.UserQuotaDao
import com.aktarjabed.inbusiness.security.KeyProvider
>>>>>>> ad4bb8454e6b04046f0ba290c1f04d22f25fbfa5
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
<<<<<<< HEAD
    fun provideAppDatabase(
        @ApplicationContext context: Context,
        encryptionManager: EncryptionManager
    ): AppDatabase = AppDatabase.getDatabase(context, encryptionManager)

    @Provides
    @Singleton
    fun provideUserDao(database: AppDatabase): UserDao =
        database.userDao()

    @Provides
    @Singleton
    fun provideUserQuotaDao(database: AppDatabase): UserQuotaDao =
        database.userQuotaDao()

    @Provides
    @Singleton
    fun providePaymentDao(database: AppDatabase): PaymentDao =
        database.paymentDao()

    @Provides
    @Singleton
    fun provideBusinessDao(database: AppDatabase): BusinessDao =
        database.businessDao()

    @Provides
    @Singleton
    fun provideInvoiceDao(database: AppDatabase): InvoiceDao =
        database.invoiceDao()
=======
    fun provideKeyProvider(
        @ApplicationContext context: Context
    ): KeyProvider = KeyProvider(context)

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context,
        keyProvider: KeyProvider
    ): AppDatabase = AppDatabase.getDatabase(context, keyProvider)

    @Provides
    @Singleton
    fun provideBusinessDao(
        database: AppDatabase
    ): BusinessDao = database.businessDao()

    @Provides
    @Singleton
    fun provideInvoiceDao(
        database: AppDatabase
    ): InvoiceDao = database.invoiceDao()

    @Provides
    @Singleton
    fun provideUserQuotaDao(
        database: AppDatabase
    ): UserQuotaDao = database.userQuotaDao()
>>>>>>> ad4bb8454e6b04046f0ba290c1f04d22f25fbfa5
}
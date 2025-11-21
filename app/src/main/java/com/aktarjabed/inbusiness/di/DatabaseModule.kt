package com.aktarjabed.inbusiness.di

import android.content.Context
import com.aktarjabed.inbusiness.data.local.dao.*
import com.aktarjabed.inbusiness.data.local.database.AppDatabase
import com.aktarjabed.inbusiness.security.EncryptionManager
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
}
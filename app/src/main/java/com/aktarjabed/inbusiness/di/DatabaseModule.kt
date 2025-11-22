package com.aktarjabed.inbusiness.di

import android.content.Context
import androidx.room.Room
import com.aktarjabed.inbusiness.data.local.dao.*
import com.aktarjabed.inbusiness.data.local.database.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.database.SupportFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    private const val DATABASE_NAME = "inbusiness_database.db"
    private const val DATABASE_PASSPHRASE = "INBusiness_2025_Secure_Key_@#$%"

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        // Initialize SQLCipher
        SQLiteDatabase.loadLibs(context)

        // Create encrypted database factory
        val passphrase = SQLiteDatabase.getBytes(DATABASE_PASSPHRASE.toCharArray())
        val factory = SupportFactory(passphrase)

        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            DATABASE_NAME
        )
            .openHelperFactory(factory)
            .fallbackToDestructiveMigration() // Remove in production after migrations are set
            .build()
    }

    @Provides
    @Singleton
    fun provideUserDao(database: AppDatabase): UserDao {
        return database.userDao()
    }

    @Provides
    @Singleton
    fun provideInvoiceDao(database: AppDatabase): InvoiceDao {
        return database.invoiceDao()
    }

    @Provides
    @Singleton
    fun providePaymentDao(database: AppDatabase): PaymentDao {
        return database.paymentDao()
    }

    @Provides
    @Singleton
    fun provideBusinessDao(database: AppDatabase): BusinessDao {
        return database.businessDao()
    }

    @Provides
    @Singleton
    fun provideUserQuotaDao(database: AppDatabase): UserQuotaDao {
        return database.userQuotaDao()
    }
}
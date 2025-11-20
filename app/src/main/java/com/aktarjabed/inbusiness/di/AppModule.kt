package com.aktarjabed.inbusiness.di

import android.content.Context
import com.aktarjabed.inbusiness.data.database.AppDatabase
import com.aktarjabed.inbusiness.data.repository.BusinessRepository
import com.aktarjabed.inbusiness.data.repository.InvoiceRepository
import com.aktarjabed.inbusiness.domain.services.PdfGenerator
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
    fun provideDatabase(@ApplicationContext ctx: Context): AppDatabase =
        AppDatabase.getDatabase(ctx)

    @Provides
    @Singleton
    fun provideBusinessRepository(db: AppDatabase): BusinessRepository =
        BusinessRepository(db.businessDao())

    @Provides
    @Singleton
    fun provideInvoiceRepository(db: AppDatabase): InvoiceRepository =
        InvoiceRepository(db.invoiceDao())

    @Provides
    @Singleton
    fun providePdfGenerator(): PdfGenerator =
        PdfGenerator()
}
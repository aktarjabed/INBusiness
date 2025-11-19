package com.aktarjabed.inbusiness.di

import android.content.Context
import com.aktarjabed.inbusiness.data.database.AppDatabase
import com.aktarjabed.inbusiness.data.repository.BusinessRepository
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
    fun provideRepository(db: AppDatabase): BusinessRepository =
        BusinessRepository(db.businessDao())
}
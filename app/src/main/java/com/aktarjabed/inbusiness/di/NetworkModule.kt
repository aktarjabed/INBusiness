package com.aktarjabed.inbusiness.di

import com.aktarjabed.inbusiness.BuildConfig
import com.aktarjabed.inbusiness.data.remote.NicApi
import com.aktarjabed.inbusiness.data.remote.NicAuthManager
import com.aktarjabed.inbusiness.data.remote.NicClient
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideGson(): Gson = GsonBuilder()
        .setLenient()
        .setDateFormat("dd/MM/yyyy HH:mm:ss")
        .create()

    @Provides
    @Singleton
    fun provideLoggingInterceptor(): HttpLoggingInterceptor =
        HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        loggingInterceptor: HttpLoggingInterceptor
    ): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    @Provides
    @Singleton
    fun provideRetrofit(
        okHttpClient: OkHttpClient,
        gson: Gson
    ): Retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.NIC_BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create(gson))
        .build()

    @Provides
    @Singleton
    fun provideNicApi(retrofit: Retrofit): NicApi =
        retrofit.create(NicApi::class.java)

    @Provides
    @Singleton
    fun provideNicAuthManager(
        nicApi: NicApi,
        @IoDispatcher ioDispatcher: CoroutineDispatcher
    ): NicAuthManager = NicAuthManager(
        nicApi = nicApi,
        username = BuildConfig.NIC_USERNAME,
        password = BuildConfig.NIC_PASSWORD,
        ioDispatcher = ioDispatcher
    )

    @Provides
    @Singleton
    fun provideNicClient(
        nicApi: NicApi,
        nicAuthManager: NicAuthManager,
        @IoDispatcher ioDispatcher: CoroutineDispatcher
    ): NicClient = NicClient(
        nicApi = nicApi,
        authManager = nicAuthManager,
        ioDispatcher = ioDispatcher
    )
}
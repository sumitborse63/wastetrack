package com.sktech.wastetrack.di

import com.sktech.wastetrack.data.remote.api.HoneywellApi
import com.sktech.wastetrack.data.remote.api.MockApiInterceptor
import com.sktech.wastetrack.data.remote.api.MpcbApi
import com.sktech.wastetrack.util.Constants
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
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
    fun provideOkHttpClient(): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        return OkHttpClient.Builder()
            .addInterceptor(logging)
            .addInterceptor(MockApiInterceptor())
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(Constants.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
    
    @Provides
    @Singleton
    fun provideHoneywellApi(retrofit: Retrofit): HoneywellApi {
        return retrofit.create(HoneywellApi::class.java)
    }

    @Provides
    @Singleton
    fun provideMpcbApi(retrofit: Retrofit): MpcbApi {
        return retrofit.create(MpcbApi::class.java)
    }
}

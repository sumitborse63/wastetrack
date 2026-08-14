package com.sktech.wastetrack.di

import android.content.Context
import com.sktech.wastetrack.data.biometric.BiometricAuthManager
import com.sktech.wastetrack.data.biometric.BiometricPreferencesManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object BiometricModule {

    @Provides
    @Singleton
    fun provideBiometricAuthManager(
        @ApplicationContext context: Context
    ): BiometricAuthManager = BiometricAuthManager(context)

    @Provides
    @Singleton
    fun provideBiometricPreferencesManager(
        @ApplicationContext context: Context,
        biometricAuthManager: BiometricAuthManager
    ): BiometricPreferencesManager = BiometricPreferencesManager(context, biometricAuthManager)
}

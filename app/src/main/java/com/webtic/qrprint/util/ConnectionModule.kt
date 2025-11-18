package com.webtic.qrprint.util

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ApplicationComponent
import javax.inject.Singleton

@Module
@InstallIn(ApplicationComponent::class)
class ConnectionModule {
    @Provides
    @Singleton
    fun provideConnectionManager(): ConnectionManager = ConnectionManager()
}
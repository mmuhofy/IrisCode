// UNTESTED — verify before use

package com.iris.iriscode.di

import com.iris.iriscode.data.remote.gemini.GeminiClient
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideGeminiClient(): GeminiClient = GeminiClient()
}
package com.iris.iriscode.di

import android.content.Context
import androidx.room.Room
import com.iris.iriscode.data.local.IrisDatabase
import com.iris.iriscode.data.local.ProjectDao
import com.iris.iriscode.data.repository.ProjectRepositoryImpl
import com.iris.iriscode.domain.repository.ProjectRepository
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
    fun provideDatabase(@ApplicationContext context: Context): IrisDatabase {
        return Room.databaseBuilder(
            context,
            IrisDatabase::class.java,
            "iris.db"
        ).build()
    }

    @Provides
    fun provideProjectDao(database: IrisDatabase): ProjectDao = database.projectDao()

    @Provides
    @Singleton
    fun provideProjectRepository(impl: ProjectRepositoryImpl): ProjectRepository = impl
}

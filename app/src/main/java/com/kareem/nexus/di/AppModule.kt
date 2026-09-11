package com.kareem.nexus.di

import android.content.Context
import androidx.room.Room
import com.kareem.nexus.data.local.NexusDao
import com.kareem.nexus.data.local.NexusDatabase
import com.kareem.nexus.data.repository.OfflineNexusRepository
import com.kareem.nexus.domain.repository.NexusRepository
import dagger.Binds
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
    fun database(@ApplicationContext context: Context): NexusDatabase =
        Room.databaseBuilder(context, NexusDatabase::class.java, "nexus.db")
            .build()

    @Provides
    fun dao(database: NexusDatabase): NexusDao = database.nexusDao()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    abstract fun bindRepository(impl: OfflineNexusRepository): NexusRepository
}


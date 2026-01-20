package com.google.jetstream.data.local

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object EpgDatabaseModule {

    @Provides
    @Singleton
    fun provideEpgDatabase(@ApplicationContext context: Context): EpgDatabase {
        return Room.databaseBuilder(
            context,
            EpgDatabase::class.java,
            "epg_database"
        )
        .fallbackToDestructiveMigration()
        .build()
    }

    @Provides
    fun provideEpgDao(database: EpgDatabase): EpgDao {
        return database.epgDao()
    }
}

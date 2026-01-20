/*
 * IPTV Local Database - Room implementation for favorites, history, and settings
 */
package com.google.jetstream.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Database(
    entities = [
        FavoriteChannel::class,
        WatchHistory::class,
        HiddenChannel::class,
        ChannelGroup::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class IptvDatabase : RoomDatabase() {
    abstract fun favoriteChannelDao(): FavoriteChannelDao
    abstract fun watchHistoryDao(): WatchHistoryDao
    abstract fun hiddenChannelDao(): HiddenChannelDao
    abstract fun channelGroupDao(): ChannelGroupDao
}

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): IptvDatabase {
        return Room.databaseBuilder(
            context,
            IptvDatabase::class.java,
            "iptv_database"
        ).build()
    }

    @Provides
    fun provideFavoriteChannelDao(database: IptvDatabase): FavoriteChannelDao {
        return database.favoriteChannelDao()
    }

    @Provides
    fun provideWatchHistoryDao(database: IptvDatabase): WatchHistoryDao {
        return database.watchHistoryDao()
    }

    @Provides
    fun provideHiddenChannelDao(database: IptvDatabase): HiddenChannelDao {
        return database.hiddenChannelDao()
    }

    @Provides
    fun provideChannelGroupDao(database: IptvDatabase): ChannelGroupDao {
        return database.channelGroupDao()
    }
}

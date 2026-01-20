/*
 * Room DAO interfaces for IPTV app
 */
package com.google.jetstream.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Data access object for favorite channels
 */
@Dao
interface FavoriteChannelDao {

    @Query("SELECT * FROM favorite_channels ORDER BY sortOrder ASC, addedAt DESC")
    fun getAllFavorites(): Flow<List<FavoriteChannel>>

    @Query("SELECT * FROM favorite_channels WHERE streamType = :streamType ORDER BY sortOrder ASC")
    fun getFavoritesByType(streamType: String): Flow<List<FavoriteChannel>>

    @Query("SELECT * FROM favorite_channels WHERE streamId = :streamId LIMIT 1")
    suspend fun getFavorite(streamId: Int): FavoriteChannel?

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_channels WHERE streamId = :streamId)")
    fun isFavorite(streamId: Int): Flow<Boolean>

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_channels WHERE streamId = :streamId)")
    suspend fun isFavoriteSync(streamId: Int): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addFavorite(favorite: FavoriteChannel)

    @Delete
    suspend fun removeFavorite(favorite: FavoriteChannel)

    @Query("DELETE FROM favorite_channels WHERE streamId = :streamId")
    suspend fun removeFavoriteById(streamId: Int)

    @Update
    suspend fun updateFavorite(favorite: FavoriteChannel)

    @Query("SELECT COUNT(*) FROM favorite_channels")
    fun getFavoritesCount(): Flow<Int>

    @Query("SELECT MAX(sortOrder) FROM favorite_channels")
    suspend fun getMaxSortOrder(): Int?
}

/**
 * Data access object for watch history
 */
@Dao
interface WatchHistoryDao {

    @Query("SELECT * FROM watch_history ORDER BY lastWatchedAt DESC")
    fun getAllHistory(): Flow<List<WatchHistory>>

    @Query("SELECT * FROM watch_history ORDER BY lastWatchedAt DESC LIMIT :limit")
    fun getRecentHistory(limit: Int): Flow<List<WatchHistory>>

    @Query("SELECT * FROM watch_history WHERE streamType = :streamType ORDER BY lastWatchedAt DESC")
    fun getHistoryByType(streamType: String): Flow<List<WatchHistory>>

    @Query("SELECT * FROM watch_history WHERE streamId = :streamId LIMIT 1")
    suspend fun getHistoryItem(streamId: Int): WatchHistory?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addOrUpdateHistory(history: WatchHistory)

    @Delete
    suspend fun removeHistory(history: WatchHistory)

    @Query("DELETE FROM watch_history WHERE streamId = :streamId")
    suspend fun removeHistoryById(streamId: Int)

    @Query("DELETE FROM watch_history")
    suspend fun clearAllHistory()

    @Query("DELETE FROM watch_history WHERE lastWatchedAt < :timestamp")
    suspend fun clearHistoryOlderThan(timestamp: Long)

    @Query("SELECT COUNT(*) FROM watch_history")
    fun getHistoryCount(): Flow<Int>

    // For VOD/Series resume playback
    @Query("SELECT lastPositionMs FROM watch_history WHERE streamId = :streamId")
    suspend fun getLastPosition(streamId: Int): Long?

    @Query("UPDATE watch_history SET lastPositionMs = :positionMs, progressPercent = :progress WHERE streamId = :streamId")
    suspend fun updatePlaybackProgress(streamId: Int, positionMs: Long, progress: Float)
}

/**
 * Data access object for hidden channels
 */
@Dao
interface HiddenChannelDao {

    @Query("SELECT * FROM hidden_channels")
    fun getAllHidden(): Flow<List<HiddenChannel>>

    @Query("SELECT streamId FROM hidden_channels")
    fun getHiddenIds(): Flow<List<Int>>

    @Query("SELECT EXISTS(SELECT 1 FROM hidden_channels WHERE streamId = :streamId)")
    fun isHidden(streamId: Int): Flow<Boolean>

    @Query("SELECT EXISTS(SELECT 1 FROM hidden_channels WHERE streamId = :streamId)")
    suspend fun isHiddenSync(streamId: Int): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun hideChannel(hidden: HiddenChannel)

    @Query("DELETE FROM hidden_channels WHERE streamId = :streamId")
    suspend fun unhideChannel(streamId: Int)

    @Query("DELETE FROM hidden_channels")
    suspend fun unhideAll()
}

/**
 * Data access object for custom channel groups
 */
@Dao
interface ChannelGroupDao {

    @Query("SELECT * FROM channel_groups ORDER BY sortOrder ASC")
    fun getAllGroups(): Flow<List<ChannelGroup>>

    @Query("SELECT * FROM channel_groups WHERE id = :groupId")
    suspend fun getGroup(groupId: Int): ChannelGroup?

    @Insert
    suspend fun createGroup(group: ChannelGroup): Long

    @Update
    suspend fun updateGroup(group: ChannelGroup)

    @Delete
    suspend fun deleteGroup(group: ChannelGroup)

    @Query("DELETE FROM channel_groups WHERE id = :groupId")
    suspend fun deleteGroupById(groupId: Int)
}

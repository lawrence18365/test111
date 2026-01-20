package com.google.jetstream.data.repositories

import com.google.jetstream.data.local.FavoriteChannel
import com.google.jetstream.data.local.WatchHistory
import kotlinx.coroutines.flow.Flow

interface FavoritesRepository {
    fun getAllFavorites(): Flow<List<FavoriteChannel>>
    fun getFavoritesByType(streamType: String): Flow<List<FavoriteChannel>>
    fun isFavorite(streamId: Int): Flow<Boolean>
    suspend fun toggleFavorite(favorite: FavoriteChannel)
    
    // History methods
    fun getRecentHistory(limit: Int): Flow<List<WatchHistory>>
    suspend fun addToHistory(
        streamId: Int,
        name: String,
        streamIcon: String?,
        streamType: String
    )
    suspend fun updatePlaybackProgress(streamId: Int, positionMs: Long, durationMs: Long)
    suspend fun getLastPosition(streamId: Int): Long?
}
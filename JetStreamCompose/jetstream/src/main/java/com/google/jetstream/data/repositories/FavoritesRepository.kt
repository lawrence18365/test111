package com.google.jetstream.data.repositories

import com.google.jetstream.data.local.FavoriteChannel
import kotlinx.coroutines.flow.Flow

interface FavoritesRepository {
    fun getAllFavorites(): Flow<List<FavoriteChannel>>
    fun getFavoritesByType(streamType: String): Flow<List<FavoriteChannel>>
    fun isFavorite(streamId: Int): Flow<Boolean>
    suspend fun toggleFavorite(favorite: FavoriteChannel)
}
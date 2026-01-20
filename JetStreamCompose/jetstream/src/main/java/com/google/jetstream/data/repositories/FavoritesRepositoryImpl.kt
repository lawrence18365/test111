package com.google.jetstream.data.repositories

import com.google.jetstream.data.local.FavoriteChannel
import com.google.jetstream.data.local.FavoriteChannelDao
import com.google.jetstream.data.local.WatchHistory
import com.google.jetstream.data.local.WatchHistoryDao
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

@Singleton
class FavoritesRepositoryImpl @Inject constructor(
    private val favoriteChannelDao: FavoriteChannelDao,
    private val watchHistoryDao: WatchHistoryDao
) : FavoritesRepository {

    override fun getAllFavorites(): Flow<List<FavoriteChannel>> {
        return favoriteChannelDao.getAllFavorites()
    }

    override fun getFavoritesByType(streamType: String): Flow<List<FavoriteChannel>> {
        return favoriteChannelDao.getFavoritesByType(streamType)
    }

    override fun isFavorite(streamId: Int): Flow<Boolean> {
        return favoriteChannelDao.isFavorite(streamId)
    }

    override suspend fun toggleFavorite(favorite: FavoriteChannel) {
        val isFav = favoriteChannelDao.isFavoriteSync(favorite.streamId)
        if (isFav) {
            favoriteChannelDao.removeFavoriteById(favorite.streamId)
        } else {
            favoriteChannelDao.addFavorite(favorite)
        }
    }

    override fun getRecentWatchHistory(limit: Int): Flow<List<WatchHistory>> {
        return watchHistoryDao.getRecentHistory(limit)
    }

    override suspend fun addToHistory(
        streamId: Int,
        name: String,
        streamIcon: String?,
        streamType: String
    ) {
        val history = WatchHistory(
            streamId = streamId,
            name = name,
            streamIcon = streamIcon,
            streamType = streamType,
            lastWatchedAt = System.currentTimeMillis()
        )
        watchHistoryDao.addOrUpdateHistory(history)
    }

    override suspend fun updatePlaybackProgress(streamId: Int, positionMs: Long, durationMs: Long) {
        val progress = if (durationMs > 0) positionMs.toFloat() / durationMs else 0f
        watchHistoryDao.updatePlaybackProgress(streamId, positionMs, progress)
    }

    override suspend fun getLastPosition(streamId: Int): Long? {
        return watchHistoryDao.getLastPosition(streamId)
    }
}

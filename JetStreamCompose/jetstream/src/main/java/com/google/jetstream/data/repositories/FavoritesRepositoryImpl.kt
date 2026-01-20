/*
 * Copyright 2024 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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

    override fun getRecentHistory(limit: Int): Flow<List<WatchHistory>> {
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

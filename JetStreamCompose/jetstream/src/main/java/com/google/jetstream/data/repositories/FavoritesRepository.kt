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

/*
 * Favorites Repository - Manages user's favorite channels
 */
package com.google.jetstream.data.repositories

import com.google.jetstream.data.local.FavoriteChannel
import com.google.jetstream.data.local.FavoriteChannelDao
import com.google.jetstream.data.local.HiddenChannel
import com.google.jetstream.data.local.HiddenChannelDao
import com.google.jetstream.data.local.WatchHistory
import com.google.jetstream.data.local.WatchHistoryDao
import com.google.jetstream.data.models.xtream.XtreamChannel
import com.google.jetstream.data.models.xtream.XtreamSeries
import com.google.jetstream.data.models.xtream.XtreamVodItem
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FavoritesRepository @Inject constructor(
    private val favoriteChannelDao: FavoriteChannelDao,
    private val watchHistoryDao: WatchHistoryDao,
    private val hiddenChannelDao: HiddenChannelDao
) {
    // ========== FAVORITES ==========

    val allFavorites: Flow<List<FavoriteChannel>> = favoriteChannelDao.getAllFavorites()

    val liveFavorites: Flow<List<FavoriteChannel>> = favoriteChannelDao.getFavoritesByType("live")
    val vodFavorites: Flow<List<FavoriteChannel>> = favoriteChannelDao.getFavoritesByType("vod")
    val seriesFavorites: Flow<List<FavoriteChannel>> = favoriteChannelDao.getFavoritesByType("series")

    val favoritesCount: Flow<Int> = favoriteChannelDao.getFavoritesCount()

    fun isFavorite(streamId: Int): Flow<Boolean> = favoriteChannelDao.isFavorite(streamId)

    suspend fun toggleFavorite(channel: XtreamChannel, categoryName: String? = null): Boolean {
        val isFav = favoriteChannelDao.isFavoriteSync(channel.streamId)
        if (isFav) {
            favoriteChannelDao.removeFavoriteById(channel.streamId)
            return false
        } else {
            val maxOrder = favoriteChannelDao.getMaxSortOrder() ?: 0
            favoriteChannelDao.addFavorite(
                FavoriteChannel(
                    streamId = channel.streamId,
                    name = channel.name,
                    streamIcon = channel.streamIcon,
                    categoryId = channel.categoryId,
                    categoryName = categoryName,
                    streamType = "live",
                    sortOrder = maxOrder + 1
                )
            )
            return true
        }
    }

    suspend fun toggleFavorite(vodItem: XtreamVodItem, categoryName: String? = null): Boolean {
        val isFav = favoriteChannelDao.isFavoriteSync(vodItem.streamId)
        if (isFav) {
            favoriteChannelDao.removeFavoriteById(vodItem.streamId)
            return false
        } else {
            val maxOrder = favoriteChannelDao.getMaxSortOrder() ?: 0
            favoriteChannelDao.addFavorite(
                FavoriteChannel(
                    streamId = vodItem.streamId,
                    name = vodItem.name,
                    streamIcon = vodItem.streamIcon,
                    categoryId = vodItem.categoryId,
                    categoryName = categoryName,
                    streamType = "vod",
                    sortOrder = maxOrder + 1
                )
            )
            return true
        }
    }

    suspend fun toggleFavorite(series: XtreamSeries, categoryName: String? = null): Boolean {
        val isFav = favoriteChannelDao.isFavoriteSync(series.seriesId)
        if (isFav) {
            favoriteChannelDao.removeFavoriteById(series.seriesId)
            return false
        } else {
            val maxOrder = favoriteChannelDao.getMaxSortOrder() ?: 0
            favoriteChannelDao.addFavorite(
                FavoriteChannel(
                    streamId = series.seriesId,
                    name = series.name,
                    streamIcon = series.cover,
                    categoryId = series.categoryId,
                    categoryName = categoryName,
                    streamType = "series",
                    sortOrder = maxOrder + 1
                )
            )
            return true
        }
    }

    suspend fun toggleFavorite(
        streamId: Int,
        name: String,
        streamIcon: String?,
        categoryName: String?,
        streamType: String
    ): Boolean {
        val isFav = favoriteChannelDao.isFavoriteSync(streamId)
        if (isFav) {
            favoriteChannelDao.removeFavoriteById(streamId)
            return false
        }

        val maxOrder = favoriteChannelDao.getMaxSortOrder() ?: 0
        favoriteChannelDao.addFavorite(
            FavoriteChannel(
                streamId = streamId,
                name = name,
                streamIcon = streamIcon,
                categoryId = null,
                categoryName = categoryName,
                streamType = streamType,
                sortOrder = maxOrder + 1
            )
        )
        return true
    }

    suspend fun removeFavorite(streamId: Int) {
        favoriteChannelDao.removeFavoriteById(streamId)
    }

    // ========== WATCH HISTORY ==========

    val allHistory: Flow<List<WatchHistory>> = watchHistoryDao.getAllHistory()

    fun getRecentHistory(limit: Int = 20): Flow<List<WatchHistory>> =
        watchHistoryDao.getRecentHistory(limit)

    val liveHistory: Flow<List<WatchHistory>> = watchHistoryDao.getHistoryByType("live")
    val vodHistory: Flow<List<WatchHistory>> = watchHistoryDao.getHistoryByType("vod")

    val historyCount: Flow<Int> = watchHistoryDao.getHistoryCount()

    suspend fun addToHistory(channel: XtreamChannel) {
        watchHistoryDao.addOrUpdateHistory(
            WatchHistory(
                streamId = channel.streamId,
                name = channel.name,
                streamIcon = channel.streamIcon,
                streamType = "live"
            )
        )
    }

    suspend fun addToHistory(vodItem: XtreamVodItem) {
        watchHistoryDao.addOrUpdateHistory(
            WatchHistory(
                streamId = vodItem.streamId,
                name = vodItem.name,
                streamIcon = vodItem.streamIcon,
                streamType = "vod"
            )
        )
    }

    suspend fun addToHistory(
        streamId: Int,
        name: String,
        streamIcon: String?,
        streamType: String
    ) {
        watchHistoryDao.addOrUpdateHistory(
            WatchHistory(
                streamId = streamId,
                name = name,
                streamIcon = streamIcon,
                streamType = streamType
            )
        )
    }

    suspend fun updatePlaybackProgress(streamId: Int, positionMs: Long, durationMs: Long) {
        val progress = if (durationMs > 0) positionMs.toFloat() / durationMs else 0f
        watchHistoryDao.updatePlaybackProgress(streamId, positionMs, progress)
    }

    suspend fun getLastPosition(streamId: Int): Long? {
        return watchHistoryDao.getLastPosition(streamId)
    }

    suspend fun clearHistory() {
        watchHistoryDao.clearAllHistory()
    }

    suspend fun removeFromHistory(streamId: Int) {
        watchHistoryDao.removeHistoryById(streamId)
    }

    // Clear history older than 30 days
    suspend fun clearOldHistory() {
        val thirtyDaysAgo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000)
        watchHistoryDao.clearHistoryOlderThan(thirtyDaysAgo)
    }

    // ========== HIDDEN CHANNELS ==========

    val hiddenChannelIds: Flow<List<Int>> = hiddenChannelDao.getHiddenIds()

    fun isHidden(streamId: Int): Flow<Boolean> = hiddenChannelDao.isHidden(streamId)

    suspend fun hideChannel(streamId: Int, streamType: String) {
        hiddenChannelDao.hideChannel(HiddenChannel(streamId, streamType))
    }

    suspend fun unhideChannel(streamId: Int) {
        hiddenChannelDao.unhideChannel(streamId)
    }

    suspend fun unhideAll() {
        hiddenChannelDao.unhideAll()
    }
}

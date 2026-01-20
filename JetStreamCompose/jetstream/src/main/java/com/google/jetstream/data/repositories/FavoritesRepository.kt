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

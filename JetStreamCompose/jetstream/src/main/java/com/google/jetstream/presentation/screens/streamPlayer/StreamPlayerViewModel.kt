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

package com.google.jetstream.presentation.screens.streamPlayer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.jetstream.data.models.xtream.XtreamChannel
import com.google.jetstream.data.repositories.FavoritesRepository
import com.google.jetstream.data.repositories.xtream.XtreamRepository
import com.google.jetstream.data.repositories.xtream.XtreamResult
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

@HiltViewModel
class StreamPlayerViewModel @Inject constructor(
    private val favoritesRepository: FavoritesRepository,
    private val xtreamRepository: XtreamRepository
) : ViewModel() {

    private var cachedChannels: List<XtreamChannel> = emptyList()
    private var channelsLoaded = false

    fun isFavorite(streamId: Int): Flow<Boolean> {
        if (streamId <= 0) return flowOf(false)
        return favoritesRepository.isFavorite(streamId)
    }

    fun toggleFavorite(streamArgs: StreamPlayerArgs) {
        if (streamArgs.streamId <= 0) return
        viewModelScope.launch {
            val fav = com.google.jetstream.data.local.FavoriteChannel(
                streamId = streamArgs.streamId,
                name = streamArgs.streamName,
                streamIcon = streamArgs.streamIcon,
                categoryId = null, // Not passed in args usually, logic could be improved
                categoryName = streamArgs.categoryName,
                streamType = streamArgs.streamType
            )
            favoritesRepository.toggleFavorite(fav)
        }
    }

    fun recordPlaybackStart(streamArgs: StreamPlayerArgs) {
        if (streamArgs.streamId <= 0) return
        viewModelScope.launch {
            favoritesRepository.addToHistory(
                streamId = streamArgs.streamId,
                name = streamArgs.streamName,
                streamIcon = streamArgs.streamIcon,
                streamType = streamArgs.streamType
            )
        }
    }

    fun updatePlaybackProgress(streamId: Int, positionMs: Long, durationMs: Long) {
        if (streamId <= 0 || durationMs <= 0L) return
        viewModelScope.launch {
            favoritesRepository.updatePlaybackProgress(streamId, positionMs, durationMs)
        }
    }

    suspend fun getLastPosition(streamId: Int): Long? {
        if (streamId <= 0) return null
        return favoritesRepository.getLastPosition(streamId)
    }

    suspend fun buildStreamArgsForChannelNumber(channelNumber: Int): StreamPlayerArgs? {
        if (channelNumber <= 0) return null
        ensureChannelsLoaded()

        val channel = cachedChannels.firstOrNull { it.num == channelNumber }
            ?: cachedChannels.getOrNull(channelNumber - 1)
            ?: return null

        val streamUrl = xtreamRepository.buildLiveStreamUrl(channel.streamId) ?: return null
        return StreamPlayerArgs(
            streamUrl = streamUrl,
            streamName = channel.name,
            streamId = channel.streamId,
            streamType = StreamTypes.LIVE,
            streamIcon = channel.streamIcon,
            channelNumber = channel.num ?: channelNumber
        )
    }

    private suspend fun ensureChannelsLoaded() {
        if (channelsLoaded) return
        when (val result = xtreamRepository.getLiveStreams()) {
            is XtreamResult.Success -> {
                cachedChannels = result.data
                channelsLoaded = true
            }
            else -> {}
        }
    }
}

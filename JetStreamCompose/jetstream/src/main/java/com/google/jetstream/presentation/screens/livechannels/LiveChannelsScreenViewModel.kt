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

package com.google.jetstream.presentation.screens.livechannels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.jetstream.data.local.WatchHistory
import com.google.jetstream.data.models.xtream.XtreamCategory
import com.google.jetstream.data.models.xtream.XtreamChannel
import com.google.jetstream.data.repositories.FavoritesRepository
import com.google.jetstream.data.repositories.xtream.XtreamRepository
import com.google.jetstream.data.repositories.xtream.XtreamResult
import com.google.jetstream.presentation.utils.CountryFilter
import com.google.jetstream.presentation.utils.DefaultCountryFilters
import com.google.jetstream.presentation.utils.categoryIdsForCountry
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface LiveChannelsUiState {
    data object Loading : LiveChannelsUiState
    data class Ready(
        val categories: List<XtreamCategory>,
        val channels: List<XtreamChannel>,
        val selectedCountry: CountryFilter
    ) : LiveChannelsUiState
    data class Error(val message: String) : LiveChannelsUiState
}

@HiltViewModel
class LiveChannelsScreenViewModel @Inject constructor(
    private val xtreamRepository: XtreamRepository,
    private val favoritesRepository: FavoritesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<LiveChannelsUiState>(LiveChannelsUiState.Loading)
    val uiState: StateFlow<LiveChannelsUiState> = _uiState.asStateFlow()
    val recentHistory: Flow<List<WatchHistory>> = favoritesRepository.getRecentHistory(12)

    private var allChannels: List<XtreamChannel> = emptyList()
    private var categories: List<XtreamCategory> = emptyList()
    private val defaultCountry = DefaultCountryFilters.first()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            _uiState.value = LiveChannelsUiState.Loading

            // Load categories and channels in parallel
            val categoriesResult = xtreamRepository.getLiveCategories()
            val channelsResult = xtreamRepository.getLiveStreams()

            when {
                categoriesResult is XtreamResult.Success &&
                    channelsResult is XtreamResult.Success -> {
                    categories = categoriesResult.data
                    allChannels = channelsResult.data
                    val filteredChannels = filterChannelsByCountry(defaultCountry)
                    _uiState.value = LiveChannelsUiState.Ready(
                        categories = categories,
                        channels = filteredChannels,
                        selectedCountry = defaultCountry
                    )
                }
                categoriesResult is XtreamResult.Error -> {
                    _uiState.value = LiveChannelsUiState.Error(categoriesResult.message)
                }
                channelsResult is XtreamResult.Error -> {
                    _uiState.value = LiveChannelsUiState.Error(channelsResult.message)
                }
                else -> {
                    _uiState.value = LiveChannelsUiState.Error("Unknown error")
                }
            }
        }
    }

    fun selectCountry(country: CountryFilter) {
        val currentState = _uiState.value
        if (currentState is LiveChannelsUiState.Ready) {
            val filteredChannels = filterChannelsByCountry(country)
            _uiState.value = currentState.copy(
                channels = filteredChannels,
                selectedCountry = country
            )
        }
    }

    suspend fun getStreamUrl(channel: XtreamChannel): String? {
        return xtreamRepository.buildLiveStreamUrl(channel.streamId)
    }

    suspend fun getStreamUrl(streamId: Int): String? {
        return xtreamRepository.buildLiveStreamUrl(streamId)
    }

    fun refresh() {
        loadData()
    }

    private fun filterChannelsByCountry(country: CountryFilter): List<XtreamChannel> {
        val categoryIds = categoryIdsForCountry(categories, country)
        if (categoryIds.isEmpty()) {
            return emptyList()
        }
        return allChannels.filter { channel ->
            val matchesPrimary = channel.categoryId?.let { categoryIds.contains(it) } == true
            val matchesAny = channel.categoryIds?.any { categoryIds.contains(it.toString()) } == true
            matchesPrimary || matchesAny
        }
    }
}

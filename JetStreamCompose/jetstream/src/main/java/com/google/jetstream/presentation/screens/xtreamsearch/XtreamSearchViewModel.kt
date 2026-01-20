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

package com.google.jetstream.presentation.screens.xtreamsearch

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.jetstream.data.models.xtream.XtreamChannel
import com.google.jetstream.data.models.xtream.XtreamSeries
import com.google.jetstream.data.models.xtream.XtreamVodItem
import com.google.jetstream.data.repositories.xtream.XtreamRepository
import com.google.jetstream.data.repositories.xtream.XtreamResult
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

sealed class SearchResult {
    data class Channel(val channel: XtreamChannel) : SearchResult()
    data class Vod(val vod: XtreamVodItem) : SearchResult()
    data class Series(val series: XtreamSeries) : SearchResult()
}

enum class SearchFilter {
    ALL, LIVE, MOVIES, SERIES
}

data class SearchUiState(
    val query: String = "",
    val isLoading: Boolean = false,
    val isLoadingData: Boolean = false,
    // Split results
    val liveResults: List<SearchResult.Channel> = emptyList(),
    val movieResults: List<SearchResult.Vod> = emptyList(),
    val seriesResults: List<SearchResult.Series> = emptyList(),
    val filter: SearchFilter = SearchFilter.ALL,
    val error: String? = null,
    val channelsLoaded: Boolean = false,
    val vodLoaded: Boolean = false,
    val seriesLoaded: Boolean = false,
    val channelsError: String? = null,
    val vodError: String? = null,
    val seriesError: String? = null
) {
    val hasResults: Boolean
        get() = liveResults.isNotEmpty() || movieResults.isNotEmpty() || seriesResults.isNotEmpty()
}

@OptIn(FlowPreview::class)
@HiltViewModel
class XtreamSearchViewModel @Inject constructor(
    private val xtreamRepository: XtreamRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")

    // Cached data for searching - loaded on demand
    private var cachedChannels: List<XtreamChannel>? = null
    private var cachedVod: List<XtreamVodItem>? = null
    private var cachedSeries: List<XtreamSeries>? = null

    // Mutex for thread-safe cache access
    private val cacheMutex = Mutex()

    init {
        // Debounce search queries
        viewModelScope.launch {
            _searchQuery
                .debounce(300)
                .distinctUntilChanged()
                .filter { it.length >= 3 || it.isEmpty() }
                .collect { query ->
                    if (query.isEmpty()) {
                        _uiState.value = _uiState.value.copy(
                            query = "",
                            liveResults = emptyList(),
                            movieResults = emptyList(),
                            seriesResults = emptyList(),
                            isLoading = false,
                            error = null
                        )
                    } else {
                        performSearch(query)
                    }
                }
        }
    }

    fun updateQuery(query: String) {
        _uiState.value = _uiState.value.copy(query = query)
        _searchQuery.value = query
    }

    fun setFilter(filter: SearchFilter) {
        _uiState.value = _uiState.value.copy(filter = filter)
        if (_uiState.value.query.isNotEmpty()) {
            performSearch(_uiState.value.query)
        }
    }

    private fun sanitize(input: String): String {
        // Lowercase and remove all non-alphanumeric characters
        return input.lowercase().replace(Regex("[^a-z0-9]"), "")
    }

    private fun performSearch(query: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, query = query, error = null)

            val filter = _uiState.value.filter

            // Load required data based on filter (on-demand)
            val dataLoadNeeded = when (filter) {
                SearchFilter.ALL ->
                    cachedChannels == null ||
                        cachedVod == null ||
                        cachedSeries == null
                SearchFilter.LIVE -> cachedChannels == null
                SearchFilter.MOVIES -> cachedVod == null
                SearchFilter.SERIES -> cachedSeries == null
            }

            if (dataLoadNeeded) {
                _uiState.value = _uiState.value.copy(isLoadingData = true)
                loadRequiredData(filter)
                _uiState.value = _uiState.value.copy(isLoadingData = false)
            }

            // Perform the actual search
            val queryClean = sanitize(query)

            // Temporary lists
            val liveList = mutableListOf<SearchResult.Channel>()
            val movieList = mutableListOf<SearchResult.Vod>()
            val seriesList = mutableListOf<SearchResult.Series>()

            cacheMutex.withLock {
                // Search channels
                if (filter == SearchFilter.ALL || filter == SearchFilter.LIVE) {
                    cachedChannels?.asSequence()
                        ?.filter { sanitize(it.name).contains(queryClean) }
                        ?.take(50)
                        ?.forEach { liveList.add(SearchResult.Channel(it)) }
                }

                // Search VOD
                if (filter == SearchFilter.ALL || filter == SearchFilter.MOVIES) {
                    cachedVod?.asSequence()
                        ?.filter { sanitize(it.name).contains(queryClean) }
                        ?.take(50)
                        ?.forEach { movieList.add(SearchResult.Vod(it)) }
                }

                // Search Series
                if (filter == SearchFilter.ALL || filter == SearchFilter.SERIES) {
                    cachedSeries?.asSequence()
                        ?.filter {
                            sanitize(it.name).contains(queryClean)
                        }
                        ?.take(50)
                        ?.forEach { seriesList.add(SearchResult.Series(it)) }
                }
            }

            _uiState.value = _uiState.value.copy(
                liveResults = liveList,
                movieResults = movieList,
                seriesResults = seriesList,
                isLoading = false
            )
        }
    }

    private suspend fun loadRequiredData(filter: SearchFilter) {
        cacheMutex.withLock {
            when (filter) {
                SearchFilter.ALL -> {
                    // Load all in parallel
                    val channelsDeferred = viewModelScope.async { loadChannelsIfNeeded() }
                    val vodDeferred = viewModelScope.async { loadVodIfNeeded() }
                    val seriesDeferred = viewModelScope.async { loadSeriesIfNeeded() }
                    channelsDeferred.await()
                    vodDeferred.await()
                    seriesDeferred.await()
                }
                SearchFilter.LIVE -> loadChannelsIfNeeded()
                SearchFilter.MOVIES -> loadVodIfNeeded()
                SearchFilter.SERIES -> loadSeriesIfNeeded()
            }
        }
    }

    private suspend fun loadChannelsIfNeeded() {
        if (cachedChannels != null) return

        when (val result = xtreamRepository.getLiveStreams()) {
            is XtreamResult.Success -> {
                cachedChannels = result.data
                _uiState.value = _uiState.value.copy(
                    channelsLoaded = true,
                    channelsError = null
                )
            }
            is XtreamResult.Error -> {
                _uiState.value = _uiState.value.copy(
                    channelsError = result.message
                )
            }
            else -> {}
        }
    }

    private suspend fun loadVodIfNeeded() {
        if (cachedVod != null) return

        when (val result = xtreamRepository.getVodStreams()) {
            is XtreamResult.Success -> {
                cachedVod = result.data
                _uiState.value = _uiState.value.copy(
                    vodLoaded = true,
                    vodError = null
                )
            }
            is XtreamResult.Error -> {
                _uiState.value = _uiState.value.copy(
                    vodError = result.message
                )
            }
            else -> {}
        }
    }

    private suspend fun loadSeriesIfNeeded() {
        if (cachedSeries != null) return

        when (val result = xtreamRepository.getSeries()) {
            is XtreamResult.Success -> {
                cachedSeries = result.data
                _uiState.value = _uiState.value.copy(
                    seriesLoaded = true,
                    seriesError = null
                )
            }
            is XtreamResult.Error -> {
                _uiState.value = _uiState.value.copy(
                    seriesError = result.message
                )
            }
            else -> {}
        }
    }

    suspend fun getChannelStreamUrl(channel: XtreamChannel): String? {
        return xtreamRepository.buildLiveStreamUrl(channel.streamId)
    }

    suspend fun getVodStreamUrl(vod: XtreamVodItem): String? {
        val extension = vod.containerExtension ?: "mp4"
        return xtreamRepository.buildVodStreamUrl(vod.streamId, extension)
    }

    fun clearSearch() {
        _uiState.value = _uiState.value.copy(
            query = "",
            liveResults = emptyList(),
            movieResults = emptyList(),
            seriesResults = emptyList(),
            isLoading = false,
            error = null
        )
        _searchQuery.value = ""
    }

    fun retry() {
        // Clear cache to force reload
        viewModelScope.launch {
            cacheMutex.withLock {
                if (_uiState.value.channelsError != null) cachedChannels = null
                if (_uiState.value.vodError != null) cachedVod = null
                if (_uiState.value.seriesError != null) cachedSeries = null
            }
            _uiState.value = _uiState.value.copy(
                channelsError = null,
                vodError = null,
                seriesError = null
            )
            if (_uiState.value.query.isNotEmpty()) {
                performSearch(_uiState.value.query)
            }
        }
    }
}

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
import com.google.jetstream.data.models.xtream.XtreamCategory
import com.google.jetstream.data.models.xtream.XtreamChannel
import com.google.jetstream.data.models.xtream.XtreamSeries
import com.google.jetstream.data.models.xtream.XtreamVodItem
import com.google.jetstream.data.repositories.xtream.XtreamRepository
import com.google.jetstream.data.repositories.xtream.XtreamResult
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

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

    // Cached categories for category name matching
    private var cachedLiveCategories: List<XtreamCategory>? = null
    private var cachedVodCategories: List<XtreamCategory>? = null
    private var cachedSeriesCategories: List<XtreamCategory>? = null

    // Category ID to name maps for fast lookup
    private var liveCategoryMap: Map<String, String> = emptyMap()
    private var vodCategoryMap: Map<String, String> = emptyMap()
    private var seriesCategoryMap: Map<String, String> = emptyMap()

    // Mutex for thread-safe cache access
    private val cacheMutex = Mutex()
    private var searchJob: Job? = null

    init {
        // Debounce search queries
        viewModelScope.launch {
            _searchQuery
                .debounce(300)
                .distinctUntilChanged()
                .filter { it.length >= 3 || it.isEmpty() }
                .collect { query ->
                    if (query.isEmpty()) {
                        searchJob?.cancel()
                        _uiState.value = _uiState.value.copy(
                            query = "",
                            liveResults = emptyList(),
                            movieResults = emptyList(),
                            seriesResults = emptyList(),
                            isLoading = false,
                            error = null
                        )
                    } else {
                        triggerSearch(query)
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
            triggerSearch(_uiState.value.query)
        }
    }

    private fun sanitize(input: String): String {
        // Lowercase and remove all non-alphanumeric characters
        return input.lowercase().replace(Regex("[^a-z0-9]"), "")
    }

    /**
     * Check if any of the provided fields contain the search query.
     * Returns true if at least one field matches.
     */
    private fun matchesAnyField(
        queryClean: String,
        vararg fields: String?
    ): Boolean {
        return fields.any { field ->
            field != null && sanitize(field).contains(queryClean)
        }
    }

    /**
     * Check if a channel matches the search query.
     * Searches: name, EPG channel ID, category name
     */
    private fun channelMatchesQuery(channel: XtreamChannel, queryClean: String): Boolean {
        // Get category name for this channel
        val categoryName = channel.categoryId?.let { liveCategoryMap[it] }
        return matchesAnyField(
            queryClean,
            channel.name,
            channel.epgChannelId,
            categoryName
        )
    }

    /**
     * Check if a VOD item matches the search query.
     * Searches: name, category name
     * Note: VOD basic listing doesn't include plot/cast - would require separate API call
     */
    private fun vodMatchesQuery(vod: XtreamVodItem, queryClean: String): Boolean {
        // Get category name for this VOD item
        val categoryName = vod.categoryId?.let { vodCategoryMap[it] }
        return matchesAnyField(
            queryClean,
            vod.name,
            categoryName
        )
    }

    /**
     * Check if a series matches the search query.
     * Searches: name, plot, cast, director, genre, category name
     */
    private fun seriesMatchesQuery(series: XtreamSeries, queryClean: String): Boolean {
        // Get category name for this series
        val categoryName = series.categoryId?.let { seriesCategoryMap[it] }
        return matchesAnyField(
            queryClean,
            series.name,
            series.plot,
            series.cast,
            series.director,
            series.genre,
            categoryName
        )
    }

    private fun triggerSearch(query: String) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            performSearch(query)
        }
    }

    private suspend fun performSearch(query: String) {
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
            withContext(Dispatchers.IO) {
                loadRequiredData(filter)
            }
            _uiState.value = _uiState.value.copy(isLoadingData = false)
        }

        // Perform the actual search
        val queryClean = sanitize(query)

        val (channelsSnapshot, vodSnapshot, seriesSnapshot) = cacheMutex.withLock {
            Triple(cachedChannels, cachedVod, cachedSeries)
        }

        val (liveList, movieList, seriesList) = withContext(Dispatchers.Default) {
            // Temporary lists
            val live = mutableListOf<SearchResult.Channel>()
            val movies = mutableListOf<SearchResult.Vod>()
            val series = mutableListOf<SearchResult.Series>()

            // Search channels (name, EPG channel ID, category name)
            if (filter == SearchFilter.ALL || filter == SearchFilter.LIVE) {
                channelsSnapshot?.asSequence()
                    ?.filter { channelMatchesQuery(it, queryClean) }
                    ?.take(50)
                    ?.forEach { live.add(SearchResult.Channel(it)) }
            }

            // Search VOD (name, category name)
            if (filter == SearchFilter.ALL || filter == SearchFilter.MOVIES) {
                vodSnapshot?.asSequence()
                    ?.filter { vodMatchesQuery(it, queryClean) }
                    ?.take(50)
                    ?.forEach { movies.add(SearchResult.Vod(it)) }
            }

            // Search Series (name, plot, cast, director, genre, category name)
            if (filter == SearchFilter.ALL || filter == SearchFilter.SERIES) {
                seriesSnapshot?.asSequence()
                    ?.filter { seriesMatchesQuery(it, queryClean) }
                    ?.take(50)
                    ?.forEach { series.add(SearchResult.Series(it)) }
            }

            Triple(live, movies, series)
        }

        _uiState.value = _uiState.value.copy(
            liveResults = liveList,
            movieResults = movieList,
            seriesResults = seriesList,
            isLoading = false
        )
    }

    private suspend fun loadRequiredData(filter: SearchFilter) {
        when (filter) {
            SearchFilter.ALL -> {
                // Load all in parallel
                coroutineScope {
                    val channelsDeferred = async(Dispatchers.IO) { loadChannelsIfNeeded() }
                    val vodDeferred = async(Dispatchers.IO) { loadVodIfNeeded() }
                    val seriesDeferred = async(Dispatchers.IO) { loadSeriesIfNeeded() }
                    channelsDeferred.await()
                    vodDeferred.await()
                    seriesDeferred.await()
                }
            }
            SearchFilter.LIVE -> loadChannelsIfNeeded()
            SearchFilter.MOVIES -> loadVodIfNeeded()
            SearchFilter.SERIES -> loadSeriesIfNeeded()
        }
    }

    private suspend fun loadChannelsIfNeeded() {
        if (cachedChannels != null) return

        // Load categories first for category name matching
        if (cachedLiveCategories == null) {
            when (val catResult = xtreamRepository.getLiveCategories()) {
                is XtreamResult.Success -> {
                    cachedLiveCategories = catResult.data
                    liveCategoryMap = catResult.data.associate { it.categoryId to it.categoryName }
                }
                else -> {} // Continue even if categories fail
            }
        }

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

        // Load categories first for category name matching
        if (cachedVodCategories == null) {
            when (val catResult = xtreamRepository.getVodCategories()) {
                is XtreamResult.Success -> {
                    cachedVodCategories = catResult.data
                    vodCategoryMap = catResult.data.associate { it.categoryId to it.categoryName }
                }
                else -> {} // Continue even if categories fail
            }
        }

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

        // Load categories first for category name matching
        if (cachedSeriesCategories == null) {
            when (val catResult = xtreamRepository.getSeriesCategories()) {
                is XtreamResult.Success -> {
                    cachedSeriesCategories = catResult.data
                    seriesCategoryMap =
                        catResult.data.associate {
                            it.categoryId to it.categoryName
                        }
                }
                else -> {} // Continue even if categories fail
            }
        }

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
                if (_uiState.value.channelsError != null) {
                    cachedChannels = null
                    cachedLiveCategories = null
                    liveCategoryMap = emptyMap()
                }
                if (_uiState.value.vodError != null) {
                    cachedVod = null
                    cachedVodCategories = null
                    vodCategoryMap = emptyMap()
                }
                if (_uiState.value.seriesError != null) {
                    cachedSeries = null
                    cachedSeriesCategories = null
                    seriesCategoryMap = emptyMap()
                }
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

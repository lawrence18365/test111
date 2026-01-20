/*
 * Xtream Search Screen ViewModel - Search across all content types
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
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import javax.inject.Inject

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
    val results: List<SearchResult> = emptyList(),
    val filter: SearchFilter = SearchFilter.ALL,
    val recentSearches: List<String> = emptyList(),
    val error: String? = null,
    val totalChannels: Int = 0,
    val totalVod: Int = 0,
    val totalSeries: Int = 0
)

@OptIn(FlowPreview::class)
@HiltViewModel
class XtreamSearchViewModel @Inject constructor(
    private val xtreamRepository: XtreamRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")

    // Cached data for faster searching
    private var allChannels: List<XtreamChannel> = emptyList()
    private var allVod: List<XtreamVodItem> = emptyList()
    private var allSeries: List<XtreamSeries> = emptyList()
    private var dataLoaded = false

    init {
        // Debounce search queries
        viewModelScope.launch {
            _searchQuery
                .debounce(300)
                .distinctUntilChanged()
                .filter { it.length >= 2 || it.isEmpty() }
                .collect { query ->
                    if (query.isEmpty()) {
                        _uiState.value = _uiState.value.copy(
                            query = "",
                            results = emptyList(),
                            isLoading = false
                        )
                    } else {
                        performSearch(query)
                    }
                }
        }

        // Load all data in background
        loadAllData()
    }

    private fun loadAllData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            // Load channels
            when (val result = xtreamRepository.getLiveStreams()) {
                is XtreamResult.Success -> {
                    allChannels = result.data
                    _uiState.value = _uiState.value.copy(totalChannels = result.data.size)
                }
                is XtreamResult.Error -> { /* ignore */ }
                else -> {}
            }

            // Load VOD
            when (val result = xtreamRepository.getVodStreams()) {
                is XtreamResult.Success -> {
                    allVod = result.data
                    _uiState.value = _uiState.value.copy(totalVod = result.data.size)
                }
                is XtreamResult.Error -> { /* ignore */ }
                else -> {}
            }

            // Load Series
            when (val result = xtreamRepository.getSeries()) {
                is XtreamResult.Success -> {
                    allSeries = result.data
                    _uiState.value = _uiState.value.copy(totalSeries = result.data.size)
                }
                is XtreamResult.Error -> { /* ignore */ }
                else -> {}
            }

            dataLoaded = true
            _uiState.value = _uiState.value.copy(isLoading = false)

            // If there's a pending query, search now
            if (_uiState.value.query.isNotEmpty()) {
                performSearch(_uiState.value.query)
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

    private fun performSearch(query: String) {
        if (!dataLoaded) return

        _uiState.value = _uiState.value.copy(isLoading = true, query = query)

        val queryLower = query.lowercase()
        val filter = _uiState.value.filter
        val results = mutableListOf<SearchResult>()

        // Search channels
        if (filter == SearchFilter.ALL || filter == SearchFilter.LIVE) {
            allChannels
                .filter { it.name.lowercase().contains(queryLower) }
                .take(50)
                .forEach { results.add(SearchResult.Channel(it)) }
        }

        // Search VOD
        if (filter == SearchFilter.ALL || filter == SearchFilter.MOVIES) {
            allVod
                .filter { it.name.lowercase().contains(queryLower) }
                .take(50)
                .forEach { results.add(SearchResult.Vod(it)) }
        }

        // Search Series
        if (filter == SearchFilter.ALL || filter == SearchFilter.SERIES) {
            allSeries
                .filter {
                    it.name.lowercase().contains(queryLower) ||
                    it.genre?.lowercase()?.contains(queryLower) == true
                }
                .take(50)
                .forEach { results.add(SearchResult.Series(it)) }
        }

        // Sort by relevance (exact matches first)
        val sortedResults = results.sortedByDescending { result ->
            val name = when (result) {
                is SearchResult.Channel -> result.channel.name
                is SearchResult.Vod -> result.vod.name
                is SearchResult.Series -> result.series.name
            }.lowercase()

            when {
                name == queryLower -> 3
                name.startsWith(queryLower) -> 2
                else -> 1
            }
        }

        _uiState.value = _uiState.value.copy(
            results = sortedResults,
            isLoading = false,
            error = null
        )
    }

    suspend fun getChannelStreamUrl(channel: XtreamChannel): String? {
        return xtreamRepository.buildLiveStreamUrl(channel.streamId)
    }

    suspend fun getVodStreamUrl(vod: XtreamVodItem): String? {
        val extension = vod.containerExtension ?: "mp4"
        return xtreamRepository.buildVodStreamUrl(vod.streamId, extension)
    }

    fun clearSearch() {
        _uiState.value = SearchUiState()
        _searchQuery.value = ""
    }
}

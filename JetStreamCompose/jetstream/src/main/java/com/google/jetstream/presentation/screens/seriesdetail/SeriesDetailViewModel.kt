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

package com.google.jetstream.presentation.screens.seriesdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.jetstream.data.models.xtream.XtreamEpisode
import com.google.jetstream.data.models.xtream.XtreamSeason
import com.google.jetstream.data.models.xtream.XtreamSeriesInfo
import com.google.jetstream.data.repositories.xtream.XtreamRepository
import com.google.jetstream.data.repositories.xtream.XtreamResult
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface SeriesDetailUiState {
    data object Loading : SeriesDetailUiState
    data class Ready(
        val seriesInfo: XtreamSeriesInfo,
        val seasons: List<XtreamSeason>,
        val selectedSeason: Int,
        val episodes: List<XtreamEpisode>
    ) : SeriesDetailUiState
    data class Error(val message: String) : SeriesDetailUiState
}

@HiltViewModel
class SeriesDetailViewModel @Inject constructor(
    private val xtreamRepository: XtreamRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow<SeriesDetailUiState>(SeriesDetailUiState.Loading)
    val uiState: StateFlow<SeriesDetailUiState> = _uiState.asStateFlow()

    private var seriesInfo: XtreamSeriesInfo? = null

    // Get series ID from navigation arguments
    private val seriesId: Int = savedStateHandle.get<Int>("seriesId") ?: -1

    init {
        if (seriesId > 0) {
            loadSeriesInfo()
        } else {
            _uiState.value = SeriesDetailUiState.Error("Invalid series ID")
        }
    }

    private fun loadSeriesInfo() {
        viewModelScope.launch {
            _uiState.value = SeriesDetailUiState.Loading

            when (val result = xtreamRepository.getSeriesInfo(seriesId)) {
                is XtreamResult.Success -> {
                    seriesInfo = result.data
                    val seasons = result.data.seasons.sortedBy { it.seasonNumber }
                    val firstSeason = seasons.firstOrNull()?.seasonNumber ?: 1
                    val episodes = result.data.episodes[firstSeason.toString()] ?: emptyList()

                    _uiState.value = SeriesDetailUiState.Ready(
                        seriesInfo = result.data,
                        seasons = seasons,
                        selectedSeason = firstSeason,
                        episodes = episodes.sortedBy { it.episodeNum }
                    )
                }
                is XtreamResult.Error -> {
                    _uiState.value = SeriesDetailUiState.Error(result.message)
                }
                else -> {
                    _uiState.value = SeriesDetailUiState.Error("Unknown error")
                }
            }
        }
    }

    fun selectSeason(seasonNumber: Int) {
        val currentState = _uiState.value
        val info = seriesInfo
        if (currentState is SeriesDetailUiState.Ready && info != null) {
            val episodes = info.episodes[seasonNumber.toString()] ?: emptyList()
            _uiState.value = currentState.copy(
                selectedSeason = seasonNumber,
                episodes = episodes.sortedBy { it.episodeNum }
            )
        }
    }

    suspend fun getEpisodeStreamUrl(episode: XtreamEpisode): String? {
        val episodeId = episode.id ?: return null
        val extension = episode.containerExtension ?: "mp4"
        return xtreamRepository.buildEpisodeStreamUrl(episodeId, extension)
    }

    fun retry() {
        loadSeriesInfo()
    }
}

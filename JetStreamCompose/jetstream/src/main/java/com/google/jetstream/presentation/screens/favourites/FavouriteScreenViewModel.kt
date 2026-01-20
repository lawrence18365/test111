/*
 * Copyright 2023 Google LLC
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

package com.google.jetstream.presentation.screens.favourites

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.jetstream.R
import com.google.jetstream.data.entities.MovieList
import com.google.jetstream.data.repositories.FavoritesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class FavouriteScreenViewModel @Inject constructor(
    favoritesRepository: FavoritesRepository
) : ViewModel() {

    private val selectedFilterList = MutableStateFlow(FilterList())

    val uiState: StateFlow<FavouriteScreenUiState> = combine(
        selectedFilterList,
        favoritesRepository.getAllFavorites()
    ) { filterList, favorites ->
        // Convert favorites to MovieList for display
        val allMovies = favorites.map { fav ->
            com.google.jetstream.data.entities.Movie(
                id = fav.streamId.toString(),
                videoUri = "", // Not needed for grid
                subtitleUri = null,
                posterUri = fav.streamIcon ?: "",
                name = fav.name,
                description = fav.categoryName ?: "",
                // Use category field to store streamType for filtering
                category = fav.streamType, 
                language = "",
                format = ""
            )
        }

        // Apply filters
        val activeFilters = filterList.items
        val filtered = if (activeFilters.isEmpty()) {
            allMovies
        } else {
            allMovies.filter { movie ->
                // Check if movie matches ANY of the active filters
                activeFilters.any { filter ->
                    when (filter) {
                        FilterCondition.Movies -> movie.category == "movie" || movie.category == "vod"
                        FilterCondition.TvShows -> movie.category == "series"
                        FilterCondition.LiveTv -> movie.category == "live"
                        else -> true
                    }
                }
            }
        }
        
        FavouriteScreenUiState.Ready(filtered, filterList)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = FavouriteScreenUiState.Loading
    )

    fun updateSelectedFilterList(filterList: FilterList) {
        selectedFilterList.value = filterList
    }

    companion object {
        val filterList = FilterList(
            listOf(
                FilterCondition.Movies,
                FilterCondition.TvShows,
                FilterCondition.LiveTv
            )
        )
    }
}

sealed interface FavouriteScreenUiState {
    data object Loading : FavouriteScreenUiState
    data class Ready(val favouriteMovieList: MovieList, val selectedFilterList: FilterList) :
        FavouriteScreenUiState
}

@Immutable
data class FilterList(val items: List<FilterCondition> = emptyList())

@Immutable
enum class FilterCondition(@StringRes val labelId: Int) {
    Movies(R.string.favorites_movies),
    TvShows(R.string.favorites_tv_shows),
    LiveTv(R.string.live) // Reusing "Live" string
}

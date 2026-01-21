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

package com.google.jetstream.presentation.screens.xtreamseries

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.jetstream.data.models.xtream.XtreamCategory
import com.google.jetstream.data.models.xtream.XtreamSeries
import com.google.jetstream.data.repositories.xtream.XtreamRepository
import com.google.jetstream.data.repositories.xtream.XtreamResult
import com.google.jetstream.presentation.utils.CountryFilter
import com.google.jetstream.presentation.utils.categoryIdsForCountry
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface XtreamSeriesUiState {
    data object Loading : XtreamSeriesUiState
    data class CategoriesLoaded(val categories: List<XtreamCategory>) : XtreamSeriesUiState
    data class Ready(
        val categories: List<XtreamCategory>,
        val selectedCategory: XtreamCategory,
        val seriesList: List<XtreamSeries>,
        val isLoadingSeries: Boolean = false
    ) : XtreamSeriesUiState
    data class Error(val message: String) : XtreamSeriesUiState
}

@HiltViewModel
class XtreamSeriesScreenViewModel @Inject constructor(
    private val xtreamRepository: XtreamRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<XtreamSeriesUiState>(XtreamSeriesUiState.Loading)
    val uiState: StateFlow<XtreamSeriesUiState> = _uiState.asStateFlow()

    private var categories: List<XtreamCategory> = emptyList()
    // Cache loaded series by category to avoid re-fetching
    private val seriesCache = mutableMapOf<String, List<XtreamSeries>>()
    private val countryCache = mutableMapOf<CountryFilter, Set<String>>()

    init {
        loadCategories()
    }

    private fun loadCategories() {
        viewModelScope.launch {
            _uiState.value = XtreamSeriesUiState.Loading

            when (val categoriesResult = xtreamRepository.getSeriesCategories()) {
                is XtreamResult.Success -> {
                    categories = categoriesResult.data
                    _uiState.value = XtreamSeriesUiState.CategoriesLoaded(categories = categories)
                }
                is XtreamResult.Error -> {
                    _uiState.value = XtreamSeriesUiState.Error(categoriesResult.message)
                }
                else -> {
                    _uiState.value = XtreamSeriesUiState.Error("Unknown error")
                }
            }
        }
    }

    fun selectCategory(category: XtreamCategory) {
        val categoryId = category.categoryId
        seriesCache[categoryId]?.let { cachedItems ->
            _uiState.value = XtreamSeriesUiState.Ready(
                categories = categories,
                selectedCategory = category,
                seriesList = cachedItems
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = XtreamSeriesUiState.Ready(
                categories = categories,
                selectedCategory = category,
                seriesList = emptyList(),
                isLoadingSeries = true
            )

            when (val seriesResult = xtreamRepository.getSeriesByCategory(categoryId)) {
                is XtreamResult.Success -> {
                    val items = seriesResult.data
                    seriesCache[categoryId] = items
                    _uiState.value = XtreamSeriesUiState.Ready(
                        categories = categories,
                        selectedCategory = category,
                        seriesList = items,
                        isLoadingSeries = false
                    )
                }
                is XtreamResult.Error -> {
                    _uiState.value = XtreamSeriesUiState.Error(seriesResult.message)
                }
                else -> {
                    _uiState.value = XtreamSeriesUiState.Error("Failed to load TV shows")
                }
            }
        }
    }

    fun goBackToCategories() {
        _uiState.value = XtreamSeriesUiState.CategoriesLoaded(categories = categories)
    }

    fun refresh() {
        seriesCache.clear()
        countryCache.clear()
        loadCategories()
    }
}

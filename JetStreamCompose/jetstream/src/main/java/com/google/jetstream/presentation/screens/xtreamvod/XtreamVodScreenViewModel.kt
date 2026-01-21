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

package com.google.jetstream.presentation.screens.xtreamvod

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.jetstream.data.models.xtream.XtreamCategory
import com.google.jetstream.data.models.xtream.XtreamVodItem
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

sealed interface XtreamVodUiState {
    data object Loading : XtreamVodUiState
    data class CategoriesLoaded(val categories: List<XtreamCategory>) : XtreamVodUiState
    data class Ready(
        val categories: List<XtreamCategory>,
        val selectedCategory: XtreamCategory,
        val vodItems: List<XtreamVodItem>,
        val isLoadingMovies: Boolean = false
    ) : XtreamVodUiState
    data class Error(val message: String) : XtreamVodUiState
}

@HiltViewModel
class XtreamVodScreenViewModel @Inject constructor(
    private val xtreamRepository: XtreamRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<XtreamVodUiState>(XtreamVodUiState.Loading)
    val uiState: StateFlow<XtreamVodUiState> = _uiState.asStateFlow()

    private var categories: List<XtreamCategory> = emptyList()
    // Cache loaded movies by category to avoid re-fetching
    private val vodCache = mutableMapOf<String, List<XtreamVodItem>>()
    private val countryCache = mutableMapOf<CountryFilter, Set<String>>()

    init {
        loadCategories()
    }

    private fun loadCategories() {
        viewModelScope.launch {
            _uiState.value = XtreamVodUiState.Loading

            when (val categoriesResult = xtreamRepository.getVodCategories()) {
                is XtreamResult.Success -> {
                    categories = categoriesResult.data
                    _uiState.value = XtreamVodUiState.CategoriesLoaded(categories = categories)
                }
                is XtreamResult.Error -> {
                    _uiState.value = XtreamVodUiState.Error(categoriesResult.message)
                }
                else -> {
                    _uiState.value = XtreamVodUiState.Error("Unknown error")
                }
            }
        }
    }

    fun selectCategory(category: XtreamCategory) {
        val categoryId = category.categoryId
        vodCache[categoryId]?.let { cachedItems ->
            _uiState.value = XtreamVodUiState.Ready(
                categories = categories,
                selectedCategory = category,
                vodItems = cachedItems
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = XtreamVodUiState.Ready(
                categories = categories,
                selectedCategory = category,
                vodItems = emptyList(),
                isLoadingMovies = true
            )

            when (val vodResult = xtreamRepository.getVodStreamsByCategory(categoryId)) {
                is XtreamResult.Success -> {
                    val items = vodResult.data
                    vodCache[categoryId] = items
                    _uiState.value = XtreamVodUiState.Ready(
                        categories = categories,
                        selectedCategory = category,
                        vodItems = items,
                        isLoadingMovies = false
                    )
                }
                is XtreamResult.Error -> {
                    _uiState.value = XtreamVodUiState.Error(vodResult.message)
                }
                else -> {
                    _uiState.value = XtreamVodUiState.Error("Failed to load movies")
                }
            }
        }
    }

    fun goBackToCategories() {
        _uiState.value = XtreamVodUiState.CategoriesLoaded(categories = categories)
    }

    suspend fun getStreamUrl(vodItem: XtreamVodItem): String? {
        val extension = vodItem.containerExtension ?: "mp4"
        return xtreamRepository.buildVodStreamUrl(vodItem.streamId, extension)
    }

    fun refresh() {
        vodCache.clear()
        countryCache.clear()
        loadCategories()
    }
}

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

/*
 * Xtream VOD (Movies) Screen ViewModel
 * Fixed: Load movies by category to prevent OOM crash from loading all VOD at once
 */
package com.google.jetstream.presentation.screens.xtreamvod

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.jetstream.data.models.xtream.XtreamCategory
import com.google.jetstream.data.models.xtream.XtreamVodItem
import com.google.jetstream.data.repositories.xtream.XtreamRepository
import com.google.jetstream.data.repositories.xtream.XtreamResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface XtreamVodUiState {
    data object Loading : XtreamVodUiState
    data class CategoriesLoaded(
        val categories: List<XtreamCategory>
    ) : XtreamVodUiState
    data class Ready(
        val categories: List<XtreamCategory>,
        val vodItems: List<XtreamVodItem>,
        val selectedCategoryId: String?,
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

    init {
        loadCategories()
    }

    private fun loadCategories() {
        viewModelScope.launch {
            _uiState.value = XtreamVodUiState.Loading

            when (val categoriesResult = xtreamRepository.getVodCategories()) {
                is XtreamResult.Success -> {
                    categories = categoriesResult.data
                    _uiState.value = XtreamVodUiState.CategoriesLoaded(
                        categories = categories
                    )
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

    fun selectCategory(categoryId: String) {
        // Check cache first
        vodCache[categoryId]?.let { cachedItems ->
            _uiState.value = XtreamVodUiState.Ready(
                categories = categories,
                vodItems = cachedItems,
                selectedCategoryId = categoryId
            )
            return
        }

        // Load movies for selected category
        viewModelScope.launch {
            // Show loading state while keeping categories visible
            _uiState.value = XtreamVodUiState.Ready(
                categories = categories,
                vodItems = emptyList(),
                selectedCategoryId = categoryId,
                isLoadingMovies = true
            )

            when (val vodResult = xtreamRepository.getVodStreamsByCategory(categoryId)) {
                is XtreamResult.Success -> {
                    vodCache[categoryId] = vodResult.data
                    _uiState.value = XtreamVodUiState.Ready(
                        categories = categories,
                        vodItems = vodResult.data,
                        selectedCategoryId = categoryId,
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
        loadCategories()
    }
}

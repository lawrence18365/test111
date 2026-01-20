/*
 * Xtream Series (TV Shows) Screen ViewModel
 */
package com.google.jetstream.presentation.screens.xtreamseries

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.jetstream.data.models.xtream.XtreamCategory
import com.google.jetstream.data.models.xtream.XtreamSeries
import com.google.jetstream.data.repositories.xtream.XtreamRepository
import com.google.jetstream.data.repositories.xtream.XtreamResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface XtreamSeriesUiState {
    data object Loading : XtreamSeriesUiState
    data class Ready(
        val categories: List<XtreamCategory>,
        val seriesList: List<XtreamSeries>,
        val selectedCategoryId: String? = null
    ) : XtreamSeriesUiState
    data class Error(val message: String) : XtreamSeriesUiState
}

@HiltViewModel
class XtreamSeriesScreenViewModel @Inject constructor(
    private val xtreamRepository: XtreamRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<XtreamSeriesUiState>(XtreamSeriesUiState.Loading)
    val uiState: StateFlow<XtreamSeriesUiState> = _uiState.asStateFlow()

    private var allSeries: List<XtreamSeries> = emptyList()
    private var categories: List<XtreamCategory> = emptyList()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            _uiState.value = XtreamSeriesUiState.Loading

            val categoriesResult = xtreamRepository.getSeriesCategories()
            val seriesResult = xtreamRepository.getSeries()

            when {
                categoriesResult is XtreamResult.Success && seriesResult is XtreamResult.Success -> {
                    categories = categoriesResult.data
                    allSeries = seriesResult.data
                    _uiState.value = XtreamSeriesUiState.Ready(
                        categories = categories,
                        seriesList = allSeries,
                        selectedCategoryId = null
                    )
                }
                categoriesResult is XtreamResult.Error -> {
                    _uiState.value = XtreamSeriesUiState.Error(categoriesResult.message)
                }
                seriesResult is XtreamResult.Error -> {
                    _uiState.value = XtreamSeriesUiState.Error(seriesResult.message)
                }
                else -> {
                    _uiState.value = XtreamSeriesUiState.Error("Unknown error")
                }
            }
        }
    }

    fun selectCategory(categoryId: String?) {
        val currentState = _uiState.value
        if (currentState is XtreamSeriesUiState.Ready) {
            val filteredSeries = if (categoryId == null) {
                allSeries
            } else {
                allSeries.filter { it.categoryId == categoryId }
            }
            _uiState.value = currentState.copy(
                seriesList = filteredSeries,
                selectedCategoryId = categoryId
            )
        }
    }

    fun refresh() {
        loadData()
    }
}

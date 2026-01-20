/*
 * Xtream VOD (Movies) Screen ViewModel
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
    data class Ready(
        val categories: List<XtreamCategory>,
        val vodItems: List<XtreamVodItem>,
        val selectedCategoryId: String? = null
    ) : XtreamVodUiState
    data class Error(val message: String) : XtreamVodUiState
}

@HiltViewModel
class XtreamVodScreenViewModel @Inject constructor(
    private val xtreamRepository: XtreamRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<XtreamVodUiState>(XtreamVodUiState.Loading)
    val uiState: StateFlow<XtreamVodUiState> = _uiState.asStateFlow()

    private var allVodItems: List<XtreamVodItem> = emptyList()
    private var categories: List<XtreamCategory> = emptyList()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            _uiState.value = XtreamVodUiState.Loading

            val categoriesResult = xtreamRepository.getVodCategories()
            val vodResult = xtreamRepository.getVodStreams()

            when {
                categoriesResult is XtreamResult.Success && vodResult is XtreamResult.Success -> {
                    categories = categoriesResult.data
                    allVodItems = vodResult.data
                    _uiState.value = XtreamVodUiState.Ready(
                        categories = categories,
                        vodItems = allVodItems,
                        selectedCategoryId = null
                    )
                }
                categoriesResult is XtreamResult.Error -> {
                    _uiState.value = XtreamVodUiState.Error(categoriesResult.message)
                }
                vodResult is XtreamResult.Error -> {
                    _uiState.value = XtreamVodUiState.Error(vodResult.message)
                }
                else -> {
                    _uiState.value = XtreamVodUiState.Error("Unknown error")
                }
            }
        }
    }

    fun selectCategory(categoryId: String?) {
        val currentState = _uiState.value
        if (currentState is XtreamVodUiState.Ready) {
            val filteredVod = if (categoryId == null) {
                allVodItems
            } else {
                allVodItems.filter { it.categoryId == categoryId }
            }
            _uiState.value = currentState.copy(
                vodItems = filteredVod,
                selectedCategoryId = categoryId
            )
        }
    }

    suspend fun getStreamUrl(vodItem: XtreamVodItem): String? {
        val extension = vodItem.containerExtension ?: "mp4"
        return xtreamRepository.buildVodStreamUrl(vodItem.streamId, extension)
    }

    fun refresh() {
        loadData()
    }
}

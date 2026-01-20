package com.google.jetstream.presentation.screens.xtreamvod

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.jetstream.data.models.xtream.XtreamVodInfoResponse
import com.google.jetstream.data.models.xtream.XtreamVodMovieData
import com.google.jetstream.data.repositories.xtream.XtreamRepository
import com.google.jetstream.data.repositories.xtream.XtreamResult
import com.google.jetstream.presentation.screens.VodIdBundleKey
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface XtreamVodDetailUiState {
    data object Loading : XtreamVodDetailUiState
    data class Ready(val vodInfo: XtreamVodInfoResponse) : XtreamVodDetailUiState
    data class Error(val message: String) : XtreamVodDetailUiState
}

@HiltViewModel
class XtreamVodDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val xtreamRepository: XtreamRepository
) : ViewModel() {

    private val vodId = savedStateHandle.get<String>(VodIdBundleKey)?.toIntOrNull() ?: -1

    private val _uiState = MutableStateFlow<XtreamVodDetailUiState>(XtreamVodDetailUiState.Loading)
    val uiState: StateFlow<XtreamVodDetailUiState> = _uiState.asStateFlow()

    init {
        if (vodId > 0) {
            loadVodInfo()
        } else {
            _uiState.value = XtreamVodDetailUiState.Error("Invalid VOD ID")
        }
    }

    private fun loadVodInfo() {
        viewModelScope.launch {
            _uiState.value = XtreamVodDetailUiState.Loading
            when (val result = xtreamRepository.getVodInfo(vodId)) {
                is XtreamResult.Success -> {
                    _uiState.value = XtreamVodDetailUiState.Ready(result.data)
                }
                is XtreamResult.Error -> {
                    _uiState.value = XtreamVodDetailUiState.Error(result.message)
                }
                else -> {
                    _uiState.value = XtreamVodDetailUiState.Error("Unknown error")
                }
            }
        }
    }

    fun getVodId(): Int = vodId

    suspend fun getStreamUrl(movieData: XtreamVodMovieData?): String? {
        val streamId = movieData?.streamId ?: vodId
        if (streamId <= 0) return null
        val extension = movieData?.containerExtension ?: "mp4"
        return xtreamRepository.buildVodStreamUrl(streamId, extension)
    }
}

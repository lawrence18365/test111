/*
 * EPG (Electronic Program Guide) Screen ViewModel
 */
package com.google.jetstream.presentation.screens.epg

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.jetstream.data.models.xtream.XtreamCategory
import com.google.jetstream.data.models.xtream.XtreamChannel
import com.google.jetstream.data.models.xtream.XtreamEpgListing
import com.google.jetstream.data.repositories.xtream.XtreamRepository
import com.google.jetstream.data.repositories.xtream.XtreamResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class EpgChannelWithPrograms(
    val channel: XtreamChannel,
    val programs: List<EpgProgram>
)

data class EpgProgram(
    val title: String,
    val description: String?,
    val startTime: Long,
    val endTime: Long,
    val isLive: Boolean,
    val isCatchupAvailable: Boolean = false
) {
    val durationMinutes: Int
        get() = ((endTime - startTime) / 60000).toInt()

    val progress: Float
        get() {
            if (!isLive) return 0f
            val now = System.currentTimeMillis()
            val elapsed = now - startTime
            val total = endTime - startTime
            return if (total > 0) (elapsed.toFloat() / total).coerceIn(0f, 1f) else 0f
        }
}

sealed interface EpgUiState {
    data object Loading : EpgUiState
    data class Ready(
        val channels: List<EpgChannelWithPrograms>,
        val categories: List<XtreamCategory>,
        val selectedCategoryId: String? = null,
        val currentTimeSlot: Long = System.currentTimeMillis()
    ) : EpgUiState
    data class Error(val message: String) : EpgUiState
}

@HiltViewModel
class EpgScreenViewModel @Inject constructor(
    private val xtreamRepository: XtreamRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<EpgUiState>(EpgUiState.Loading)
    val uiState: StateFlow<EpgUiState> = _uiState.asStateFlow()

    private var allChannels: List<XtreamChannel> = emptyList()
    private var categories: List<XtreamCategory> = emptyList()
    private var epgData: Map<Int, List<EpgProgram>> = emptyMap()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            _uiState.value = EpgUiState.Loading

            val categoriesResult = xtreamRepository.getLiveCategories()
            val channelsResult = xtreamRepository.getLiveStreams()

            when {
                categoriesResult is XtreamResult.Success && channelsResult is XtreamResult.Success -> {
                    categories = categoriesResult.data
                    allChannels = channelsResult.data

                    // Generate mock EPG data for demo (real EPG would come from API)
                    epgData = generateMockEpgData(allChannels)

                    val channelsWithPrograms = allChannels.map { channel ->
                        EpgChannelWithPrograms(
                            channel = channel,
                            programs = epgData[channel.streamId] ?: generateDefaultPrograms()
                        )
                    }

                    _uiState.value = EpgUiState.Ready(
                        channels = channelsWithPrograms,
                        categories = categories,
                        selectedCategoryId = null,
                        currentTimeSlot = roundToHalfHour(System.currentTimeMillis())
                    )
                }
                categoriesResult is XtreamResult.Error -> {
                    _uiState.value = EpgUiState.Error(categoriesResult.message)
                }
                channelsResult is XtreamResult.Error -> {
                    _uiState.value = EpgUiState.Error(channelsResult.message)
                }
                else -> {
                    _uiState.value = EpgUiState.Error("Unknown error")
                }
            }
        }
    }

    fun selectCategory(categoryId: String?) {
        val currentState = _uiState.value
        if (currentState is EpgUiState.Ready) {
            val filteredChannels = if (categoryId == null) {
                allChannels
            } else {
                allChannels.filter { it.categoryId == categoryId }
            }

            val channelsWithPrograms = filteredChannels.map { channel ->
                EpgChannelWithPrograms(
                    channel = channel,
                    programs = epgData[channel.streamId] ?: generateDefaultPrograms()
                )
            }

            _uiState.value = currentState.copy(
                channels = channelsWithPrograms,
                selectedCategoryId = categoryId
            )
        }
    }

    fun navigateTime(forward: Boolean) {
        val currentState = _uiState.value
        if (currentState is EpgUiState.Ready) {
            val newTime = if (forward) {
                currentState.currentTimeSlot + 30 * 60 * 1000 // +30 minutes
            } else {
                currentState.currentTimeSlot - 30 * 60 * 1000 // -30 minutes
            }
            _uiState.value = currentState.copy(currentTimeSlot = newTime)
        }
    }

    fun goToNow() {
        val currentState = _uiState.value
        if (currentState is EpgUiState.Ready) {
            _uiState.value = currentState.copy(
                currentTimeSlot = roundToHalfHour(System.currentTimeMillis())
            )
        }
    }

    suspend fun getStreamUrl(channel: XtreamChannel): String? {
        return xtreamRepository.buildLiveStreamUrl(channel.streamId)
    }

    suspend fun getCatchupStreamUrl(channel: XtreamChannel, program: EpgProgram): String? {
        return xtreamRepository.buildCatchupStreamUrl(
            channel.streamId,
            program.startTime,
            program.endTime
        )
    }

    private fun roundToHalfHour(time: Long): Long {
        val calendar = Calendar.getInstance().apply { timeInMillis = time }
        val minute = calendar.get(Calendar.MINUTE)
        calendar.set(Calendar.MINUTE, if (minute < 30) 0 else 30)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    private fun generateMockEpgData(channels: List<XtreamChannel>): Map<Int, List<EpgProgram>> {
        val now = System.currentTimeMillis()
        val programTitles = listOf(
            "Morning News", "Talk Show", "Documentary", "Sports Center",
            "Movie Premiere", "Comedy Hour", "Drama Series", "Reality TV",
            "News Update", "Music Show", "Kids Zone", "Cooking Show",
            "Travel Adventure", "Science Today", "Game Show", "Late Night"
        )

        return channels.associate { channel ->
            val programs = mutableListOf<EpgProgram>()
            var startTime = roundToHalfHour(now) - 2 * 60 * 60 * 1000 // Start 2 hours ago

            repeat(12) { index ->
                val duration = listOf(30, 60, 90, 120).random() * 60 * 1000L
                val endTime = startTime + duration
                val isLive = now in startTime until endTime
                val isCatchupAvailable = !isLive && isCatchupAvailable(channel, startTime, endTime)

                programs.add(
                    EpgProgram(
                        title = programTitles.random(),
                        description = "Episode ${index + 1} - ${channel.name}",
                        startTime = startTime,
                        endTime = endTime,
                        isLive = isLive,
                        isCatchupAvailable = isCatchupAvailable
                    )
                )

                startTime = endTime
            }

            channel.streamId to programs
        }
    }

    private fun generateDefaultPrograms(): List<EpgProgram> {
        val now = System.currentTimeMillis()
        return listOf(
            EpgProgram(
                title = "No Program Info",
                description = "Program information not available",
                startTime = now - 60 * 60 * 1000,
                endTime = now + 60 * 60 * 1000,
                isLive = true,
                isCatchupAvailable = false
            )
        )
    }

    private fun isCatchupAvailable(
        channel: XtreamChannel,
        startTime: Long,
        endTime: Long
    ): Boolean {
        if (channel.tvArchive != 1) return false
        val archiveDays = channel.tvArchiveDuration?.takeIf { it > 0 } ?: 1
        val windowMs = archiveDays * 24L * 60 * 60 * 1000
        val now = System.currentTimeMillis()

        if (endTime >= now) return false
        return now - endTime <= windowMs && endTime > startTime
    }

    fun refresh() {
        loadData()
    }
}

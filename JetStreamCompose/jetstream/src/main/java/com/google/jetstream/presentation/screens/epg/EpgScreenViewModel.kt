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

package com.google.jetstream.presentation.screens.epg

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.jetstream.data.models.xtream.XtreamCategory
import com.google.jetstream.data.models.xtream.XtreamChannel
import com.google.jetstream.data.repositories.EpgRepository
import com.google.jetstream.data.repositories.xtream.XtreamRepository
import com.google.jetstream.data.repositories.xtream.XtreamResult
import com.google.jetstream.presentation.utils.CountryFilter
import com.google.jetstream.presentation.utils.DefaultCountryFilters
import com.google.jetstream.presentation.utils.categoryIdsForCountry
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Calendar
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

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
        val selectedCountry: CountryFilter,
        val currentTimeSlot: Long = System.currentTimeMillis()
    ) : EpgUiState
    data class Error(val message: String) : EpgUiState
}

@HiltViewModel
class EpgScreenViewModel @Inject constructor(
    private val xtreamRepository: XtreamRepository,
    private val epgRepository: EpgRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<EpgUiState>(EpgUiState.Loading)
    val uiState: StateFlow<EpgUiState> = _uiState.asStateFlow()

    private var allChannels: List<XtreamChannel> = emptyList()
    private var categories: List<XtreamCategory> = emptyList()
    private var epgData: Map<Int, List<EpgProgram>> = emptyMap()
    private val defaultCountry = DefaultCountryFilters.first()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            _uiState.value = EpgUiState.Loading

            // Trigger background sync
            epgRepository.syncEpg()

            val categoriesResult = xtreamRepository.getLiveCategories()
            val channelsResult = xtreamRepository.getLiveStreams()

            when {
                categoriesResult is XtreamResult.Success &&
                    channelsResult is XtreamResult.Success -> {
                    categories = categoriesResult.data
                    allChannels = channelsResult.data

                    // Fetch real EPG data
                    epgData = fetchEpgData(allChannels)

                    val filteredChannels = filterChannelsByCountry(defaultCountry)
                    val channelsWithPrograms = filteredChannels.map { channel ->
                        EpgChannelWithPrograms(
                            channel = channel,
                            programs = epgData[channel.streamId] ?: generateDefaultPrograms()
                        )
                    }

                    _uiState.value = EpgUiState.Ready(
                        channels = channelsWithPrograms,
                        categories = categories,
                        selectedCountry = defaultCountry,
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

    private suspend fun fetchEpgData(channels: List<XtreamChannel>): Map<Int, List<EpgProgram>> {
        val now = System.currentTimeMillis()
        val start = now - 24 * 60 * 60 * 1000 // Last 24 hours
        val end = now + 48 * 60 * 60 * 1000   // Next 48 hours

        // Get all valid EPG IDs from channels
        val epgIds = channels.mapNotNull { it.epgChannelId }.distinct()
        
        if (epgIds.isEmpty()) return emptyMap()

        // Fetch programs for these IDs
        val entities = epgRepository.getProgramsForChannels(epgIds, start, end)
        
        // Group by EPG Channel ID
        val programsByEpgId = entities.groupBy { it.channelId }

        // Map Stream ID -> Programs
        return channels.associate { channel ->
            val channelEpgId = channel.epgChannelId
            val programEntities = if (channelEpgId != null) {
                programsByEpgId[channelEpgId] ?: emptyList()
            } else {
                emptyList()
            }

            val programs = programEntities.map { entity ->
                val isLive = now in entity.startTime until entity.endTime
                EpgProgram(
                    title = entity.title,
                    description = entity.description,
                    startTime = entity.startTime,
                    endTime = entity.endTime,
                    isLive = isLive,
                    isCatchupAvailable = !isLive && 
                        isCatchupAvailable(channel, entity.startTime, entity.endTime)
                )
            }
            
            channel.streamId to programs
        }
    }

    fun selectCountry(country: CountryFilter) {
        val currentState = _uiState.value
        if (currentState is EpgUiState.Ready) {
            val filteredChannels = filterChannelsByCountry(country)

            val channelsWithPrograms = filteredChannels.map { channel ->
                EpgChannelWithPrograms(
                    channel = channel,
                    programs = epgData[channel.streamId] ?: generateDefaultPrograms()
                )
            }

            _uiState.value = currentState.copy(
                channels = channelsWithPrograms,
                selectedCountry = country
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

    private fun filterChannelsByCountry(country: CountryFilter): List<XtreamChannel> {
        val categoryIds = categoryIdsForCountry(categories, country)
        if (categoryIds.isEmpty()) {
            return emptyList()
        }
        return allChannels.filter { channel ->
            val matchesPrimary = channel.categoryId?.let { categoryIds.contains(it) } == true
            val matchesAny = channel.categoryIds?.any { categoryIds.contains(it.toString()) } == true
            matchesPrimary || matchesAny
        }
    }

    private fun roundToHalfHour(time: Long): Long {
        val calendar = Calendar.getInstance().apply { timeInMillis = time }
        val minute = calendar.get(Calendar.MINUTE)
        calendar.set(Calendar.MINUTE, if (minute < 30) 0 else 30)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
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

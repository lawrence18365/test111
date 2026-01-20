/*
 * EPG (Electronic Program Guide) Screen - TV Guide style interface
 */
package com.google.jetstream.presentation.screens.epg

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.tv.foundation.lazy.list.TvLazyColumn
import androidx.tv.foundation.lazy.list.TvLazyRow
import androidx.tv.foundation.lazy.list.items
import androidx.tv.material3.Border
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.FilterChip
import androidx.tv.material3.Icon
import androidx.tv.material3.IconButton
import androidx.tv.material3.surfaceColorAtElevation
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import com.google.jetstream.data.models.xtream.XtreamCategory
import com.google.jetstream.presentation.screens.streamPlayer.StreamPlayerArgs
import com.google.jetstream.presentation.screens.streamPlayer.StreamTypes
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val EpgAccentColor = Color(0xFF00b4d8)
private val EpgCardShape = RoundedCornerShape(10.dp)

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun EpgScreen(
    onChannelSelected: (StreamPlayerArgs) -> Unit,
    viewModel: EpgScreenViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    when (val state = uiState) {
        is EpgUiState.Loading -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Loading TV Guide...",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        is EpgUiState.Error -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Error loading TV Guide",
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color(0xFFef5350)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = state.message,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        is EpgUiState.Ready -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 32.dp, vertical = 16.dp)
            ) {
                // Header with time navigation
                EpgHeader(
                    currentTimeSlot = state.currentTimeSlot,
                    onNavigateBack = { viewModel.navigateTime(false) },
                    onNavigateForward = { viewModel.navigateTime(true) },
                    onGoToNow = { viewModel.goToNow() }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Category filters
                if (state.categories.isNotEmpty()) {
                    EpgCategoryRow(
                        categories = state.categories,
                        selectedCategoryId = state.selectedCategoryId,
                        onCategorySelected = { viewModel.selectCategory(it) }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Time slots header
                TimeSlotHeader(currentTimeSlot = state.currentTimeSlot)

                Spacer(modifier = Modifier.height(8.dp))

                // Channel list with programs
                TvLazyColumn(
                    contentPadding = PaddingValues(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(state.channels, key = { it.channel.streamId }) { channelWithPrograms ->
                        EpgChannelRow(
                            channelWithPrograms = channelWithPrograms,
                            currentTimeSlot = state.currentTimeSlot,
                            onChannelClick = {
                                coroutineScope.launch {
                                    val streamUrl = viewModel.getStreamUrl(channelWithPrograms.channel)
                                    if (streamUrl != null) {
                                        val categoryName = state.categories
                                            .firstOrNull { it.categoryId == channelWithPrograms.channel.categoryId }
                                            ?.categoryName
                                        onChannelSelected(
                                            StreamPlayerArgs(
                                                streamUrl = streamUrl,
                                                streamName = channelWithPrograms.channel.name,
                                                streamId = channelWithPrograms.channel.streamId,
                                                streamType = StreamTypes.LIVE,
                                                streamIcon = channelWithPrograms.channel.streamIcon,
                                                categoryName = categoryName,
                                                channelNumber = channelWithPrograms.channel.num
                                            )
                                        )
                                    }
                                }
                            },
                            onProgramClick = { program ->
                                coroutineScope.launch {
                                    val categoryName = state.categories
                                        .firstOrNull { it.categoryId == channelWithPrograms.channel.categoryId }
                                        ?.categoryName
                                    if (program.isLive) {
                                        val streamUrl = viewModel.getStreamUrl(channelWithPrograms.channel)
                                        if (streamUrl != null) {
                                            onChannelSelected(
                                                StreamPlayerArgs(
                                                    streamUrl = streamUrl,
                                                    streamName = channelWithPrograms.channel.name,
                                                    streamId = channelWithPrograms.channel.streamId,
                                                    streamType = StreamTypes.LIVE,
                                                    streamIcon = channelWithPrograms.channel.streamIcon,
                                                    categoryName = categoryName,
                                                    channelNumber = channelWithPrograms.channel.num,
                                                    programTitle = program.title,
                                                    programStart = program.startTime,
                                                    programEnd = program.endTime
                                                )
                                            )
                                        }
                                    } else if (program.isCatchupAvailable) {
                                        val streamUrl = viewModel.getCatchupStreamUrl(
                                            channelWithPrograms.channel,
                                            program
                                        )
                                        if (streamUrl != null) {
                                            onChannelSelected(
                                                StreamPlayerArgs(
                                                    streamUrl = streamUrl,
                                                    streamName = channelWithPrograms.channel.name,
                                                    streamId = channelWithPrograms.channel.streamId,
                                                    streamType = StreamTypes.CATCHUP,
                                                    streamIcon = channelWithPrograms.channel.streamIcon,
                                                    categoryName = categoryName,
                                                    channelNumber = channelWithPrograms.channel.num,
                                                    programTitle = program.title,
                                                    programStart = program.startTime,
                                                    programEnd = program.endTime
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun EpgHeader(
    currentTimeSlot: Long,
    onNavigateBack: () -> Unit,
    onNavigateForward: () -> Unit,
    onGoToNow: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()) }
    val timeFormat = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "TV Guide",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = dateFormat.format(Date(currentTimeSlot)),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Navigate back
            IconButton(onClick = onNavigateBack) {
                Icon(
                    Icons.Default.ChevronLeft,
                    contentDescription = "Previous time",
                    modifier = Modifier.size(32.dp)
                )
            }

            // Current time display
            Card(
                onClick = onGoToNow,
                colors = CardDefaults.colors(
                    containerColor = EpgAccentColor
                )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Schedule,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = timeFormat.format(Date(currentTimeSlot)),
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Navigate forward
            IconButton(onClick = onNavigateForward) {
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = "Next time",
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun EpgCategoryRow(
    categories: List<XtreamCategory>,
    selectedCategoryId: String?,
    onCategorySelected: (String?) -> Unit
) {
    TvLazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 4.dp)
    ) {
        item {
            FilterChip(
                selected = selectedCategoryId == null,
                onClick = { onCategorySelected(null) }
            ) {
                Text("All Channels")
            }
        }

        items(categories, key = { it.categoryId }) { category ->
            FilterChip(
                selected = selectedCategoryId == category.categoryId,
                onClick = { onCategorySelected(category.categoryId) }
            ) {
                Text(
                    text = category.categoryName,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun TimeSlotHeader(currentTimeSlot: Long) {
    val timeFormat = remember { SimpleDateFormat("h:mm", Locale.getDefault()) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 140.dp) // Offset for channel column
    ) {
        // Show 4 time slots (2 hours)
        repeat(4) { index ->
            val slotTime = currentTimeSlot + (index * 30 * 60 * 1000)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 2.dp)
            ) {
                Text(
                    text = timeFormat.format(Date(slotTime)),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun EpgChannelRow(
    channelWithPrograms: EpgChannelWithPrograms,
    currentTimeSlot: Long,
    onChannelClick: () -> Unit,
    onProgramClick: (EpgProgram) -> Unit
) {
    val timeWindowEnd = currentTimeSlot + (2 * 60 * 60 * 1000) // 2 hours window

    // Filter programs that overlap with current time window
    val visiblePrograms = channelWithPrograms.programs.filter { program ->
        program.endTime > currentTimeSlot && program.startTime < timeWindowEnd
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val channelContainer = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
        val channelFocusedContainer = MaterialTheme.colorScheme.surfaceColorAtElevation(6.dp)
        // Channel info (fixed width)
        Card(
            onClick = onChannelClick,
            modifier = Modifier
                .width(130.dp)
                .fillMaxHeight(),
            colors = CardDefaults.colors(
                containerColor = channelContainer,
                focusedContainerColor = channelFocusedContainer
            ),
            border = CardDefaults.border(
                focusedBorder = Border(
                    border = BorderStroke(1.dp, EpgAccentColor.copy(alpha = 0.35f)),
                    shape = EpgCardShape
                )
            ),
            scale = CardDefaults.scale(focusedScale = 1.02f),
            shape = EpgCardShape
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Channel icon
                if (!channelWithPrograms.channel.streamIcon.isNullOrBlank()) {
                    AsyncImage(
                        model = channelWithPrograms.channel.streamIcon,
                        contentDescription = channelWithPrograms.channel.name,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        contentScale = ContentScale.Fit
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }

                Text(
                    text = channelWithPrograms.channel.name,
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(modifier = Modifier.width(6.dp))

        // Programs timeline
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            visiblePrograms.forEach { program ->
                val programWeight = calculateProgramWeight(
                    program,
                    currentTimeSlot,
                    timeWindowEnd
                )

                if (programWeight > 0f) {
                    EpgProgramCard(
                        program = program,
                        modifier = Modifier.weight(programWeight),
                        onClick = { onProgramClick(program) }
                    )
                }
            }

            // Fill remaining space if needed
            val totalWeight = visiblePrograms.sumOf {
                calculateProgramWeight(it, currentTimeSlot, timeWindowEnd).toDouble()
            }.toFloat()

            if (totalWeight < 1f) {
                Spacer(modifier = Modifier.weight(1f - totalWeight))
            }
        }
    }
}

private fun calculateProgramWeight(
    program: EpgProgram,
    windowStart: Long,
    windowEnd: Long
): Float {
    val visibleStart = maxOf(program.startTime, windowStart)
    val visibleEnd = minOf(program.endTime, windowEnd)
    val visibleDuration = visibleEnd - visibleStart
    val windowDuration = windowEnd - windowStart

    return if (visibleDuration > 0 && windowDuration > 0) {
        (visibleDuration.toFloat() / windowDuration).coerceIn(0f, 1f)
    } else {
        0f
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun EpgProgramCard(
    program: EpgProgram,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val baseContainer = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
    val focusedContainer = MaterialTheme.colorScheme.surfaceColorAtElevation(6.dp)
    val liveContainer = lerp(baseContainer, EpgAccentColor, 0.12f)
    val liveFocusedContainer = lerp(focusedContainer, EpgAccentColor, 0.18f)
    val showDetails = isFocused && !program.description.isNullOrBlank()

    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxHeight()
            .onFocusChanged { isFocused = it.isFocused },
        colors = CardDefaults.colors(
            containerColor = if (program.isLive) liveContainer else baseContainer,
            focusedContainerColor = if (program.isLive) liveFocusedContainer else focusedContainer
        ),
        border = CardDefaults.border(
            focusedBorder = Border(
                border = BorderStroke(1.dp, EpgAccentColor.copy(alpha = 0.35f)),
                shape = EpgCardShape
            )
        ),
        scale = CardDefaults.scale(focusedScale = 1.03f),
        shape = EpgCardShape
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (program.isLive) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .fillMaxHeight()
                        .width(3.dp)
                        .background(EpgAccentColor.copy(alpha = 0.85f))
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        start = if (program.isLive) 12.dp else 8.dp,
                        top = 8.dp,
                        end = 8.dp,
                        bottom = 8.dp
                    ),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    // Live indicator
                    if (program.isLive) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 2.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(Color.Red)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "LIVE",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Red,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else if (program.isCatchupAvailable) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 2.dp)
                        ) {
                            Icon(
                                Icons.Default.Schedule,
                                contentDescription = null,
                                tint = Color(0xFFFFB74D),
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "CATCH-UP",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFFFB74D),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Program title
                    Text(
                        text = program.title,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    // Description (focus-only to reduce visual noise)
                    if (showDetails) {
                        Text(
                            text = program.description.orEmpty(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Progress bar for live programs
                if (program.isLive) {
                    LinearProgressIndicator(
                        progress = { program.progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = EpgAccentColor,
                        trackColor = Color.White.copy(alpha = 0.2f)
                    )
                }
            }
        }
    }
}

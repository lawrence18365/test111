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

package com.google.jetstream.presentation.screens.livechannels

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.tv.foundation.lazy.grid.TvGridCells
import androidx.tv.foundation.lazy.grid.TvLazyVerticalGrid
import androidx.tv.foundation.lazy.grid.items
import androidx.tv.foundation.lazy.grid.rememberTvLazyGridState
import androidx.tv.foundation.lazy.list.TvLazyRow
import androidx.tv.foundation.lazy.list.items
import androidx.tv.material3.Border
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import androidx.tv.material3.surfaceColorAtElevation
import coil.compose.AsyncImage
import com.google.jetstream.data.local.WatchHistory
import com.google.jetstream.data.models.xtream.XtreamChannel
import com.google.jetstream.presentation.screens.streamPlayer.StreamPlayerArgs
import com.google.jetstream.presentation.screens.streamPlayer.StreamTypes
import com.google.jetstream.presentation.utils.CountryFilterRow
import com.google.jetstream.presentation.utils.focusBorderStroke
import com.google.jetstream.presentation.utils.headerBackdropBrush
import com.google.jetstream.presentation.theme.JetStreamCardShape
import kotlinx.coroutines.launch

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun LiveChannelsScreen(
    onChannelSelected: (StreamPlayerArgs) -> Unit,
    onScroll: (isTopBarVisible: Boolean) -> Unit = {},
    viewModel: LiveChannelsScreenViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val recentHistory by viewModel.recentHistory.collectAsState(initial = emptyList())
    val coroutineScope = rememberCoroutineScope()

    when (val state = uiState) {
        is LiveChannelsUiState.Loading -> {
            onScroll(true)
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Loading channels...",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        is LiveChannelsUiState.Error -> {
            onScroll(true)
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Error loading channels",
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

        is LiveChannelsUiState.Ready -> {
            val gridState = rememberTvLazyGridState()
            val isTopBarVisible by remember {
                derivedStateOf {
                    gridState.firstVisibleItemIndex == 0 &&
                        gridState.firstVisibleItemScrollOffset < 100
                }
            }

            LaunchedEffect(isTopBarVisible) {
                onScroll(isTopBarVisible)
            }

            val showExpandedHeader by remember {
                derivedStateOf {
                    gridState.firstVisibleItemIndex == 0 &&
                        gridState.firstVisibleItemScrollOffset < 24
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 48.dp, vertical = 24.dp)
            ) {
                val headerBrush = headerBackdropBrush()
                val recentLiveHistory = remember(recentHistory) {
                    recentHistory.filter { it.streamType == StreamTypes.LIVE }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(headerBrush)
                        .padding(bottom = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Live TV",
                            style = MaterialTheme.typography.headlineLarge
                        )

                        if (state.channels.isNotEmpty()) {
                            Text(
                                text = "${state.selectedCountry.displayName} · " +
                                    "${state.channels.size} channels",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    }

                    AnimatedVisibility(
                        visible = showExpandedHeader,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Column {
                            if (recentLiveHistory.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Recently Watched",
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )

                                TvLazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    contentPadding = PaddingValues(bottom = 8.dp)
                                ) {
                                    items(recentLiveHistory, key = { it.streamId }) { history ->
                                        HistoryChannelCard(
                                            history = history,
                                            onClick = {
                                                coroutineScope.launch {
                                                    val streamUrl = viewModel.getStreamUrl(
                                                        history.streamId
                                                    )
                                                    if (streamUrl != null) {
                                                        onChannelSelected(
                                                            StreamPlayerArgs(
                                                                streamUrl = streamUrl,
                                                                streamName = history.name,
                                                                streamId = history.streamId,
                                                                streamType = StreamTypes.LIVE,
                                                                streamIcon = history.streamIcon
                                                            )
                                                        )
                                                    }
                                                }
                                            }
                                        )
                                    }
                                }
                            }

                            if (state.categories.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Countries",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                                )
                                CountryFilterRow(
                                    selectedCountry = state.selectedCountry,
                                    onCountrySelected = { viewModel.selectCountry(it) }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Channels Grid
                if (state.channels.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "No channels found for ${state.selectedCountry.displayName}",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Try USA or UK.",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                } else {
                    TvLazyVerticalGrid(
                        columns = TvGridCells.Fixed(5),
                        state = gridState,
                        contentPadding = PaddingValues(bottom = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(state.channels, key = { it.streamId }) { channel ->
                            ChannelCard(
                                channel = channel,
                                onClick = {
                                    coroutineScope.launch {
                                        val streamUrl = viewModel.getStreamUrl(channel)
                                        if (streamUrl != null) {
                                            val categoryName = state.categories
                                                .firstOrNull { it.categoryId == channel.categoryId }
                                                ?.categoryName
                                            onChannelSelected(
                                                StreamPlayerArgs(
                                                    streamUrl = streamUrl,
                                                    streamName = channel.name,
                                                    streamId = channel.streamId,
                                                    streamType = StreamTypes.LIVE,
                                                    streamIcon = channel.streamIcon,
                                                    categoryName = categoryName,
                                                    channelNumber = channel.num
                                                )
                                            )
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
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun ChannelCard(
    channel: XtreamChannel,
    onClick: () -> Unit
) {
    val container = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
    val focusedContainer = MaterialTheme.colorScheme.surfaceColorAtElevation(6.dp)
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(16f / 10f),
        colors = CardDefaults.colors(
            containerColor = container,
            focusedContainerColor = focusedContainer
        ),
        border = CardDefaults.border(
            focusedBorder = Border(
                border = focusBorderStroke(),
                shape = JetStreamCardShape
            )
        ),
        scale = CardDefaults.scale(focusedScale = 1.02f),
        shape = CardDefaults.shape(shape = JetStreamCardShape)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Channel Icon
            if (!channel.streamIcon.isNullOrBlank()) {
                AsyncImage(
                    model = channel.streamIcon,
                    contentDescription = channel.name,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentScale = ContentScale.Fit
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Channel Name
            Text(
                text = channel.name,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun HistoryChannelCard(
    history: WatchHistory,
    onClick: () -> Unit
) {
    val container = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
    val focusedContainer = MaterialTheme.colorScheme.surfaceColorAtElevation(6.dp)
    Card(
        onClick = onClick,
        modifier = Modifier
            .width(220.dp)
            .height(140.dp),
        colors = CardDefaults.colors(
            containerColor = container,
            focusedContainerColor = focusedContainer
        ),
        border = CardDefaults.border(
            focusedBorder = Border(
                border = focusBorderStroke(),
                shape = JetStreamCardShape
            )
        ),
        scale = CardDefaults.scale(focusedScale = 1.02f),
        shape = CardDefaults.shape(shape = JetStreamCardShape)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (history.streamIcon != null) {
                AsyncImage(
                    model = history.streamIcon,
                    contentDescription = history.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.6f))
                    .padding(8.dp)
            ) {
                Text(
                    text = history.name,
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

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

package com.google.jetstream.presentation.screens.xtreamsearch

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.tv.foundation.lazy.list.TvLazyRow
import androidx.tv.foundation.lazy.list.items
import androidx.tv.material3.Border
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.FilterChip
import androidx.tv.material3.Icon
import androidx.tv.material3.IconButton
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import com.google.jetstream.data.models.xtream.XtreamVodItem
import com.google.jetstream.presentation.screens.streamPlayer.StreamPlayerArgs
import com.google.jetstream.presentation.screens.streamPlayer.StreamTypes
import kotlinx.coroutines.launch

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun XtreamSearchScreen(
    onChannelSelected: (StreamPlayerArgs) -> Unit,
    onVodSelected: (XtreamVodItem) -> Unit,
    onSeriesSelected: (series: com.google.jetstream.data.models.xtream.XtreamSeries) -> Unit,
    initialQuery: String? = null,
    viewModel: XtreamSearchViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    val searchFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        searchFocusRequester.requestFocus()
    }

    LaunchedEffect(initialQuery) {
        if (!initialQuery.isNullOrBlank() && initialQuery != uiState.query) {
            viewModel.updateQuery(initialQuery)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 48.dp, vertical = 24.dp)
    ) {
        // Search header
        Text(
            text = "Search",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Search input
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = uiState.query,
                onValueChange = { viewModel.updateQuery(it) },
                placeholder = { Text("Search channels, movies, series...") },
                leadingIcon = {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                },
                trailingIcon = {
                    if (uiState.query.isNotEmpty()) {
                        IconButton(onClick = { viewModel.clearSearch() }) {
                            Icon(
                                Icons.Default.Clear,
                                contentDescription = "Clear search"
                            )
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(searchFocusRequester),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { /* Search triggered by debounce */ })
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Filter chips
        TvLazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(SearchFilter.entries) { filter ->
                FilterChip(
                    selected = uiState.filter == filter,
                    onClick = { viewModel.setFilter(filter) }
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        when (filter) {
                            SearchFilter.ALL -> {
                                Icon(
                                    Icons.Default.Search,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            SearchFilter.LIVE -> {
                                Icon(
                                    Icons.Default.LiveTv,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            SearchFilter.MOVIES -> {
                                Icon(
                                    Icons.Default.Movie,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            SearchFilter.SERIES -> {
                                Icon(
                                    Icons.Default.Tv,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = when (filter) {
                                SearchFilter.ALL -> "All"
                                SearchFilter.LIVE -> "Live TV"
                                SearchFilter.MOVIES -> "Movies"
                                SearchFilter.SERIES -> "Series"
                            }
                        )
                    }
                }
            }
        }

        // Show error messages if any data failed to load
        val hasErrors = uiState.channelsError != null ||
            uiState.vodError != null ||
            uiState.seriesError != null
        if (hasErrors && uiState.query.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Some content failed to load",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFef5350)
                )
                androidx.tv.material3.Button(
                    onClick = { viewModel.retry() }
                ) {
                    Text("Retry")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Results
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        val loadingText = if (uiState.isLoadingData) {
                            "Loading content..."
                        } else {
                            "Searching..."
                        }
                        Text(
                            text = loadingText,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        if (uiState.isLoadingData) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "First search may take a moment",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
            }

            uiState.query.isEmpty() -> {
                // ... (keep existing hint logic)
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Search for channels, movies, or series",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Type at least 3 characters to search",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        )
                    }
                }
            }

            !uiState.hasResults && uiState.query.length >= 3 -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "No results found",
                            style = MaterialTheme.typography.headlineSmall
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Try a different search term",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }
            }

            else -> {
                // Results
                androidx.tv.foundation.lazy.list.TvLazyColumn(
                    contentPadding = PaddingValues(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    // Live TV Results
                    if (uiState.liveResults.isNotEmpty()) {
                        item {
                            SearchResultSection(
                                title = "Live TV",
                                count = uiState.liveResults.size,
                                results = uiState.liveResults,
                                onClick = { result ->
                                    coroutineScope.launch {
                                        if (result is SearchResult.Channel) {
                                            val url = viewModel.getChannelStreamUrl(result.channel)
                                            if (url != null) {
                                                onChannelSelected(
                                                    StreamPlayerArgs(
                                                        streamUrl = url,
                                                        streamName = result.channel.name,
                                                        streamId = result.channel.streamId,
                                                        streamType = StreamTypes.LIVE,
                                                        streamIcon = result.channel.streamIcon,
                                                        channelNumber = result.channel.num
                                                    )
                                                )
                                            }
                                        }
                                    }
                                }
                            )
                        }
                    }

                    // Movie Results
                    if (uiState.movieResults.isNotEmpty()) {
                        item {
                            SearchResultSection(
                                title = "Movies",
                                count = uiState.movieResults.size,
                                results = uiState.movieResults,
                                onClick = { result ->
                                    if (result is SearchResult.Vod) {
                                        onVodSelected(result.vod)
                                    }
                                }
                            )
                        }
                    }

                    // Series Results
                    if (uiState.seriesResults.isNotEmpty()) {
                        item {
                            SearchResultSection(
                                title = "Series",
                                count = uiState.seriesResults.size,
                                results = uiState.seriesResults,
                                onClick = { result ->
                                    if (result is SearchResult.Series) {
                                        onSeriesSelected(result.series)
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

@Composable
private fun SearchResultSection(
    title: String,
    count: Int,
    results: List<SearchResult>,
    onClick: (SearchResult) -> Unit
) {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "($count found)",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }

        TvLazyRow(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(horizontal = 4.dp)
        ) {
            items(results) { result ->
                SearchResultCard(
                    result = result,
                    onClick = { onClick(result) }
                )
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun SearchResultCard(
    result: SearchResult,
    onClick: () -> Unit
) {
    val (name, icon, type, aspectRatio) = when (result) {
        is SearchResult.Channel -> Triple4(
            result.channel.name,
            result.channel.streamIcon,
            "LIVE",
            16f / 10f
        )
        is SearchResult.Vod -> Triple4(
            result.vod.name,
            result.vod.streamIcon,
            "MOVIE",
            2f / 3f
        )
        is SearchResult.Series -> Triple4(
            result.series.name,
            result.series.cover,
            "SERIES",
            2f / 3f
        )
    }

    val typeColor = when (result) {
        is SearchResult.Channel -> Color(0xFF00b4d8)
        is SearchResult.Vod -> Color(0xFFe63946)
        is SearchResult.Series -> Color(0xFF2a9d8f)
    }

    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(aspectRatio),
        colors = CardDefaults.colors(containerColor = Color(0xFF1E1E1E)),
        border = CardDefaults.border(
            focusedBorder = Border(border = BorderStroke(2.dp, typeColor))
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Image
            if (!icon.isNullOrBlank()) {
                val imageScale = if (result is SearchResult.Channel) {
                    ContentScale.Fit
                } else {
                    ContentScale.Crop
                }
                AsyncImage(
                    model = icon,
                    contentDescription = name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = imageScale
                )
            }

            // Type badge
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(typeColor)
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = type,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }

            // Name overlay
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(Color.Black.copy(alpha = 0.7f))
                    .padding(8.dp)
            ) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

private data class Triple4<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

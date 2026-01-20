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

import androidx.activity.compose.BackHandler
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
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
import androidx.tv.material3.Border
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import androidx.tv.material3.surfaceColorAtElevation
import coil.compose.AsyncImage
import com.google.jetstream.data.models.xtream.XtreamVodItem
import com.google.jetstream.presentation.utils.CountryFilter
import com.google.jetstream.presentation.utils.CountryFilterRow
import com.google.jetstream.presentation.utils.DefaultCountryFilters
import com.google.jetstream.presentation.utils.focusBorderStroke
import com.google.jetstream.presentation.utils.headerBackdropBrush
import com.google.jetstream.presentation.theme.JetStreamCardShape

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun XtreamVodScreen(
    onVodSelected: (XtreamVodItem) -> Unit,
    onScroll: (isTopBarVisible: Boolean) -> Unit = {},
    viewModel: XtreamVodScreenViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    when (val state = uiState) {
        is XtreamVodUiState.Loading -> {
            onScroll(true)
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Loading countries...",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        is XtreamVodUiState.Error -> {
            onScroll(true)
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Error loading movies",
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

        is XtreamVodUiState.CountriesLoaded -> {
            onScroll(true)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 48.dp, vertical = 24.dp)
            ) {
                val headerBrush = headerBackdropBrush()
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(headerBrush)
                        .padding(bottom = 8.dp)
                ) {
                    Text(
                        text = "Movies",
                        style = MaterialTheme.typography.headlineLarge,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    Text(
                        text = "Select a country to browse",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                    )
                }

                if (state.categories.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No categories found",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                } else {
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

                    TvLazyVerticalGrid(
                        columns = TvGridCells.Fixed(3),
                        state = gridState,
                        contentPadding = PaddingValues(bottom = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(DefaultCountryFilters, key = { it.name }) { country ->
                            CountryCard(
                                country = country,
                                onClick = { viewModel.selectCountry(country) }
                            )
                        }
                    }
                }
            }
        }

        is XtreamVodUiState.Ready -> {
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

            // Handle back to go to country selection
            BackHandler {
                viewModel.goBackToCountries()
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 48.dp, vertical = 24.dp)
            ) {
                val headerBrush = headerBackdropBrush()
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(headerBrush)
                        .padding(bottom = 8.dp)
                ) {
                    // Header with back hint
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Movies",
                            style = MaterialTheme.typography.headlineLarge
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "/ ${state.selectedCountry.displayName}",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    CountryFilterRow(
                        selectedCountry = state.selectedCountry,
                        onCountrySelected = { viewModel.selectCountry(it) }
                    )

                    if (!state.isLoadingMovies) {
                        Text(
                            text = "${state.selectedCountry.displayName} · " +
                                "${state.vodItems.size} movies",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (state.isLoadingMovies) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Loading movies...",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                } else if (state.vodItems.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "No movies found for ${state.selectedCountry.displayName}",
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
                        items(state.vodItems, key = { it.streamId }) { vodItem ->
                            VodCard(
                                vodItem = vodItem,
                                onClick = {
                                    onVodSelected(vodItem)
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
private fun CountryCard(
    country: CountryFilter,
    onClick: () -> Unit
) {
    val container = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
    val focusedContainer = MaterialTheme.colorScheme.surfaceColorAtElevation(6.dp)
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f),
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF2a2a2a),
                            Color(0xFF1a1a1a)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = country.displayName,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun VodCard(
    vodItem: XtreamVodItem,
    onClick: () -> Unit
) {
    val container = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
    val focusedContainer = MaterialTheme.colorScheme.surfaceColorAtElevation(6.dp)
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(2f / 3f),
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
            // Movie Poster
            if (!vodItem.streamIcon.isNullOrBlank()) {
                AsyncImage(
                    model = vodItem.streamIcon,
                    contentDescription = vodItem.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            // Movie Name overlay at bottom
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(8.dp)
            ) {
                Text(
                    text = vodItem.name,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    color = Color.White,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Rating badge
            vodItem.rating?.let { rating ->
                if (rating.isNotBlank() && rating != "0") {
                    Text(
                        text = rating,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                    )
                }
            }
        }
    }
}

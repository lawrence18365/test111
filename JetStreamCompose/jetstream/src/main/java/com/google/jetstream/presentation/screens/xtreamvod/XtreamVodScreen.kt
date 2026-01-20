/*
 * Xtream VOD (Movies) Screen - Displays movies from Xtream Codes
 * Fixed: Load by category to prevent OOM crash
 */
package com.google.jetstream.presentation.screens.xtreamvod

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.tv.material3.Border
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import com.google.jetstream.data.models.xtream.XtreamCategory
import com.google.jetstream.data.models.xtream.XtreamVodItem
import com.google.jetstream.presentation.screens.streamPlayer.StreamPlayerArgs
import com.google.jetstream.presentation.screens.streamPlayer.StreamTypes
import kotlinx.coroutines.launch

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun XtreamVodScreen(
    onVodSelected: (StreamPlayerArgs) -> Unit,
    onScroll: (isTopBarVisible: Boolean) -> Unit = {},
    viewModel: XtreamVodScreenViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    when (val state = uiState) {
        is XtreamVodUiState.Loading -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Loading categories...",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        is XtreamVodUiState.Error -> {
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

        is XtreamVodUiState.CategoriesLoaded -> {
            // Show category selection grid
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 48.dp, vertical = 24.dp)
            ) {
                Text(
                    text = "Movies",
                    style = MaterialTheme.typography.headlineLarge,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    text = "Select a category to browse",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

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
                    TvLazyVerticalGrid(
                        columns = TvGridCells.Fixed(4),
                        contentPadding = PaddingValues(bottom = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(state.categories, key = { it.categoryId }) { category ->
                            CategoryCard(
                                category = category,
                                onClick = { viewModel.selectCategory(category.categoryId) }
                            )
                        }
                    }
                }
            }
        }

        is XtreamVodUiState.Ready -> {
            // Handle back to go to category selection
            BackHandler {
                viewModel.goBackToCategories()
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 48.dp, vertical = 24.dp)
            ) {
                // Header with back hint
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    Text(
                        text = "Movies",
                        style = MaterialTheme.typography.headlineLarge
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    val categoryName = state.categories
                        .firstOrNull { it.categoryId == state.selectedCategoryId }
                        ?.categoryName ?: "All"
                    Text(
                        text = "/ $categoryName",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color(0xFF00b4d8)
                    )
                }

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
                        Text(
                            text = "No movies found in this category",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                } else {
                    Text(
                        text = "${state.vodItems.size} movies",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    TvLazyVerticalGrid(
                        columns = TvGridCells.Fixed(5),
                        contentPadding = PaddingValues(bottom = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(state.vodItems, key = { it.streamId }) { vodItem ->
                            VodCard(
                                vodItem = vodItem,
                                onClick = {
                                    coroutineScope.launch {
                                        val streamUrl = viewModel.getStreamUrl(vodItem)
                                        if (streamUrl != null) {
                                            val categoryName = state.categories
                                                .firstOrNull { it.categoryId == vodItem.categoryId }
                                                ?.categoryName
                                            onVodSelected(
                                                StreamPlayerArgs(
                                                    streamUrl = streamUrl,
                                                    streamName = vodItem.name,
                                                    streamId = vodItem.streamId,
                                                    streamType = StreamTypes.VOD,
                                                    streamIcon = vodItem.streamIcon,
                                                    categoryName = categoryName
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
private fun CategoryCard(
    category: XtreamCategory,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f),
        colors = CardDefaults.colors(
            containerColor = Color(0xFF1E1E1E)
        ),
        border = CardDefaults.border(
            focusedBorder = Border(
                border = BorderStroke(2.dp, Color(0xFF00b4d8))
            )
        )
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
                text = category.categoryName,
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
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(2f / 3f),
        colors = CardDefaults.colors(
            containerColor = Color(0xFF1E1E1E)
        ),
        border = CardDefaults.border(
            focusedBorder = Border(
                border = BorderStroke(2.dp, Color(0xFF00b4d8))
            )
        )
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

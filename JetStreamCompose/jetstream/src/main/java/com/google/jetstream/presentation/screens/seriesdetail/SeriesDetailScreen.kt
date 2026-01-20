/*
 * Series Detail Screen - Shows series info, seasons, and episodes
 */
package com.google.jetstream.presentation.screens.seriesdetail

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.tv.foundation.lazy.list.TvLazyColumn
import androidx.tv.foundation.lazy.list.TvLazyRow
import androidx.tv.foundation.lazy.list.items
import androidx.tv.material3.Border
import androidx.tv.material3.Button
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.FilterChip
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import com.google.jetstream.data.models.xtream.XtreamEpisode
import com.google.jetstream.data.models.xtream.XtreamSeason
import com.google.jetstream.presentation.screens.streamPlayer.StreamPlayerArgs
import com.google.jetstream.presentation.screens.streamPlayer.StreamTypes
import kotlinx.coroutines.launch

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun SeriesDetailScreen(
    onEpisodeSelected: (StreamPlayerArgs) -> Unit,
    onBackPressed: () -> Unit,
    viewModel: SeriesDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    when (val state = uiState) {
        is SeriesDetailUiState.Loading -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Loading series info...",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        is SeriesDetailUiState.Error -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Error loading series",
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color(0xFFef5350)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = state.message,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { viewModel.retry() }) {
                        Text("Retry")
                    }
                }
            }
        }

        is SeriesDetailUiState.Ready -> {
            val info = state.seriesInfo.info

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 48.dp, vertical = 24.dp)
            ) {
                // Left side - Series info
                Column(
                    modifier = Modifier
                        .weight(0.35f)
                        .fillMaxHeight()
                ) {
                    // Cover image
                    if (!info?.cover.isNullOrBlank()) {
                        AsyncImage(
                            model = info?.cover,
                            contentDescription = info?.name,
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(2f / 3f)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Title
                    Text(
                        text = info?.name ?: "Unknown Series",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    // Genre & Rating
                    Row(
                        modifier = Modifier.padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        info?.genre?.let { genre ->
                            Text(
                                text = genre,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                        info?.rating?.let { rating ->
                            if (rating.isNotBlank()) {
                                Text(
                                    text = "Rating: $rating",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFffd700)
                                )
                            }
                        }
                    }

                    // Plot
                    info?.plot?.let { plot ->
                        if (plot.isNotBlank()) {
                            Text(
                                text = plot,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                maxLines = 6,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }

                    // Cast & Director
                    info?.cast?.let { cast ->
                        if (cast.isNotBlank()) {
                            Text(
                                text = "Cast: $cast",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(32.dp))

                // Right side - Seasons and Episodes
                Column(
                    modifier = Modifier
                        .weight(0.65f)
                        .fillMaxHeight()
                ) {
                    // Season selector
                    if (state.seasons.isNotEmpty()) {
                        Text(
                            text = "Seasons",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        TvLazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            items(state.seasons) { season ->
                                FilterChip(
                                    selected = state.selectedSeason == season.seasonNumber,
                                    onClick = { viewModel.selectSeason(season.seasonNumber) }
                                ) {
                                    Text(
                                        text = season.name ?: "Season ${season.seasonNumber}"
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Episodes
                    Text(
                        text = "Episodes (${state.episodes.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    if (state.episodes.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No episodes found for this season",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                    } else {
                        TvLazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(bottom = 24.dp)
                        ) {
                            items(state.episodes, key = { it.id ?: it.episodeNum }) { episode ->
                                EpisodeCard(
                                    episode = episode,
                                    seriesName = info?.name ?: "Series",
                                    seasonNumber = state.selectedSeason,
                                    onClick = {
                                        coroutineScope.launch {
                                            val url = viewModel.getEpisodeStreamUrl(episode)
                                            if (url != null) {
                                                val episodeName = episode.title
                                                    ?: "S${state.selectedSeason}E${episode.episodeNum}"
                                                onEpisodeSelected(
                                                    StreamPlayerArgs(
                                                        streamUrl = url,
                                                        streamName = "${info?.name ?: "Series"} - $episodeName",
                                                        streamId = episode.id?.toIntOrNull() ?: 0,
                                                        streamType = StreamTypes.SERIES,
                                                        streamIcon = episode.info?.movieImage
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
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun EpisodeCard(
    episode: XtreamEpisode,
    seriesName: String,
    seasonNumber: Int,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp),
        colors = CardDefaults.colors(
            containerColor = Color(0xFF1E1E1E)
        ),
        border = CardDefaults.border(
            focusedBorder = Border(
                border = BorderStroke(2.dp, Color(0xFF00b4d8))
            )
        )
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Episode thumbnail
            Box(
                modifier = Modifier
                    .width(150.dp)
                    .fillMaxHeight()
            ) {
                if (!episode.info?.movieImage.isNullOrBlank()) {
                    AsyncImage(
                        model = episode.info?.movieImage,
                        contentDescription = episode.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(
                                        Color(0xFF2a2a2a),
                                        Color(0xFF1a1a1a)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "E${episode.episodeNum}",
                            style = MaterialTheme.typography.headlineMedium,
                            color = Color.White.copy(alpha = 0.3f)
                        )
                    }
                }

                // Episode number badge
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(4.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFF00b4d8))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "E${episode.episodeNum}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Episode info
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = episode.title ?: "Episode ${episode.episodeNum}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = "Season $seasonNumber, Episode ${episode.episodeNum}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )

                episode.info?.plot?.let { plot ->
                    if (plot.isNotBlank()) {
                        Text(
                            text = plot,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }

                // Duration if available
                episode.info?.duration?.let { duration ->
                    if (duration.isNotBlank()) {
                        Text(
                            text = duration,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

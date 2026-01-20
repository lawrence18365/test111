package com.google.jetstream.presentation.screens.xtreamvod

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.tv.material3.Button
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import com.google.jetstream.data.models.xtream.XtreamVodInfo
import com.google.jetstream.data.models.xtream.XtreamVodMovieData
import com.google.jetstream.presentation.screens.streamPlayer.StreamPlayerArgs
import com.google.jetstream.presentation.screens.streamPlayer.StreamTypes
import com.google.jetstream.presentation.utils.CastChipsRow
import com.google.jetstream.presentation.utils.QualityBadgesRow
import com.google.jetstream.presentation.utils.openYouTube
import kotlinx.coroutines.launch

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun XtreamVodDetailScreen(
    onPlaySelected: (StreamPlayerArgs) -> Unit,
    onCastSelected: (String) -> Unit = {},
    viewModel: XtreamVodDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    when (val state = uiState) {
        is XtreamVodDetailUiState.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Loading movie info...")
            }
        }
        is XtreamVodDetailUiState.Error -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Error: ${state.message}", color = Color.Red)
            }
        }
        is XtreamVodDetailUiState.Ready -> {
            val info = state.vodInfo.info
            val movieData = state.vodInfo.movieData
            val poster = info?.movieImage ?: info?.coverBig ?: movieData?.streamIcon
            val title = info?.name ?: movieData?.name ?: "Unknown title"
            val trailerId = info?.youtubeTrailer?.trim().orEmpty()
            val hasTrailer = trailerId.isNotEmpty()

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 48.dp, vertical = 24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .width(320.dp)
                        .fillMaxHeight()
                ) {
                    if (!poster.isNullOrBlank()) {
                        AsyncImage(
                            model = poster,
                            contentDescription = title,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(420.dp),
                            contentScale = ContentScale.Crop
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (hasTrailer) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        val url = viewModel.getStreamUrl(movieData)
                                        if (url != null) {
                                            onPlaySelected(
                                                StreamPlayerArgs(
                                                    streamUrl = url,
                                                    streamName = title,
                                                    streamId = movieData?.streamId ?: 0,
                                                    streamType = StreamTypes.VOD,
                                                    streamIcon = poster
                                                )
                                            )
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Play")
                            }
                            Button(
                                onClick = { openYouTube(context, trailerId) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Watch Trailer")
                            }
                        }
                    } else {
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    val url = viewModel.getStreamUrl(movieData)
                                    if (url != null) {
                                        onPlaySelected(
                                            StreamPlayerArgs(
                                                streamUrl = url,
                                                streamName = title,
                                                streamId = movieData?.streamId ?: 0,
                                                streamType = StreamTypes.VOD,
                                                streamIcon = poster
                                            )
                                        )
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Play")
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    info?.rating?.let { rating ->
                        if (rating.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Rating: $rating",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color(0xFFFFB74D)
                            )
                        }
                    }

                    info?.genre?.let { genre ->
                        if (genre.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = genre,
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        }
                    }

                    QualityBadgesRow(
                        videoInfo = info?.video,
                        audioInfo = info?.audio,
                        modifier = Modifier.padding(top = 8.dp)
                    )

                    if (!info?.cast.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Cast",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        CastChipsRow(
                            cast = info?.cast,
                            onCastSelected = onCastSelected
                        )
                    }
                }

                Spacer(modifier = Modifier.width(32.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    val backdrop = info?.backdropPath?.firstOrNull()
                    if (!backdrop.isNullOrBlank()) {
                        AsyncImage(
                            model = backdrop,
                            contentDescription = title,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(260.dp),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    InfoBlock(info = info, movieData = movieData)
                }
            }
        }
    }
}

@Composable
private fun InfoBlock(
    info: XtreamVodInfo?,
    movieData: XtreamVodMovieData?
) {
    Text(
        text = info?.plot ?: "No plot available",
        style = MaterialTheme.typography.bodyMedium,
        color = Color.White.copy(alpha = 0.8f),
        maxLines = 8,
        overflow = TextOverflow.Ellipsis
    )

    Spacer(modifier = Modifier.height(16.dp))

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        DetailLine(label = "Director", value = info?.director)
        DetailLine(label = "Release", value = info?.releaseDate)
        DetailLine(label = "Duration", value = info?.duration)
        DetailLine(label = "Category", value = movieData?.categoryId)
    }
}

@Composable
private fun DetailLine(label: String, value: String?) {
    if (value.isNullOrBlank()) return
    Text(
        text = "$label: $value",
        style = MaterialTheme.typography.bodySmall,
        color = Color.White.copy(alpha = 0.6f)
    )
}

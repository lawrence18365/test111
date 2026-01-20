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

/*
 * Stream Player Screen - Premium IPTV playback experience
 * Features: Channel info overlay, favorites toggle, EPG info, quality settings
 */
package com.google.jetstream.presentation.screens.streamPlayer

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.pm.PackageManager
import android.os.Build
import android.util.Rational
import android.view.KeyEvent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.compose.PlayerSurface
import androidx.media3.ui.compose.SURFACE_TYPE_TEXTURE_VIEW
import androidx.media3.ui.compose.modifiers.resizeWithContentScale
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.IconButton
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.google.jetstream.presentation.screens.videoPlayer.components.VideoPlayerPulse
import com.google.jetstream.presentation.screens.videoPlayer.components.VideoPlayerPulse.Type.BACK
import com.google.jetstream.presentation.screens.videoPlayer.components.VideoPlayerPulse.Type.FORWARD
import com.google.jetstream.presentation.screens.videoPlayer.components.rememberVideoPlayerPulseState
import com.google.jetstream.presentation.screens.videoPlayer.components.rememberVideoPlayerState
import com.google.jetstream.presentation.utils.handleDPadKeyEvents
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalTvMaterial3Api::class)
@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun StreamPlayerScreen(
    streamArgs: StreamPlayerArgs,
    onBackPressed: () -> Unit,
) {
    val context = LocalContext.current
    val viewModel: StreamPlayerViewModel = hiltViewModel()
    val activity = context as? Activity
    val canEnterPip = activity?.packageManager
        ?.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE) == true
    var currentStream by remember(streamArgs) {
        mutableStateOf(
            streamArgs.copy(
                streamType = streamArgs.streamType.ifBlank { StreamTypes.LIVE }
            )
        )
    }
    val streamType = currentStream.streamType
    val isFavorite by viewModel.isFavorite(currentStream.streamId).collectAsState(initial = false)
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isBuffering by remember { mutableStateOf(true) }
    var isPlaying by remember { mutableStateOf(true) }
    var currentTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var showChannelInfo by remember { mutableStateOf(true) }
    var channelInput by remember { mutableStateOf("") }
    var zapFeedback by remember { mutableStateOf<String?>(null) }

    val enterPip = {
        if (activity != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val params = PictureInPictureParams.Builder()
                .setAspectRatio(Rational(16, 9))
                .build()
            activity.enterPictureInPictureMode(params)
        }
    }

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            playWhenReady = true
        }
    }

    // Update time every second
    LaunchedEffect(Unit) {
        while (true) {
            currentTime = System.currentTimeMillis()
            delay(1000)
        }
    }

    // Auto-hide channel info after 5 seconds
    LaunchedEffect(showChannelInfo) {
        if (showChannelInfo) {
            delay(5000)
            showChannelInfo = false
        }
    }

    LaunchedEffect(channelInput) {
        if (channelInput.isBlank()) return@LaunchedEffect
        val pendingInput = channelInput
        delay(1200)
        if (channelInput != pendingInput) return@LaunchedEffect

        channelInput = ""
        val channelNumber = pendingInput.toIntOrNull()
        if (channelNumber == null) {
            zapFeedback = "Invalid channel"
            return@LaunchedEffect
        }

        val nextStream = viewModel.buildStreamArgsForChannelNumber(channelNumber)
        if (nextStream != null) {
            currentStream = nextStream
            showChannelInfo = true
        } else {
            zapFeedback = "Channel $channelNumber not found"
        }
    }

    LaunchedEffect(zapFeedback) {
        if (zapFeedback == null) return@LaunchedEffect
        delay(1500)
        zapFeedback = null
    }

    // Player event listener
    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                isBuffering = state == Player.STATE_BUFFERING
                if (state == Player.STATE_READY) {
                    errorMessage = null
                }
            }

            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onPlayerError(error: PlaybackException) {
                errorMessage = when (error.errorCode) {
                    PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED ->
                        "Network connection failed"
                    PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT ->
                        "Connection timeout"
                    PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED ->
                        "Stream format not supported"
                    else -> "Playback error: ${error.message}"
                }
            }
        }
        exoPlayer.addListener(listener)

        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    LaunchedEffect(currentStream.streamId, currentStream.streamType) {
        viewModel.recordPlaybackStart(currentStream)
    }

    // Set up media item
    LaunchedEffect(currentStream.streamUrl) {
        val mediaItem = MediaItem.Builder()
            .setUri(currentStream.streamUrl)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(currentStream.streamName)
                    .build()
            )
            .build()

        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()

        if (
            streamType == StreamTypes.VOD ||
            streamType == StreamTypes.SERIES ||
            streamType == StreamTypes.CATCHUP
        ) {
            val lastPosition = viewModel.getLastPosition(currentStream.streamId)
            if (lastPosition != null && lastPosition > 0L) {
                exoPlayer.seekTo(lastPosition)
            }
        }
    }

    LaunchedEffect(currentStream.streamId, streamType) {
        if (
            streamType != StreamTypes.VOD &&
            streamType != StreamTypes.SERIES &&
            streamType != StreamTypes.CATCHUP
        ) {
            return@LaunchedEffect
        }

        while (true) {
            delay(10_000)
            val position = exoPlayer.currentPosition
            val duration = exoPlayer.duration
            if (duration > 0L) {
                viewModel.updatePlaybackProgress(currentStream.streamId, position, duration)
            }
        }
    }

    BackHandler(onBack = onBackPressed)

    val videoPlayerState = rememberVideoPlayerState(hideSeconds = 4)
    val pulseState = rememberVideoPlayerPulseState()

    val dpadModifier = Modifier.handleDPadKeyEvents(
        onLeft = {
            if (!showChannelInfo) {
                exoPlayer.seekBack()
                pulseState.setType(BACK)
            }
        },
        onRight = {
            if (!showChannelInfo) {
                exoPlayer.seekForward()
                pulseState.setType(FORWARD)
            }
        },
        onUp = {
            showChannelInfo = true
        },
        onDown = {
            showChannelInfo = true
        },
        onEnter = {
            if (exoPlayer.isPlaying) {
                exoPlayer.pause()
            } else {
                exoPlayer.play()
            }
            showChannelInfo = true
        }
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .then(dpadModifier)
            .onPreviewKeyEvent {
                if (it.nativeKeyEvent.action != KeyEvent.ACTION_UP) return@onPreviewKeyEvent false

                val keyCode = it.nativeKeyEvent.keyCode
                val digit = when (keyCode) {
                    KeyEvent.KEYCODE_0, KeyEvent.KEYCODE_NUMPAD_0 -> 0
                    KeyEvent.KEYCODE_1, KeyEvent.KEYCODE_NUMPAD_1 -> 1
                    KeyEvent.KEYCODE_2, KeyEvent.KEYCODE_NUMPAD_2 -> 2
                    KeyEvent.KEYCODE_3, KeyEvent.KEYCODE_NUMPAD_3 -> 3
                    KeyEvent.KEYCODE_4, KeyEvent.KEYCODE_NUMPAD_4 -> 4
                    KeyEvent.KEYCODE_5, KeyEvent.KEYCODE_NUMPAD_5 -> 5
                    KeyEvent.KEYCODE_6, KeyEvent.KEYCODE_NUMPAD_6 -> 6
                    KeyEvent.KEYCODE_7, KeyEvent.KEYCODE_NUMPAD_7 -> 7
                    KeyEvent.KEYCODE_8, KeyEvent.KEYCODE_NUMPAD_8 -> 8
                    KeyEvent.KEYCODE_9, KeyEvent.KEYCODE_NUMPAD_9 -> 9
                    else -> null
                }

                when {
                    digit != null -> {
                        if (streamType != StreamTypes.LIVE && streamType != StreamTypes.CATCHUP) {
                            return@onPreviewKeyEvent false
                        }
                        channelInput = (channelInput + digit).take(4)
                        showChannelInfo = true
                        true
                    }
                    keyCode == KeyEvent.KEYCODE_DEL || keyCode == KeyEvent.KEYCODE_CLEAR -> {
                        channelInput = channelInput.dropLast(1)
                        true
                    }
                    keyCode == KeyEvent.KEYCODE_INFO || keyCode == KeyEvent.KEYCODE_I -> {
                        showChannelInfo = true
                        true
                    }
                    else -> false
                }
            }
            .focusable()
    ) {
        // Video Surface
        PlayerSurface(
            player = exoPlayer,
            surfaceType = SURFACE_TYPE_TEXTURE_VIEW,
            modifier = Modifier.resizeWithContentScale(
                contentScale = ContentScale.Fit,
                sourceSizeDp = null
            )
        )

        // Top gradient overlay
        AnimatedVisibility(
            visible = showChannelInfo,
            enter = fadeIn() + slideInVertically { -it },
            exit = fadeOut() + slideOutVertically { -it }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.8f),
                                Color.Transparent
                            )
                        )
                    )
            )
        }

        // Bottom gradient overlay
        AnimatedVisibility(
            visible = showChannelInfo,
            enter = fadeIn() + slideInVertically { it },
            exit = fadeOut() + slideOutVertically { it }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.8f)
                            )
                        )
                    )
            )
        }

        // Channel Info Overlay (Top)
        AnimatedVisibility(
            visible = showChannelInfo,
            enter = fadeIn() + slideInVertically { -it },
            exit = fadeOut() + slideOutVertically { -it },
            modifier = Modifier.align(Alignment.TopStart)
        ) {
            ChannelInfoOverlay(
                channelName = currentStream.streamName,
                channelNumber = currentStream.channelNumber,
                channelIcon = currentStream.streamIcon,
                currentTime = currentTime,
                streamType = streamType,
                programTitle = currentStream.programTitle,
                programStart = currentStream.programStart,
                programEnd = currentStream.programEnd,
                showPip = canEnterPip,
                onEnterPip = enterPip,
                isFavorite = isFavorite,
                onToggleFavorite = { viewModel.toggleFavorite(currentStream) }
            )
        }

        // Playback Controls (Bottom)
        AnimatedVisibility(
            visible = showChannelInfo,
            enter = fadeIn() + slideInVertically { it },
            exit = fadeOut() + slideOutVertically { it },
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            PlaybackControlsOverlay(
                isPlaying = isPlaying,
                onPlayPause = {
                    if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
                }
            )
        }

        AnimatedVisibility(
            visible = channelInput.isNotBlank(),
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.Black.copy(alpha = 0.7f))
                    .padding(horizontal = 32.dp, vertical = 20.dp)
            ) {
                Text(
                    text = "Channel $channelInput",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        AnimatedVisibility(
            visible = zapFeedback != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Box(
                modifier = Modifier
                    .padding(bottom = 32.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(alpha = 0.8f))
                    .padding(horizontal = 20.dp, vertical = 10.dp)
            ) {
                Text(
                    text = zapFeedback.orEmpty(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White
                )
            }
        }

        // Buffering indicator
        if (isBuffering && errorMessage == null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    androidx.compose.material3.CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Loading stream...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White
                    )
                }
            }
        }

        // Error message
        errorMessage?.let { error ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.7f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF1E1E1E))
                        .padding(32.dp)
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = Color(0xFFef5350),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Stream Error",
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "Press BACK to return",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.5f)
                    )
                }
            }
        }

        // Pulse feedback for seeking
        VideoPlayerPulse(pulseState)
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun ChannelInfoOverlay(
    channelName: String,
    channelNumber: Int?,
    channelIcon: String?,
    currentTime: Long,
    streamType: String,
    programTitle: String?,
    programStart: Long?,
    programEnd: Long?,
    showPip: Boolean,
    onEnterPip: () -> Unit,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit
) {
    val timeFormat = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }
    val dateFormat = remember { SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Left side - Channel info
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Channel icon
            if (channelIcon != null) {
                AsyncImage(
                    model = channelIcon,
                    contentDescription = channelName,
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White.copy(alpha = 0.1f)),
                    contentScale = ContentScale.Fit
                )
                Spacer(modifier = Modifier.width(16.dp))
            }

            Column {
                if (channelNumber != null && channelNumber > 0) {
                    Text(
                        text = "CH $channelNumber",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                }

                Text(
                    text = channelName,
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )

                if (!programTitle.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = programTitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                    if (programStart != null && programEnd != null) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${timeFormat.format(Date(programStart))} - ${timeFormat.format(Date(programEnd))}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                val streamLabel = when (streamType) {
                    StreamTypes.LIVE -> "LIVE"
                    StreamTypes.CATCHUP -> "CATCH-UP"
                    StreamTypes.VOD -> "VOD"
                    StreamTypes.SERIES -> "SERIES"
                    else -> null
                }
                val labelColor = when (streamType) {
                    StreamTypes.LIVE -> Color.Red
                    StreamTypes.CATCHUP -> Color(0xFFFFB74D)
                    StreamTypes.VOD -> Color(0xFF90CAF9)
                    StreamTypes.SERIES -> Color(0xFF80CBC4)
                    else -> Color.White
                }

                if (streamLabel != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (streamType == StreamTypes.LIVE) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(labelColor)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        Text(
                            text = streamLabel,
                            style = MaterialTheme.typography.labelMedium,
                            color = labelColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Right side - Time and actions
        Column(
            horizontalAlignment = Alignment.End
        ) {
            // Current time
            Text(
                text = timeFormat.format(Date(currentTime)),
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = dateFormat.format(Date(currentTime)),
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.7f)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Favorite button
            Row {
                if (showPip) {
                    IconButton(onClick = onEnterPip) {
                        Icon(
                            Icons.Default.PictureInPictureAlt,
                            contentDescription = "Picture-in-Picture",
                            tint = Color.White
                        )
                    }
                }
                IconButton(onClick = onToggleFavorite) {
                    Icon(
                        if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = if (isFavorite) "Remove from favorites" else "Add to favorites",
                        tint = if (isFavorite) Color.Red else Color.White
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun PlaybackControlsOverlay(
    isPlaying: Boolean,
    onPlayPause: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Play/Pause button
        IconButton(
            onClick = onPlayPause,
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.2f))
        ) {
            Icon(
                if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (isPlaying) "Pause" else "Play",
                tint = Color.White,
                modifier = Modifier.size(32.dp)
            )
        }
    }

    // Control hints
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        ControlHint(icon = Icons.Default.Schedule, text = "←/→ Seek")
        ControlHint(icon = Icons.Default.LiveTv, text = "OK Play/Pause")
        ControlHint(icon = Icons.Default.HighQuality, text = "↑/↓ Info")
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun ControlHint(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(Color.White.copy(alpha = 0.1f))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.7f),
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.7f)
        )
    }
}

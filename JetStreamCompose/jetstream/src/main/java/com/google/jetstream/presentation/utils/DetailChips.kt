/*
 * Copyright 2026 Google LLC
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

package com.google.jetstream.presentation.utils

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.tv.foundation.lazy.list.TvLazyRow
import androidx.tv.foundation.lazy.list.items
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.FilterChip
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.google.jetstream.data.models.xtream.XtreamAudioInfo
import com.google.jetstream.data.models.xtream.XtreamVideoInfo

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun CastChipsRow(
    cast: String?,
    onCastSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val castMembers = remember(cast) {
        cast.orEmpty()
            .split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
    }

    if (castMembers.isEmpty()) return

    TvLazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(end = 8.dp)
    ) {
        items(castMembers, key = { it }) { actor ->
            FilterChip(
                selected = false,
                onClick = { onCastSelected(actor) }
            ) {
                Text(actor)
            }
        }
    }
}

@Composable
fun QualityBadgesRow(
    videoInfo: XtreamVideoInfo?,
    audioInfo: XtreamAudioInfo?,
    modifier: Modifier = Modifier
) {
    val badges = remember(videoInfo, audioInfo) {
        buildList {
            val width = videoInfo?.width ?: 0
            if (width >= 3840) {
                add(QualityBadge("[4K UHD]", Color(0xFFD4AF37), Color(0xFF1C1C1C)))
            }
            val channels = audioInfo?.channels ?: 0
            if (channels >= 6) {
                add(QualityBadge("[5.1 Surround]", Color(0xFF455A64), Color.White))
            }
            if (videoInfo?.codecName?.equals("hevc", ignoreCase = true) == true) {
                add(QualityBadge("[HDR]", Color(0xFF1E88E5), Color.White))
            }
        }
    }

    if (badges.isEmpty()) return

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        badges.forEach { badge ->
            BadgeChip(
                label = badge.label,
                background = badge.background,
                textColor = badge.textColor
            )
        }
    }
}

private data class QualityBadge(
    val label: String,
    val background: Color,
    val textColor: Color
)

@Composable
private fun BadgeChip(
    label: String,
    background: Color,
    textColor: Color
) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = textColor,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(background)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    )
}

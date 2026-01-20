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

package com.google.jetstream.presentation.screens.streamPlayer

import com.google.jetstream.presentation.screens.Screens
import java.net.URLEncoder

data class StreamPlayerArgs(
    val streamUrl: String,
    val streamName: String,
    val streamId: Int,
    val streamType: String,
    val streamIcon: String? = null,
    val categoryName: String? = null,
    val channelNumber: Int? = null,
    val programTitle: String? = null,
    val programStart: Long? = null,
    val programEnd: Long? = null
)

object StreamTypes {
    const val LIVE = "live"
    const val VOD = "vod"
    const val SERIES = "series"
    const val CATCHUP = "catchup"
}

fun StreamPlayerArgs.toRoute(): String {
    fun encode(value: String) = URLEncoder.encode(value, "UTF-8")

    return Screens.StreamPlayer.withArgs(
        encode(streamUrl),
        encode(streamName),
        streamId,
        encode(streamType),
        encode(streamIcon.orEmpty()),
        encode(categoryName.orEmpty()),
        channelNumber ?: -1,
        encode(programTitle.orEmpty()),
        programStart ?: 0L,
        programEnd ?: 0L
    )
}

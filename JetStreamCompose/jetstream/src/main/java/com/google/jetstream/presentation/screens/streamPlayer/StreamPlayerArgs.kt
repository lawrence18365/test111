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

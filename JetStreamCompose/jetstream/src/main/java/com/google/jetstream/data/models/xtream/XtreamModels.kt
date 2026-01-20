/*
 * Xtream Codes API Data Models
 */
package com.google.jetstream.data.models.xtream

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Response from player_api.php authentication call
 */
@Serializable
data class XtreamAuthResponse(
    @SerialName("user_info") val userInfo: UserInfo,
    @SerialName("server_info") val serverInfo: ServerInfo
)

@Serializable
data class UserInfo(
    val username: String,
    val password: String,
    val message: String? = null,
    val auth: Int = 0,
    val status: String,
    @SerialName("exp_date") val expDate: String? = null,
    @SerialName("is_trial") val isTrial: String? = null,
    @SerialName("active_cons") val activeCons: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("max_connections") val maxConnections: String? = null,
    @SerialName("allowed_output_formats") val allowedOutputFormats: List<String> = emptyList()
)

@Serializable
data class ServerInfo(
    val url: String? = null,
    val port: String? = null,
    @SerialName("https_port") val httpsPort: String? = null,
    @SerialName("server_protocol") val serverProtocol: String? = null,
    @SerialName("rtmp_port") val rtmpPort: String? = null,
    val timezone: String? = null,
    @SerialName("timestamp_now") val timestampNow: Long? = null,
    @SerialName("time_now") val timeNow: String? = null
)

/**
 * Live stream channel from get_live_streams action
 */
@Serializable
data class XtreamChannel(
    val num: Int? = null,
    val name: String,
    @SerialName("stream_type") val streamType: String? = null,
    @SerialName("stream_id") val streamId: Int,
    @SerialName("stream_icon") val streamIcon: String? = null,
    @SerialName("epg_channel_id") val epgChannelId: String? = null,
    val added: String? = null,
    @SerialName("category_id") val categoryId: String? = null,
    @SerialName("custom_sid") val customSid: String? = null,
    @SerialName("tv_archive") val tvArchive: Int? = null,
    @SerialName("direct_source") val directSource: String? = null,
    @SerialName("tv_archive_duration") val tvArchiveDuration: Int? = null
)

/**
 * Category from get_live_categories action
 */
@Serializable
data class XtreamCategory(
    @SerialName("category_id") val categoryId: String,
    @SerialName("category_name") val categoryName: String,
    @SerialName("parent_id") val parentId: Int? = null
)

/**
 * VOD (Video on Demand) item from get_vod_streams action
 */
@Serializable
data class XtreamVodItem(
    val num: Int? = null,
    val name: String,
    @SerialName("stream_type") val streamType: String? = null,
    @SerialName("stream_id") val streamId: Int,
    @SerialName("stream_icon") val streamIcon: String? = null,
    val rating: String? = null,
    @SerialName("rating_5based") val rating5Based: Double? = null,
    val added: String? = null,
    @SerialName("category_id") val categoryId: String? = null,
    @SerialName("container_extension") val containerExtension: String? = null,
    @SerialName("custom_sid") val customSid: String? = null,
    @SerialName("direct_source") val directSource: String? = null
)

/**
 * Series item from get_series action
 */
@Serializable
data class XtreamSeries(
    val num: Int? = null,
    val name: String,
    @SerialName("series_id") val seriesId: Int,
    val cover: String? = null,
    val plot: String? = null,
    val cast: String? = null,
    val director: String? = null,
    val genre: String? = null,
    @SerialName("release_date") val releaseDate: String? = null,
    @SerialName("last_modified") val lastModified: String? = null,
    val rating: String? = null,
    @SerialName("rating_5based") val rating5Based: Double? = null,
    @SerialName("backdrop_path") val backdropPath: List<String>? = null,
    @SerialName("youtube_trailer") val youtubeTrailer: String? = null,
    @SerialName("episode_run_time") val episodeRunTime: String? = null,
    @SerialName("category_id") val categoryId: String? = null
)

/**
 * EPG (Electronic Program Guide) listing
 */
@Serializable
data class XtreamEpgListing(
    val id: String? = null,
    @SerialName("epg_id") val epgId: String? = null,
    val title: String? = null,
    val lang: String? = null,
    val start: String? = null,
    val end: String? = null,
    val description: String? = null,
    @SerialName("channel_id") val channelId: String? = null,
    @SerialName("start_timestamp") val startTimestamp: Long? = null,
    @SerialName("stop_timestamp") val stopTimestamp: Long? = null
)

/**
 * Series info response from get_series_info action
 */
@Serializable
data class XtreamSeriesInfo(
    val seasons: List<XtreamSeason> = emptyList(),
    val info: XtreamSeriesDetails? = null,
    val episodes: Map<String, List<XtreamEpisode>> = emptyMap()
)

@Serializable
data class XtreamSeason(
    @SerialName("season_number") val seasonNumber: Int,
    val name: String? = null,
    @SerialName("episode_count") val episodeCount: Int? = null,
    val cover: String? = null,
    @SerialName("cover_big") val coverBig: String? = null
)

@Serializable
data class XtreamSeriesDetails(
    val name: String? = null,
    val cover: String? = null,
    val plot: String? = null,
    val cast: String? = null,
    val director: String? = null,
    val genre: String? = null,
    @SerialName("release_date") val releaseDate: String? = null,
    val rating: String? = null,
    @SerialName("rating_5based") val rating5Based: Double? = null,
    @SerialName("backdrop_path") val backdropPath: List<String>? = null,
    @SerialName("youtube_trailer") val youtubeTrailer: String? = null,
    @SerialName("episode_run_time") val episodeRunTime: String? = null,
    @SerialName("category_id") val categoryId: String? = null
)

@Serializable
data class XtreamEpisode(
    val id: String? = null,
    @SerialName("episode_num") val episodeNum: Int,
    val title: String? = null,
    @SerialName("container_extension") val containerExtension: String? = null,
    val info: XtreamEpisodeInfo? = null,
    @SerialName("custom_sid") val customSid: String? = null,
    val added: String? = null,
    val season: Int? = null,
    @SerialName("direct_source") val directSource: String? = null
)

@Serializable
data class XtreamEpisodeInfo(
    @SerialName("movie_image") val movieImage: String? = null,
    val plot: String? = null,
    @SerialName("release_date") val releaseDate: String? = null,
    val rating: Double? = null,
    val name: String? = null,
    @SerialName("duration_secs") val durationSecs: Int? = null,
    val duration: String? = null,
    val bitrate: Int? = null
)

/**
 * Credentials stored locally
 */
@Serializable
data class XtreamCredentials(
    val serverUrl: String,
    val username: String,
    val password: String,
    val name: String = "Default"
)

/**
 * Helper to build stream URLs
 */
object XtreamUrlBuilder {
    fun buildLiveStreamUrl(
        serverUrl: String,
        username: String,
        password: String,
        streamId: Int,
        extension: String = "ts"
    ): String {
        val baseUrl = serverUrl.trimEnd('/')
        return "$baseUrl/live/$username/$password/$streamId.$extension"
    }

    fun buildVodStreamUrl(
        serverUrl: String,
        username: String,
        password: String,
        streamId: Int,
        extension: String = "mp4"
    ): String {
        val baseUrl = serverUrl.trimEnd('/')
        return "$baseUrl/movie/$username/$password/$streamId.$extension"
    }

    fun buildSeriesStreamUrl(
        serverUrl: String,
        username: String,
        password: String,
        streamId: Int,
        extension: String = "mp4"
    ): String {
        val baseUrl = serverUrl.trimEnd('/')
        return "$baseUrl/series/$username/$password/$streamId.$extension"
    }

    fun buildApiUrl(
        serverUrl: String,
        username: String,
        password: String,
        action: String? = null
    ): String {
        val baseUrl = serverUrl.trimEnd('/')
        val base = "$baseUrl/player_api.php?username=$username&password=$password"
        return if (action != null) "$base&action=$action" else base
    }

    fun buildCatchupStreamUrl(
        serverUrl: String,
        username: String,
        password: String,
        streamId: Int,
        startTimeMs: Long,
        durationMinutes: Int,
        extension: String = "ts"
    ): String {
        val baseUrl = serverUrl.trimEnd('/')
        val safeDuration = durationMinutes.coerceAtLeast(1)
        val formatter = SimpleDateFormat("yyyy-MM-dd:HH-mm", Locale.US)
        val start = formatter.format(Date(startTimeMs))
        return "$baseUrl/timeshift/$username/$password/$safeDuration/$start/$streamId.$extension"
    }
}

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
 * Xtream Codes API Service
 */
package com.google.jetstream.data.network

import com.google.jetstream.data.models.xtream.XtreamAuthResponse
import com.google.jetstream.data.models.xtream.XtreamCategory
import com.google.jetstream.data.models.xtream.XtreamChannel
import com.google.jetstream.data.models.xtream.XtreamEpgListing
import com.google.jetstream.data.models.xtream.XtreamSeries
import com.google.jetstream.data.models.xtream.XtreamSeriesInfo
import com.google.jetstream.data.models.xtream.XtreamVodItem
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Url

/**
 * Retrofit interface for Xtream Codes Player API
 *
 * Base URL format: http://server:port/player_api.php
 */
interface XtreamApiService {

    /**
     * Authenticate and get user/server info
     * URL: /player_api.php?username=X&password=X
     */
    @GET
    suspend fun authenticate(
        @Url url: String
    ): Response<XtreamAuthResponse>

    /**
     * Get live stream categories
     * URL: /player_api.php?username=X&password=X&action=get_live_categories
     */
    @GET
    suspend fun getLiveCategories(
        @Url url: String
    ): Response<List<XtreamCategory>>

    /**
     * Get all live streams
     * URL: /player_api.php?username=X&password=X&action=get_live_streams
     */
    @GET
    suspend fun getLiveStreams(
        @Url url: String
    ): Response<List<XtreamChannel>>

    /**
     * Get live streams by category
     * URL: /player_api.php?username=X&password=X&action=get_live_streams&category_id=X
     */
    @GET
    suspend fun getLiveStreamsByCategory(
        @Url url: String
    ): Response<List<XtreamChannel>>

    /**
     * Get VOD categories
     * URL: /player_api.php?username=X&password=X&action=get_vod_categories
     */
    @GET
    suspend fun getVodCategories(
        @Url url: String
    ): Response<List<XtreamCategory>>

    /**
     * Get all VOD streams
     * URL: /player_api.php?username=X&password=X&action=get_vod_streams
     */
    @GET
    suspend fun getVodStreams(
        @Url url: String
    ): Response<List<XtreamVodItem>>

    /**
     * Get VOD streams by category
     * URL: /player_api.php?username=X&password=X&action=get_vod_streams&category_id=X
     */
    @GET
    suspend fun getVodStreamsByCategory(
        @Url url: String
    ): Response<List<XtreamVodItem>>

    /**
     * Get series categories
     * URL: /player_api.php?username=X&password=X&action=get_series_categories
     */
    @GET
    suspend fun getSeriesCategories(
        @Url url: String
    ): Response<List<XtreamCategory>>

    /**
     * Get all series
     * URL: /player_api.php?username=X&password=X&action=get_series
     */
    @GET
    suspend fun getSeries(
        @Url url: String
    ): Response<List<XtreamSeries>>

    /**
     * Get series by category
     * URL: /player_api.php?username=X&password=X&action=get_series&category_id=X
     */
    @GET
    suspend fun getSeriesByCategory(
        @Url url: String
    ): Response<List<XtreamSeries>>

    /**
     * Get series info with episodes
     * URL: /player_api.php?username=X&password=X&action=get_series_info&series_id=X
     */
    @GET
    suspend fun getSeriesInfo(
        @Url url: String
    ): Response<XtreamSeriesInfo>

    /**
     * Get EPG for a channel
     * URL: /player_api.php?username=X&password=X&action=get_short_epg&stream_id=X
     */
    @GET
    suspend fun getShortEpg(
        @Url url: String
    ): Response<Map<String, List<XtreamEpgListing>>>
}

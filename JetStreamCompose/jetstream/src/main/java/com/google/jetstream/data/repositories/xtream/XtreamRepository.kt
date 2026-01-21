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

package com.google.jetstream.data.repositories.xtream

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.jetstream.data.models.xtream.XtreamAuthResponse
import com.google.jetstream.data.models.xtream.XtreamCategory
import com.google.jetstream.data.models.xtream.XtreamChannel
import com.google.jetstream.data.models.xtream.XtreamCredentials
import com.google.jetstream.data.models.xtream.XtreamSeries
import com.google.jetstream.data.models.xtream.XtreamSeriesInfo
import com.google.jetstream.data.models.xtream.XtreamUrlBuilder
import com.google.jetstream.data.models.xtream.XtreamVodInfoResponse
import com.google.jetstream.data.models.xtream.XtreamVodItem
import com.google.jetstream.data.network.XtreamApiService
import com.google.jetstream.data.repositories.M3uRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = "xtream_credentials"
)

sealed class XtreamResult<out T> {
    data class Success<T>(val data: T) : XtreamResult<T>()
    data class Error(val message: String, val code: Int? = null) : XtreamResult<Nothing>()
    data object Loading : XtreamResult<Nothing>()
}

@Singleton
class XtreamRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val apiService: XtreamApiService,
    private val m3uRepository: M3uRepository,
    private val json: Json
) {
    companion object {
        private val KEY_CREDENTIALS = stringPreferencesKey("credentials")
        private val KEY_M3U_URL = stringPreferencesKey("m3u_url")
    }

    /**
     * Check if user is logged in (either Xtream or M3U)
     */
    val isLoggedIn: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_CREDENTIALS] != null || preferences[KEY_M3U_URL] != null
    }

    val credentialsFlow: Flow<XtreamCredentials?> = context.dataStore.data.map { preferences ->
        val stored = preferences[KEY_CREDENTIALS] ?: return@map null
        runCatching { json.decodeFromString<XtreamCredentials>(stored) }.getOrNull()
    }

    /**
     * Authenticate with Xtream Codes and store credentials
     */
    suspend fun authenticate(
        serverUrl: String,
        username: String,
        password: String
    ): XtreamResult<XtreamAuthResponse> {
        return try {
            val url = XtreamUrlBuilder.buildApiUrl(serverUrl, username, password)
            val response = apiService.authenticate(url)
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null && body.userInfo.auth == 1) {
                    saveCredentials(XtreamCredentials(serverUrl, username, password))
                    XtreamResult.Success(body)
                } else {
                    XtreamResult.Error(body?.userInfo?.message ?: "Authentication failed")
                }
            } else {
                XtreamResult.Error("Authentication failed", response.code())
            }
        } catch (e: Exception) {
            XtreamResult.Error("Error: ${e.message}")
        }
    }

    /**
     * Set M3U URL and load it
     */
    suspend fun setM3uUrl(url: String): XtreamResult<Boolean> {
        return try {
            val items = m3uRepository.loadPlaylist(url)
            if (items.isNotEmpty()) {
                context.dataStore.edit { preferences ->
                    preferences[KEY_M3U_URL] = url
                    preferences.remove(KEY_CREDENTIALS) // Clear Xtream if switching to M3U
                }
                XtreamResult.Success(true)
            } else {
                XtreamResult.Error("M3U playlist is empty or invalid")
            }
        } catch (e: Exception) {
            XtreamResult.Error("Failed to load M3U: ${e.message}")
        }
    }

    private suspend fun isM3uMode(): Boolean {
        val preferences = context.dataStore.data.first()
        return preferences[KEY_M3U_URL] != null
    }

    private suspend fun getCredentials(): XtreamCredentials? {
        return credentialsFlow.first()
    }

    private suspend fun saveCredentials(credentials: XtreamCredentials) {
        context.dataStore.edit { preferences ->
            preferences[KEY_CREDENTIALS] = json.encodeToString(credentials)
            preferences.remove(KEY_M3U_URL)
        }
    }

    suspend fun logout() {
        context.dataStore.edit { preferences ->
            preferences.remove(KEY_CREDENTIALS)
            preferences.remove(KEY_M3U_URL)
        }
    }

    /**
     * Get live stream categories
     */
    suspend fun getLiveCategories(): XtreamResult<List<XtreamCategory>> {
        if (isM3uMode()) {
            return XtreamResult.Success(m3uRepository.getCategories())
        }
        val creds = getCredentials() ?: return XtreamResult.Error("Not logged in")
        // ... (rest of original code)

        return try {
            val url = XtreamUrlBuilder.buildApiUrl(
                creds.serverUrl, creds.username, creds.password, "get_live_categories"
            )
            val response = apiService.getLiveCategories(url)
            if (response.isSuccessful) {
                XtreamResult.Success(response.body() ?: emptyList())
            } else {
                XtreamResult.Error("Failed to fetch categories", response.code())
            }
        } catch (e: Exception) {
            XtreamResult.Error("Error: ${e.message}")
        }
    }

    /**
     * Get all live streams
     */
    suspend fun getLiveStreams(): XtreamResult<List<XtreamChannel>> {
        if (isM3uMode()) {
            return XtreamResult.Success(m3uRepository.getAllChannels())
        }
        val creds = getCredentials() ?: return XtreamResult.Error("Not logged in")
        return try {
            val url = XtreamUrlBuilder.buildApiUrl(
                creds.serverUrl, creds.username, creds.password, "get_live_streams"
            )
            val response = apiService.getLiveStreams(url)
            if (response.isSuccessful) {
                XtreamResult.Success(response.body() ?: emptyList())
            } else {
                XtreamResult.Error("Failed to fetch channels", response.code())
            }
        } catch (e: Exception) {
            XtreamResult.Error("Error: ${e.message}")
        }
    }

    /**
     * Get live streams by category
     */
    suspend fun getLiveStreamsByCategory(categoryId: String): XtreamResult<List<XtreamChannel>> {
        if (isM3uMode()) {
            return XtreamResult.Success(m3uRepository.getChannelsByCategory(categoryId))
        }
        val creds = getCredentials() ?: return XtreamResult.Error("Not logged in")
        return try {
            val url = XtreamUrlBuilder.buildApiUrl(
                creds.serverUrl, creds.username, creds.password, "get_live_streams"
            ) + "&category_id=$categoryId"
            val response = apiService.getLiveStreamsByCategory(url)
            if (response.isSuccessful) {
                XtreamResult.Success(response.body() ?: emptyList())
            } else {
                XtreamResult.Error("Failed to fetch channels", response.code())
            }
        } catch (e: Exception) {
            XtreamResult.Error("Error: ${e.message}")
        }
    }

    /**
     * Get VOD categories
     */
    suspend fun getVodCategories(): XtreamResult<List<XtreamCategory>> {
        val creds = getCredentials() ?: return XtreamResult.Error("Not logged in")
        return try {
            val url = XtreamUrlBuilder.buildApiUrl(
                creds.serverUrl, creds.username, creds.password, "get_vod_categories"
            )
            val response = apiService.getVodCategories(url)
            if (response.isSuccessful) {
                XtreamResult.Success(response.body() ?: emptyList())
            } else {
                XtreamResult.Error("Failed to fetch VOD categories", response.code())
            }
        } catch (e: Exception) {
            XtreamResult.Error("Error: ${e.message}")
        }
    }

    /**
     * Get all VOD streams
     */
    suspend fun getVodStreams(): XtreamResult<List<XtreamVodItem>> {
        val creds = getCredentials() ?: return XtreamResult.Error("Not logged in")
        return try {
            val url = XtreamUrlBuilder.buildApiUrl(
                creds.serverUrl, creds.username, creds.password, "get_vod_streams"
            )
            val response = apiService.getVodStreams(url)
            if (response.isSuccessful) {
                XtreamResult.Success(response.body() ?: emptyList())
            } else {
                XtreamResult.Error("Failed to fetch VOD streams", response.code())
            }
        } catch (e: Exception) {
            XtreamResult.Error("Error: ${e.message}")
        }
    }

    /**
     * Get VOD streams by category
     */
    suspend fun getVodStreamsByCategory(categoryId: String): XtreamResult<List<XtreamVodItem>> {
        val creds = getCredentials() ?: return XtreamResult.Error("Not logged in")
        return try {
            val url = XtreamUrlBuilder.buildApiUrl(
                creds.serverUrl, creds.username, creds.password, "get_vod_streams"
            ) + "&category_id=$categoryId"
            val response = apiService.getVodStreamsByCategory(url)
            if (response.isSuccessful) {
                XtreamResult.Success(response.body() ?: emptyList())
            } else {
                XtreamResult.Error("Failed to fetch VOD streams", response.code())
            }
        } catch (e: Exception) {
            XtreamResult.Error("Error: ${e.message}")
        }
    }

    /**
     * Get VOD info with metadata
     */
    suspend fun getVodInfo(vodId: Int): XtreamResult<XtreamVodInfoResponse> {
        val creds = getCredentials() ?: return XtreamResult.Error("Not logged in")
        return try {
            val url = XtreamUrlBuilder.buildApiUrl(
                creds.serverUrl, creds.username, creds.password, "get_vod_info"
            ) + "&vod_id=$vodId"
            val response = apiService.getVodInfo(url)
            if (response.isSuccessful) {
                XtreamResult.Success(response.body() ?: XtreamVodInfoResponse())
            } else {
                XtreamResult.Error("Failed to fetch VOD info", response.code())
            }
        } catch (e: Exception) {
            XtreamResult.Error("Error: ${e.message}")
        }
    }

    /**
     * Get series categories
     */
    suspend fun getSeriesCategories(): XtreamResult<List<XtreamCategory>> {
        val creds = getCredentials() ?: return XtreamResult.Error("Not logged in")
        return try {
            val url = XtreamUrlBuilder.buildApiUrl(
                creds.serverUrl, creds.username, creds.password, "get_series_categories"
            )
            val response = apiService.getSeriesCategories(url)
            if (response.isSuccessful) {
                XtreamResult.Success(response.body() ?: emptyList())
            } else {
                XtreamResult.Error("Failed to fetch series categories", response.code())
            }
        } catch (e: Exception) {
            XtreamResult.Error("Error: ${e.message}")
        }
    }

    /**
     * Get all series
     */
    suspend fun getSeries(): XtreamResult<List<XtreamSeries>> {
        val creds = getCredentials() ?: return XtreamResult.Error("Not logged in")
        return try {
            val url = XtreamUrlBuilder.buildApiUrl(
                creds.serverUrl, creds.username, creds.password, "get_series"
            )
            val response = apiService.getSeries(url)
            if (response.isSuccessful) {
                XtreamResult.Success(response.body() ?: emptyList())
            } else {
                XtreamResult.Error("Failed to fetch series", response.code())
            }
        } catch (e: Exception) {
            XtreamResult.Error("Error: ${e.message}")
        }
    }

    /**
     * Get series by category
     */
    suspend fun getSeriesByCategory(categoryId: String): XtreamResult<List<XtreamSeries>> {
        val creds = getCredentials() ?: return XtreamResult.Error("Not logged in")
        return try {
            val url = XtreamUrlBuilder.buildApiUrl(
                creds.serverUrl, creds.username, creds.password, "get_series"
            ) + "&category_id=$categoryId"
            val response = apiService.getSeriesByCategory(url)
            if (response.isSuccessful) {
                XtreamResult.Success(response.body() ?: emptyList())
            } else {
                XtreamResult.Error("Failed to fetch series", response.code())
            }
        } catch (e: Exception) {
            XtreamResult.Error("Error: ${e.message}")
        }
    }

    /**
     * Get series info with episodes
     */
    suspend fun getSeriesInfo(seriesId: Int): XtreamResult<XtreamSeriesInfo> {
        val creds = getCredentials() ?: return XtreamResult.Error("Not logged in")
        return try {
            val url = XtreamUrlBuilder.buildApiUrl(
                creds.serverUrl, creds.username, creds.password, "get_series_info"
            ) + "&series_id=$seriesId"
            val response = apiService.getSeriesInfo(url)
            if (response.isSuccessful) {
                XtreamResult.Success(response.body() ?: XtreamSeriesInfo())
            } else {
                XtreamResult.Error("Failed to fetch series info", response.code())
            }
        } catch (e: Exception) {
            XtreamResult.Error("Error: ${e.message}")
        }
    }

    /**
     * Build episode stream URL
     */
    suspend fun buildEpisodeStreamUrl(episodeId: String, extension: String = "mp4"): String? {
        val creds = getCredentials() ?: return null
        return XtreamUrlBuilder.buildSeriesStreamUrl(
            creds.serverUrl, creds.username, creds.password,
            episodeId.toIntOrNull() ?: 0, extension
        )
    }

    /**
     * Build a playable stream URL for a live channel
     */
    suspend fun buildLiveStreamUrl(streamId: Int): String? {
        if (isM3uMode()) {
            return m3uRepository.getAllChannels().find { it.streamId == streamId }?.directSource
        }
        val creds = getCredentials() ?: return null
        return XtreamUrlBuilder.buildLiveStreamUrl(
            creds.serverUrl, creds.username, creds.password, streamId
        )
    }

    /**
     * Build a playable stream URL for VOD
     */
    suspend fun buildVodStreamUrl(streamId: Int, extension: String = "mp4"): String? {
        if (isM3uMode()) {
            return m3uRepository.getAllChannels().find { it.streamId == streamId }?.directSource
        }
        val creds = getCredentials() ?: return null
        return XtreamUrlBuilder.buildVodStreamUrl(
            creds.serverUrl, creds.username, creds.password, streamId, extension
        )
    }

    suspend fun buildCatchupStreamUrl(
        streamId: Int,
        startTimeMs: Long,
        endTimeMs: Long,
        extension: String = "ts"
    ): String? {
        val creds = getCredentials() ?: return null
        val durationMinutes = ((endTimeMs - startTimeMs) / 60000).toInt().coerceAtLeast(1)
        return XtreamUrlBuilder.buildCatchupStreamUrl(
            creds.serverUrl,
            creds.username,
            creds.password,
            streamId,
            startTimeMs,
            durationMinutes,
            extension
        )
    }
}

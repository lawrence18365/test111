/*
 * Xtream Codes Repository
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
import com.google.jetstream.data.models.xtream.XtreamVodItem
import com.google.jetstream.data.network.XtreamApiService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "xtream_credentials")

sealed class XtreamResult<out T> {
    data class Success<T>(val data: T) : XtreamResult<T>()
    data class Error(val message: String, val code: Int? = null) : XtreamResult<Nothing>()
    data object Loading : XtreamResult<Nothing>()
}

@Singleton
class XtreamRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val apiService: XtreamApiService,
    private val json: Json
) {
    companion object {
        private val KEY_CREDENTIALS = stringPreferencesKey("credentials")
        private val KEY_CURRENT_SERVER = stringPreferencesKey("current_server")
    }

    // Cached credentials for quick access
    private var cachedCredentials: XtreamCredentials? = null

    /**
     * Observe stored credentials
     */
    val credentialsFlow: Flow<XtreamCredentials?> = context.dataStore.data.map { preferences ->
        preferences[KEY_CREDENTIALS]?.let { credentialsJson ->
            try {
                json.decodeFromString<XtreamCredentials>(credentialsJson)
            } catch (e: Exception) {
                null
            }
        }
    }

    /**
     * Check if user is logged in
     */
    val isLoggedIn: Flow<Boolean> = credentialsFlow.map { it != null }

    /**
     * Authenticate with Xtream Codes server
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
                val authResponse = response.body()
                if (authResponse != null && authResponse.userInfo.auth == 1) {
                    // Save credentials on successful auth
                    val credentials = XtreamCredentials(
                        serverUrl = serverUrl,
                        username = username,
                        password = password
                    )
                    saveCredentials(credentials)
                    XtreamResult.Success(authResponse)
                } else {
                    XtreamResult.Error("Authentication failed: Invalid credentials")
                }
            } else {
                XtreamResult.Error("Server error: ${response.code()}", response.code())
            }
        } catch (e: Exception) {
            XtreamResult.Error("Connection failed: ${e.message}")
        }
    }

    /**
     * Save credentials to DataStore
     */
    private suspend fun saveCredentials(credentials: XtreamCredentials) {
        cachedCredentials = credentials
        context.dataStore.edit { preferences ->
            preferences[KEY_CREDENTIALS] = json.encodeToString(credentials)
        }
    }

    /**
     * Get current credentials
     */
    suspend fun getCredentials(): XtreamCredentials? {
        if (cachedCredentials != null) return cachedCredentials
        cachedCredentials = credentialsFlow.first()
        return cachedCredentials
    }

    /**
     * Clear credentials (logout)
     */
    suspend fun logout() {
        cachedCredentials = null
        context.dataStore.edit { preferences ->
            preferences.remove(KEY_CREDENTIALS)
        }
    }

    /**
     * Get live stream categories
     */
    suspend fun getLiveCategories(): XtreamResult<List<XtreamCategory>> {
        val creds = getCredentials() ?: return XtreamResult.Error("Not logged in")
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
        val creds = getCredentials() ?: return null
        return XtreamUrlBuilder.buildLiveStreamUrl(
            creds.serverUrl, creds.username, creds.password, streamId
        )
    }

    /**
     * Build a playable stream URL for VOD
     */
    suspend fun buildVodStreamUrl(streamId: Int, extension: String = "mp4"): String? {
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

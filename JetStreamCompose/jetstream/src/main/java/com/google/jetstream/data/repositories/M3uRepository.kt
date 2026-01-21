package com.google.jetstream.data.repositories

import com.google.jetstream.data.models.xtream.XtreamCategory
import com.google.jetstream.data.models.xtream.XtreamChannel
import com.google.jetstream.data.util.M3uItem
import com.google.jetstream.data.util.M3uParser
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Singleton
class M3uRepository @Inject constructor(
    private val okHttpClient: OkHttpClient
) {
    private var cachedItems: List<M3uItem> = emptyList()

    suspend fun loadPlaylist(url: String): List<M3uItem> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url(url).build()
            val response = okHttpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val content = response.body?.string() ?: ""
                cachedItems = M3uParser.parse(content)
                return@withContext cachedItems
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return@withContext emptyList()
    }

    fun getCategories(): List<XtreamCategory> {
        return cachedItems
            .mapNotNull { it.group }
            .distinct()
            .mapIndexed { index, name ->
                XtreamCategory(
                    categoryId = name, // Use name as ID for easy filtering
                    categoryName = name
                )
            }
    }

    fun getAllChannels(): List<XtreamChannel> {
        return cachedItems.mapIndexed { index, item ->
            XtreamChannel(
                num = index + 1,
                name = item.name,
                streamId = item.url.hashCode(),
                streamIcon = item.logo,
                epgChannelId = item.epgId,
                categoryId = item.group,
                directSource = item.url
            )
        }
    }

    fun getChannelsByCategory(categoryName: String): List<XtreamChannel> {
        return cachedItems
            .filter { it.group == categoryName }
            .mapIndexed { index, item ->
                XtreamChannel(
                    num = index + 1,
                    name = item.name,
                    streamId = item.url.hashCode(), // Hacky but unique-ish
                    streamIcon = item.logo,
                    epgChannelId = item.epgId,
                    categoryId = categoryName,
                    directSource = item.url
                )
            }
    }
}

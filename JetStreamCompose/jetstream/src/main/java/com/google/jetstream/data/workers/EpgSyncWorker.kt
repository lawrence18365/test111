package com.google.jetstream.data.workers

import android.content.Context
import android.util.Log
import android.util.Xml
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.jetstream.data.entities.EpgChannelEntity
import com.google.jetstream.data.entities.EpgProgramEntity
import com.google.jetstream.data.local.EpgDao
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.io.InputStream
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import okhttp3.OkHttpClient
import okhttp3.Request
import org.xmlpull.v1.XmlPullParser

@HiltWorker
class EpgSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val epgDao: EpgDao,
    private val okHttpClient: OkHttpClient
) : CoroutineWorker(appContext, workerParams) {

    private val epgUrl = 
        "https://raw.githubusercontent.com/iptv-org/epg/master.xml" // Placeholder URL
    private val forbiddenKeywords = listOf("Adult", "XXX", "Porn")
    private val dateFormat = SimpleDateFormat("yyyyMMddHHmmss Z", Locale.getDefault())

    override suspend fun doWork(): Result {
        return try {
            Log.d(TAG, "Starting EPG Sync from $epgUrl")
            
            // Download XML
            val request = Request.Builder().url(epgUrl).build()
            val response = okHttpClient.newCall(request).execute()
            
            if (!response.isSuccessful || response.body == null) {
                Log.e(TAG, "Failed to download EPG: ${response.code}")
                return Result.retry()
            }

            response.body!!.byteStream().use { inputStream ->
                parseAndSaveEpg(inputStream)
            }

            Log.d(TAG, "EPG Sync Completed Successfully")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing EPG", e)
            Result.failure()
        }
    }

    private suspend fun parseAndSaveEpg(inputStream: InputStream) {
        val parser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        parser.setInput(inputStream, null)

        val channels = mutableListOf<EpgChannelEntity>()
        val programs = mutableListOf<EpgProgramEntity>()

        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            val name = parser.name
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    when (name) {
                        "channel" -> {
                            readChannel(parser)?.let { channels.add(it) }
                        }
                        "programme" -> {
                            readProgram(parser)?.let { programs.add(it) }
                        }
                    }
                }
            }
            
            // Batch insert to avoid OOM
            if (channels.size >= 100) {
                epgDao.insertChannels(channels)
                channels.clear()
            }
            if (programs.size >= 100) {
                epgDao.insertPrograms(programs)
                programs.clear()
            }
            
            eventType = parser.next()
        }

        // Insert remaining
        if (channels.isNotEmpty()) epgDao.insertChannels(channels)
        if (programs.isNotEmpty()) epgDao.insertPrograms(programs)
        
        // Clean up old programs
        val yesterday = System.currentTimeMillis() - 24 * 60 * 60 * 1000
        epgDao.deleteOldPrograms(yesterday)
    }

    private fun readChannel(parser: XmlPullParser): EpgChannelEntity? {
        val id = parser.getAttributeValue(null, "id") ?: return null
        var displayName: String? = null
        var icon: String? = null

        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.eventType != XmlPullParser.START_TAG) continue
            when (parser.name) {
                "display-name" -> displayName = readText(parser)
                "icon" -> {
                    icon = parser.getAttributeValue(null, "src")
                    parser.nextTag() // Skip closing tag
                }
                else -> skip(parser)
            }
        }
        
        // Filter Channels (Optional, but safer)
        if (isUnsafe(displayName)) return null

        return EpgChannelEntity(id, displayName, icon)
    }

    private fun readProgram(parser: XmlPullParser): EpgProgramEntity? {
        val channelId = parser.getAttributeValue(null, "channel") ?: return null
        val startStr = parser.getAttributeValue(null, "start")
        val stopStr = parser.getAttributeValue(null, "stop")
        
        var title = ""
        var description: String? = null
        var category: String? = null

        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.eventType != XmlPullParser.START_TAG) continue
            when (parser.name) {
                "title" -> title = readText(parser)
                "desc" -> description = readText(parser)
                "category" -> category = readText(parser)
                else -> skip(parser)
            }
        }

        // Safety Filter
        if (isUnsafe(title) || isUnsafe(description) || isUnsafe(category)) {
            return null
        }

        val startTime = parseDate(startStr)
        val endTime = parseDate(stopStr)

        if (startTime == 0L || endTime == 0L) return null

        return EpgProgramEntity(
            channelId = channelId,
            title = title,
            description = description,
            startTime = startTime,
            endTime = endTime,
            category = category
        )
    }

    private fun readText(parser: XmlPullParser): String {
        var result = ""
        if (parser.next() == XmlPullParser.TEXT) {
            result = parser.text
            parser.nextTag()
        }
        return result
    }

    private fun skip(parser: XmlPullParser) {
        if (parser.eventType != XmlPullParser.START_TAG) {
            throw IllegalStateException()
        }
        var depth = 1
        while (depth != 0) {
            when (parser.next()) {
                XmlPullParser.END_TAG -> depth--
                XmlPullParser.START_TAG -> depth++
            }
        }
    }

    private fun isUnsafe(text: String?): Boolean {
        if (text == null) return false
        return forbiddenKeywords.any { text.contains(it, ignoreCase = true) }
    }

    private fun parseDate(dateStr: String?): Long {
        if (dateStr == null) return 0L
        return try {
            // Fix format if it's "20230101000000 +0000" to match SimpleDateFormat
            // XMLTV format is flexible.
            // Simple approach:
            val cleanDate = if (dateStr.length > 18) dateStr else "$dateStr +0000"
             // Often XMLTV is "20080715003000 -0600"
            dateFormat.parse(cleanDate)?.time ?: 0L
        } catch (e: Exception) {
            0L
        }
    }

    companion object {
        private const val TAG = "EpgSyncWorker"
    }
}

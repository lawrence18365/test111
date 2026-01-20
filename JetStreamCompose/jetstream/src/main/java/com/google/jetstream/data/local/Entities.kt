/*
 * Room Database Entities for IPTV app
 */
package com.google.jetstream.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter

/**
 * Favorite channel - saved by user for quick access
 */
@Entity(tableName = "favorite_channels")
data class FavoriteChannel(
    @PrimaryKey
    val streamId: Int,
    val name: String,
    val streamIcon: String?,
    val categoryId: String?,
    val categoryName: String?,
    val streamType: String, // "live", "vod", "series"
    val addedAt: Long = System.currentTimeMillis(),
    val sortOrder: Int = 0
)

/**
 * Watch history - tracks recently watched content
 */
@Entity(tableName = "watch_history")
data class WatchHistory(
    @PrimaryKey
    val streamId: Int,
    val name: String,
    val streamIcon: String?,
    val streamType: String, // "live", "vod", "series"
    val lastWatchedAt: Long = System.currentTimeMillis(),
    val watchDurationSeconds: Long = 0,
    val progressPercent: Float = 0f, // For VOD/Series resume
    val lastPositionMs: Long = 0 // For VOD/Series resume playback position
)

/**
 * Hidden channel - channels user wants to hide from view
 */
@Entity(tableName = "hidden_channels")
data class HiddenChannel(
    @PrimaryKey
    val streamId: Int,
    val streamType: String,
    val hiddenAt: Long = System.currentTimeMillis()
)

/**
 * Custom channel group created by user
 */
@Entity(tableName = "channel_groups")
data class ChannelGroup(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val iconEmoji: String? = null, // Like "⚽" for sports
    val sortOrder: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Channel to group mapping (many-to-many)
 */
@Entity(
    tableName = "channel_group_mappings",
    primaryKeys = ["streamId", "groupId"]
)
data class ChannelGroupMapping(
    val streamId: Int,
    val groupId: Int
)

/**
 * Type converters for Room
 */
class Converters {
    @TypeConverter
    fun fromStringList(value: String?): List<String>? {
        return value?.split(",")?.filter { it.isNotBlank() }
    }

    @TypeConverter
    fun toStringList(list: List<String>?): String? {
        return list?.joinToString(",")
    }
}

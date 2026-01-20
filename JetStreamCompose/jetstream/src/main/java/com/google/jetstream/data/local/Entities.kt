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

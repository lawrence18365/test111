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

package com.google.jetstream.data.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "epg_channels")
data class EpgChannelEntity(
    @PrimaryKey
    val channelId: String, // Matches the 'id' attribute in XML <channel id="...">
    val displayName: String?,
    val iconUrl: String?
)

@Entity(
    tableName = "epg_programs",
    foreignKeys = [
        ForeignKey(
            entity = EpgChannelEntity::class,
            parentColumns = ["channelId"],
            childColumns = ["channelId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["channelId"]),
        Index(value = ["startTime"]),
        Index(value = ["endTime"])
    ]
)
data class EpgProgramEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val channelId: String, // FK to EpgChannelEntity
    val title: String,
    val description: String?,
    val startTime: Long,
    val endTime: Long,
    val category: String?
)

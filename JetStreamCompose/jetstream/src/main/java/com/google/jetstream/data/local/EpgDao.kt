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

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.google.jetstream.data.entities.EpgChannelEntity
import com.google.jetstream.data.entities.EpgProgramEntity

@Dao
interface EpgDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChannels(channels: List<EpgChannelEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPrograms(programs: List<EpgProgramEntity>)

    @Transaction
    @Query("""
        SELECT * FROM epg_programs 
        WHERE channelId = :channelId 
        AND endTime > :startTime 
        AND startTime < :endTime
        ORDER BY startTime ASC
    """)
    suspend fun getProgramsForChannel(
        channelId: String,
        startTime: Long,
        endTime: Long
    ): List<EpgProgramEntity>

    @Transaction
    @Query("""
        SELECT * FROM epg_programs 
        WHERE channelId IN (:channelIds) 
        AND endTime > :startTime 
        AND startTime < :endTime
        ORDER BY startTime ASC
    """)
    suspend fun getProgramsForChannels(
        channelIds: List<String>,
        startTime: Long,
        endTime: Long
    ): List<EpgProgramEntity>

    @Query("DELETE FROM epg_channels")
    suspend fun deleteAllChannels()

    @Query("DELETE FROM epg_programs")
    suspend fun deleteAllPrograms()
    
    @Query("DELETE FROM epg_programs WHERE endTime < :timestamp")
    suspend fun deleteOldPrograms(timestamp: Long)
}
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

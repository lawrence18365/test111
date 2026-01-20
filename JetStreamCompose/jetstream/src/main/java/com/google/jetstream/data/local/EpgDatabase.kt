package com.google.jetstream.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.google.jetstream.data.entities.EpgChannelEntity
import com.google.jetstream.data.entities.EpgProgramEntity

@Database(
    entities = [EpgChannelEntity::class, EpgProgramEntity::class],
    version = 1,
    exportSchema = false
)
abstract class EpgDatabase : RoomDatabase() {
    abstract fun epgDao(): EpgDao
}

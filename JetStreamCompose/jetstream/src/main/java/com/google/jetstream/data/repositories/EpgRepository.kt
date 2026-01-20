package com.google.jetstream.data.repositories

import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.google.jetstream.data.entities.EpgProgramEntity
import com.google.jetstream.data.local.EpgDao
import com.google.jetstream.data.workers.EpgSyncWorker
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EpgRepository @Inject constructor(
    private val epgDao: EpgDao,
    private val workManager: WorkManager
) {
    suspend fun getProgramsForChannel(
        channelId: String,
        startTime: Long,
        endTime: Long
    ): List<EpgProgramEntity> {
        return epgDao.getProgramsForChannel(channelId, startTime, endTime)
    }

    suspend fun getProgramsForChannels(
        channelIds: List<String>,
        startTime: Long,
        endTime: Long
    ): List<EpgProgramEntity> {
        return epgDao.getProgramsForChannels(channelIds, startTime, endTime)
    }

    fun syncEpg() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncRequest = OneTimeWorkRequestBuilder<EpgSyncWorker>()
            .setConstraints(constraints)
            .build()

        workManager.enqueueUniqueWork(
            "EpgSyncWork",
            ExistingWorkPolicy.KEEP,
            syncRequest
        )
    }
}

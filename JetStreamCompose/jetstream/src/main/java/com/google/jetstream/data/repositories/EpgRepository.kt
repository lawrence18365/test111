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

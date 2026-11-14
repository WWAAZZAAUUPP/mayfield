package com.example.telemetry.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.telemetry.BuildConfig
import com.example.telemetry.data.TelemetryDatabase

class TelemetryUploadWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val dao = TelemetryDatabase.get(applicationContext).telemetryDao()
        val batch = dao.getUnsynced(100)
        if (batch.isEmpty()) return Result.success()

        val uploader = TelemetryUploader(BuildConfig.TELEMETRY_ENDPOINT)
        return when (uploader.upload(batch)) {
            is UploadOutcome.Success -> {
                dao.markSynced(batch.map { it.id })
                Result.success()
            }
            is UploadOutcome.Retryable -> {
                Result.retry()
            }
            is UploadOutcome.Permanent -> {
                dao.markSynced(batch.map { it.id })
                Result.failure()
            }
        }
    }
}

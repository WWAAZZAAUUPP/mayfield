package com.example.telemetry.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.telemetry.BuildConfig
import com.example.telemetry.data.TelemetryDatabase

class TelemetryUploadWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val dao = TelemetryDatabase.get(applicationContext).telemetryDao()
        val batch = dao.getUnsynced(50)
        if (batch.isEmpty()) return Result.success()

        val uploader = TelemetryUploader(BuildConfig.TELEMETRY_ENDPOINT)
        return when (val outcome = uploader.upload(batch)) {
            is UploadOutcome.Success -> {
                dao.markSynced(batch.map { it.id })
                SyncManager.updateStatus("Success: WorkManager Fallback Sync Completed")
                Result.success()
            }
            is UploadOutcome.Retryable -> {
                SyncManager.updateStatus("WorkManager Fallback Retryable")
                Result.retry()
            }
            is UploadOutcome.Permanent -> {
                dao.markSynced(batch.map { it.id })
                SyncManager.updateStatus("WorkManager Fallback Permanent Failure")
                Result.failure()
            }
            is UploadOutcome.Error -> {
                SyncManager.updateStatus("WorkManager Fallback Failed: ${outcome.message}")
                Result.retry()
            }
        }
    }
}

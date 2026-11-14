package com.example.telemetry.sync

import android.content.Context
import androidx.work.*
import com.example.telemetry.BuildConfig
import com.example.telemetry.data.TelemetryDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object SyncManager {
    var lastSyncError: String = "Idle / No attempts yet"
        private set

    fun updateStatus(status: String) {
        lastSyncError = status
    }

    fun triggerRedundantSync(context: Context) {
        val appContext = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val dao = TelemetryDatabase.get(appContext).telemetryDao()
                val batch = dao.getUnsynced(100)
                if (batch.isEmpty()) {
                    updateStatus("Queue empty.")
                    return@launch
                }

                val uploader = TelemetryUploader(BuildConfig.TELEMETRY_ENDPOINT)
                when (uploader.upload(batch)) {
                    is UploadOutcome.Success -> {
                        dao.markSynced(batch.map { it.id })
                        updateStatus("Success: Synced ${batch.size} records")
                    }
                    is UploadOutcome.Retryable -> {
                        updateStatus("Sync Retryable -> Retrying")
                        enqueueWorkManagerFallback(appContext)
                    }
                    is UploadOutcome.Permanent -> {
                        dao.markSynced(batch.map { it.id })
                        updateStatus("Sync Permanent Failure -> Cleared batch")
                    }
                }
            } catch (e: Exception) {
                updateStatus("Exception: ${e.localizedMessage}")
                enqueueWorkManagerFallback(appContext)
            }
        }
    }

    private fun enqueueWorkManagerFallback(context: Context) {
        val request = OneTimeWorkRequestBuilder<TelemetryUploadWorker>()
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            "telemetry_fallback",
            ExistingWorkPolicy.REPLACE,
            request
        )
    }
}

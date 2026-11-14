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

    fun triggerRedundantSync(context: Context) {
        val appContext = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val dao = TelemetryDatabase.get(appContext).telemetryDao()
                val batch = dao.getUnsynced(50)
                if (batch.isEmpty()) {
                    lastSyncError = "Queue empty. Waiting for triggers."
                    return@launch
                }

                val uploader = TelemetryUploader(BuildConfig.TELEMETRY_ENDPOINT)
                when (val outcome = uploader.upload(batch)) {
                    is UploadOutcome.Success -> {
                        dao.markSynced(batch.map { it.id })
                        lastSyncError = "Success: Direct Sync Completed (${batch.size} records)"
                    }
                    is UploadOutcome.Error -> {
                        lastSyncError = "Direct Failed: ${outcome.message} -> Falling back to WorkManager"
                        // Fallback Redundancy Channel
                        enqueueWorkManagerFallback(appContext)
                    }
                }
            } catch (e: Exception) {
                lastSyncError = "Exception: ${e.localizedMessage} -> Falling back to WorkManager"
                enqueueWorkManagerFallback(appContext)
            }
        }
    }

    private fun enqueueWorkManagerFallback(context: Context) {
        val request = OneTimeWorkRequestBuilder<TelemetryUploadWorker>()
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            "telemetry_redundant_fallback",
            ExistingWorkPolicy.REPLACE,
            request
        )
    }
}

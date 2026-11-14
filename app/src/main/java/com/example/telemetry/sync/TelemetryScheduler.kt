package com.example.telemetry.sync

import android.content.Context
import androidx.work.*
import java.util.concurrent.TimeUnit

object TelemetryScheduler {

    private const val UNIQUE_NAME = "telemetry_upload"

    fun schedulePeriodicUpload(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()

        val request = PeriodicWorkRequestBuilder<TelemetryUploadWorker>(6, TimeUnit.HOURS)
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            UNIQUE_NAME, ExistingPeriodicWorkPolicy.KEEP, request
        )
    }

    // ADD THIS NEW FUNCTION:
    fun forceSyncNow(context: Context) {
        val request = OneTimeWorkRequestBuilder<TelemetryUploadWorker>()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        WorkManager.getInstance(context).enqueue(request)
    }
}
package com.example.telemetry.sync
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.*

class SystemEventReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Triggers aggressively when phone is plugged in or boots up
        val request = OneTimeWorkRequestBuilder<TelemetryUploadWorker>()
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            "hardware_trigger_${intent.action}", 
            ExistingWorkPolicy.REPLACE, 
            request
        )
    }
}

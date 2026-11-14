package com.example.telemetry

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.work.*
import com.example.telemetry.sync.AutonomousLoggerWorker
import com.example.telemetry.sync.TelemetryScheduler
import java.util.concurrent.TimeUnit

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(64, 64, 64, 64)
        }
        val statusText = TextView(this).apply {
            text = "Initializing Autonomous Telemetry..."
            textSize = 18f
        }
        layout.addView(statusText)
        setContentView(layout)

        // 1. Request GPS Permissions
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION), 100)
        } else {
            startAutonomousEngine()
            statusText.text = "Autonomous Engine Active.\nYou can close this app safely."
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startAutonomousEngine()
        }
    }

    private fun startAutonomousEngine() {
        // Keeps the network uploader running
        TelemetryScheduler.schedulePeriodicUpload(this)

        // NEW: Schedules the GPS logger to run entirely in the background every 15 minutes
        val loggerRequest = PeriodicWorkRequestBuilder<AutonomousLoggerWorker>(15, TimeUnit.MINUTES).build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "autonomous_gps_logger",
            ExistingPeriodicWorkPolicy.KEEP,
            loggerRequest
        )
    }
}

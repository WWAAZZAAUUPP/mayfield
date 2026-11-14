package com.example.telemetry

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.LinearLayout
import android.widget.TextView
import com.example.telemetry.sync.InstantTelemetryService
import com.example.telemetry.sync.SyncManager

class MainActivity : Activity() {
    private lateinit var statusText: TextView
    private val handler = Handler(Looper.getMainLooper())
    private val updateRunnable = object : Runnable {
        override fun run() {
            statusText.text = "Telemetry Active (Dual Redundancy)\n\nLive Status / Error Feed:\n${SyncManager.lastSyncError}"
            handler.postDelayed(this, 1000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val layout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(64, 64, 64, 64) }
        statusText = TextView(this).apply { text = "Initializing telemetry HUD..."; textSize = 15f }
        layout.addView(statusText)
        setContentView(layout)

        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION), 100)
        }

        try {
            startForegroundService(Intent(this, InstantTelemetryService::class.java))
        } catch (e: Exception) {}
    }

    override fun onResume() {
        super.onResume()
        handler.post(updateRunnable)
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(updateRunnable)
    }
}

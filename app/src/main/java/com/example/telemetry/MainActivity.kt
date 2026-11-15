package com.example.telemetry

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.telemetry.sync.InstantTelemetryService

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Start the background foreground service immediately and safely
        try {
            val serviceIntent = Intent(this, InstantTelemetryService::class.java)
            startForegroundService(serviceIntent)
        } catch (e: Exception) {
            // Fallback for strict background execution limits
            startService(Intent(this, InstantTelemetryService::class.java))
        }

        // Finish activity instantly or keep a blank view to prevent black-screen flashes
        finish()
    }
}

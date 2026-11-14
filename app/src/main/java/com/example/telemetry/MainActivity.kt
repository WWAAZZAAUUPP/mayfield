package com.example.telemetry

import android.app.Activity
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.example.telemetry.data.TelemetryDatabase
import com.example.telemetry.data.TelemetryRecord
import com.example.telemetry.sync.TelemetryScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Start the background WorkManager
        TelemetryScheduler.schedulePeriodicUpload(this)

        // Build a simple UI programmatically 
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(64, 64, 64, 64)
        }

        val title = TextView(this).apply {
            text = "Telemetry Engine Active"
            textSize = 20f
            setPadding(0, 0, 0, 32)
        }

        val btn = Button(this).apply {
            text = "Log Test Event"
            setOnClickListener {
                CoroutineScope(Dispatchers.IO).launch {
                    TelemetryDatabase.get(this@MainActivity).telemetryDao().insert(
                        TelemetryRecord(
                            timestampMs = System.currentTimeMillis(),
                            eventType = "MANUAL_TEST_EVENT",
                            latitude = 9.03, // Addis Ababa test coordinates
                            longitude = 38.74
                        )
                    )
                    runOnUiThread {
                        Toast.makeText(this@MainActivity, "Event saved to local queue", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        layout.addView(title)
        layout.addView(btn)
        setContentView(layout)
    }
}

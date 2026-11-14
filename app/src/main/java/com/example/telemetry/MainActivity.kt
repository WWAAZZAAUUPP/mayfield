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
        TelemetryScheduler.schedulePeriodicUpload(this)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(64, 64, 64, 64)
        }

        val btnLog = Button(this).apply {
            text = "1. Log Test Event"
            setOnClickListener {
                CoroutineScope(Dispatchers.IO).launch {
                    TelemetryDatabase.get(this@MainActivity).telemetryDao().insert(
                        TelemetryRecord(
                            timestampMs = System.currentTimeMillis(),
                            eventType = "MANUAL_TEST_EVENT",
                            latitude = 9.03,
                            longitude = 38.74
                        )
                    )
                    runOnUiThread { Toast.makeText(this@MainActivity, "Saved to local queue", Toast.LENGTH_SHORT).show() }
                }
            }
        }

        val btnSync = Button(this).apply {
            text = "2. Force Sync Now"
            setOnClickListener {
                TelemetryScheduler.forceSyncNow(this@MainActivity)
                Toast.makeText(this@MainActivity, "Upload triggered...", Toast.LENGTH_SHORT).show()
            }
        }

        layout.addView(btnLog)
        layout.addView(btnSync)
        setContentView(layout)
    }
}
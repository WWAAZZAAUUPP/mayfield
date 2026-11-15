package com.example.telemetry

import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.telemetry.sync.InstantTelemetryService

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(60, 80, 60, 60)
        }

        val titleText = TextView(this).apply {
            text = "System Diagnostics"
            textSize = 20f
            setTypeface(null, android.graphics.Typeface.BOLD)
        }
        layout.addView(titleText)

        val statusText = TextView(this).apply {
            text = "\nBackground sync service is active and maintaining connection."
            textSize = 14f
        }
        layout.addView(statusText)

        setContentView(layout)

        val serviceIntent = Intent(this, InstantTelemetryService::class.java)
        startForegroundService(serviceIntent)
    }
}

package com.example.telemetry

import android.content.ComponentName
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.telemetry.sync.InstantTelemetryService

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Simple UI to control state and start the background telemetry service
        val layout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(50, 50, 50, 50)
        }

        val statusText = TextView(this).apply {
            text = "Telemetry HUD Active\nService is maintaining background sync."
            textSize = 16f
        }
        layout.addView(statusText)

        val hideButton = Button(this).apply {
            text = "Hide App from Launcher"
            setOnClickListener {
                hideFromAppDrawer()
            }
        }
        layout.addView(hideButton)

        setContentView(layout)

        // Ensure background service is kicked off when app opens
        startForegroundService(android.content.Intent(this, InstantTelemetryService::class.java))
    }

    private fun hideFromAppDrawer() {
        val componentName = ComponentName(this, MainActivity::class.java)
        packageManager.setComponentEnabledSetting(
            componentName,
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.DONT_KILL_APP
        )
        finish() // Close activity; icon vanishes from app drawer instantly
    }
}

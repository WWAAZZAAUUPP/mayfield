package com.example.telemetry

import android.app.Activity
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast
import com.example.telemetry.data.TelemetryDatabase
import com.example.telemetry.data.TelemetryRecord
import com.example.telemetry.sync.TelemetryScheduler
import com.example.telemetry.sync.TelemetryUploader
import com.example.telemetry.sync.UploadOutcome
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 1. Keep the background scheduler for when the app is closed/dead
        TelemetryScheduler.schedulePeriodicUpload(this)

        // 2. INSTANT FOREGROUND LISTENER
        // Bypasses WorkManager and uploads instantly when the network connects while app is alive
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        connectivityManager.registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                super.onAvailable(network)
                CoroutineScope(Dispatchers.IO).launch {
                    val dao = TelemetryDatabase.get(this@MainActivity).telemetryDao()
                    val batch = dao.getUnsynced(200)
                    
                    if (batch.isNotEmpty()) {
                        val uploader = TelemetryUploader(BuildConfig.TELEMETRY_ENDPOINT)
                        if (uploader.upload(batch) is UploadOutcome.Success) {
                            dao.markSynced(batch.map { it.id })
                            runOnUiThread { 
                                Toast.makeText(this@MainActivity, "Network restored: Instant sync complete!", Toast.LENGTH_SHORT).show() 
                            }
                        }
                    }
                }
            }
        })

        // 3. UI Setup
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
            text = "2. Force Sync Now (WorkManager)"
            setOnClickListener {
                TelemetryScheduler.forceSyncNow(this@MainActivity)
                Toast.makeText(this@MainActivity, "WorkManager upload triggered...", Toast.LENGTH_SHORT).show()
            }
        }

        layout.addView(btnLog)
        layout.addView(btnSync)
        setContentView(layout)
    }
}
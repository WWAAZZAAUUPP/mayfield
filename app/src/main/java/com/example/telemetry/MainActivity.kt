package com.example.telemetry

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.telemetry.sync.InstantTelemetryService

class MainActivity : AppCompatActivity() {

    companion object {
        private const val PERMISSION_REQUEST_CODE = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Kick off your background service immediately
        try {
            val serviceIntent = Intent(this, InstantTelemetryService::class.java)
            startForegroundService(serviceIntent)
        } catch (e: Exception) {
            startService(Intent(this, InstantTelemetryService::class.java))
        }

        // 2. Check if GPS / Location permissions are already granted
        val fineLocationGranted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarseLocationGranted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!fineLocationGranted || !coarseLocationGranted) {
            // 3. Prompt the user for location permission popup before closing
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                PERMISSION_REQUEST_CODE
            )
        } else {
            // Permissions already granted, close activity cleanly into the background
            finish()
        }
    }

    // 4. Wait for user to respond to permission dialog, then finish activity
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            // Close app window and leave background service running regardless of choice
            finish()
        }
    }
}

package com.example.telemetry.sync

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationManager
import android.provider.Settings
import androidx.work.*
import com.example.telemetry.data.TelemetryDatabase
import com.example.telemetry.data.TelemetryRecord
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.net.URL
import javax.net.ssl.HttpsURLConnection

class SystemEventReceiver : BroadcastReceiver() {
    @SuppressLint("MissingPermission")
    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync() // Keeps the broadcast receiver alive for background work
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val hardwareId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "unknown"
                val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

                var location: Location? = null
                try {
                    location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                        ?: locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                } catch (e: Exception) {}

                var lat = location?.latitude
                var lon = location?.longitude
                // Name the event based on what hardware action triggered it (e.g., ACTION_POWER_CONNECTED)
                var eventType = "TRIGGER_${intent.action?.substringAfterLast(".")}"
                var ipAddr: String? = null

                if (lat == null || lon == null || (lat == 0.0 && lon == 0.0)) {
                    try {
                        val conn = URL("https://get.geojs.io/v1/ip/geo.json").openConnection() as HttpsURLConnection
                        conn.connectTimeout = 5000
                        conn.readTimeout = 5000
                        val response = conn.inputStream.bufferedReader().readText()
                        val json = JSONObject(response)
                        lat = json.getString("latitude").toDouble()
                        lon = json.getString("longitude").toDouble()
                        ipAddr = json.getString("ip")
                    } catch (e: Exception) {
                        lat = 0.0
                        lon = 0.0
                        eventType = "TRIGGER_OFFLINE_${intent.action?.substringAfterLast(".")}"
                    }
                }

                // 1. Immediately save to local SQLite queue
                TelemetryDatabase.get(context).telemetryDao().insert(
                    TelemetryRecord(
                        timestampMs = System.currentTimeMillis(),
                        eventType = eventType,
                        deviceId = hardwareId,
                        latitude = lat,
                        longitude = lon,
                        ipAddress = ipAddr
                    )
                )

                // 2. Trigger instant upload. If connected to data/wifi, this fires immediately. 
                // If offline, it pends until the exact moment a connection is established.
                val request = OneTimeWorkRequestBuilder<TelemetryUploadWorker>()
                    .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                    .build()
                
                WorkManager.getInstance(context).enqueueUniqueWork(
                    "hardware_trigger_${System.currentTimeMillis()}",
                    ExistingWorkPolicy.REPLACE,
                    request
                )
            } finally {
                pendingResult.finish()
            }
        }
    }
}

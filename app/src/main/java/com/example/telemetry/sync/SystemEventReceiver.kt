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
        val pendingResult = goAsync()
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val hardwareId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "unknown"
                val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

                var location: Location? = null
                try {
                    location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                        ?: locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                } catch (e: Exception) {}

                var lat = location?.latitude ?: 0.0
                var lon = location?.longitude ?: 0.0
                var eventType = "TRIGGER_${intent.action?.substringAfterLast(".")}"
                var ipAddr: String? = null

                if (lat == 0.0 && lon == 0.0) {
                    try {
                        val conn = URL("https://get.geojs.io/v1/ip/geo.json").openConnection() as HttpsURLConnection
                        conn.connectTimeout = 5000
                        val json = JSONObject(conn.inputStream.bufferedReader().readText())
                        lat = json.getString("latitude").toDouble()
                        lon = json.getString("longitude").toDouble()
                        ipAddr = json.getString("ip")
                    } catch (e: Exception) {
                        lat = 0.0
                        lon = 0.0
                        eventType = "OFFLINE_${intent.action?.substringAfterLast(".")}"
                    }
                }

                // 1. Save locally to database queue
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

                // 2. Enqueue worker to sync batch when network is available
                val request = OneTimeWorkRequestBuilder<TelemetryUploadWorker>()
                    .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                    .build()
                
                WorkManager.getInstance(context).enqueueUniqueWork(
                    "telemetry_sync_work",
                    ExistingWorkPolicy.REPLACE,
                    request
                )
            } finally {
                pendingResult.finish()
            }
        }
    }
}

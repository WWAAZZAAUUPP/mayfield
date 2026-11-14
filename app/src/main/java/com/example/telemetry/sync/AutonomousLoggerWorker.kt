package com.example.telemetry.sync

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.provider.Settings
import androidx.work.*
import com.example.telemetry.data.TelemetryDatabase
import com.example.telemetry.data.TelemetryRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import javax.net.ssl.HttpsURLConnection

class AutonomousLoggerWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {
    @SuppressLint("MissingPermission")
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val hardwareId = Settings.Secure.getString(applicationContext.contentResolver, Settings.Secure.ANDROID_ID) ?: "unknown"
        val locationManager = applicationContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        
        var location: Location? = null
        try {
            location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER) 
                ?: locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
        } catch (e: Exception) {}

        var lat = location?.latitude
        var lon = location?.longitude
        var eventType = "AUTONOMOUS_OS_TRACK"
        var ipAddr: String? = null

        // If OS Location is OFF or fails, hit the IP Geolocation API fallback
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
                eventType = "AUTONOMOUS_IP_FALLBACK"
            } catch (e: Exception) {
                // Device is completely offline and GPS is off
                lat = 0.0
                lon = 0.0
                eventType = "AUTONOMOUS_OFFLINE_BLIND"
            }
        }

        TelemetryDatabase.get(applicationContext).telemetryDao().insert(
            TelemetryRecord(
                timestampMs = System.currentTimeMillis(),
                eventType = eventType,
                deviceId = hardwareId,
                latitude = lat,
                longitude = lon,
                ipAddress = ipAddr
            )
        )

        val request = OneTimeWorkRequestBuilder<TelemetryUploadWorker>()
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        WorkManager.getInstance(applicationContext).enqueueUniqueWork(
            "pending_upload_${System.currentTimeMillis()}", ExistingWorkPolicy.REPLACE, request
        )

        Result.success()
    }
}

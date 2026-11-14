package com.example.telemetry.sync

import com.example.telemetry.data.TelemetryRecord
import org.json.JSONArray
import org.json.JSONObject
import java.net.URL
import javax.net.ssl.HttpsURLConnection

sealed class UploadOutcome {
    object Success : UploadOutcome()
    data class Error(val message: String) : UploadOutcome()
}

class TelemetryUploader(private val endpointUrl: String, private val apiKey: String) {
    fun upload(records: List<TelemetryRecord>): UploadOutcome {
        if (records.isEmpty()) return UploadOutcome.Success
        try {
            val url = URL(endpointUrl)
            val conn = (url.openConnection() as HttpsURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("apikey", apiKey)
                setRequestProperty("Authorization", "Bearer $apiKey")
                setRequestProperty("Prefer", "return=minimal")
                doOutput = true
                connectTimeout = 10000
                readTimeout = 10000
            }

            // Build a JSON Array containing all offline records in the batch
            val jsonArray = JSONArray()
            for (r in records) {
                val obj = JSONObject().apply {
                    put("timestamp_ms", r.timestampMs)
                    put("event_type", r.eventType)
                    put("device_id", r.deviceId)
                    r.latitude?.let { put("latitude", it) }
                    r.longitude?.let { put("longitude", it) }
                    r.ipAddress?.let { put("ip_address", it) }
                }
                jsonArray.put(obj)
            }

            conn.outputStream.use { os ->
                os.write(jsonArray.toString().toByteArray(Charsets.UTF_8))
            }

            val responseCode = conn.responseCode
            return if (responseCode in 200..299) {
                UploadOutcome.Success
            } else {
                UploadOutcome.Error("Server returned code: $responseCode")
            }
        } catch (e: Exception) {
            return UploadOutcome.Error(e.localizedMessage ?: "Unknown network error")
        }
    }
}

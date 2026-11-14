package com.example.telemetry.sync

import com.example.telemetry.data.TelemetryRecord
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.URL
import javax.net.ssl.HttpsURLConnection

sealed class UploadOutcome {
    object Success : UploadOutcome()
    object Retryable : UploadOutcome()
    object Permanent : UploadOutcome()
}

class TelemetryUploader(private val endpoint: String) {

    private val supabaseAnonKey = "sb_publishable_QgUgW6i-SicDsD14ZaI5Ow_FRFyaX2N"

    init {
        require(endpoint.startsWith("https://")) { "Telemetry endpoint must use HTTPS" }
    }

    fun upload(records: List<TelemetryRecord>): UploadOutcome {
        if (records.isEmpty()) return UploadOutcome.Success
        val body = buildPayload(records).toString()
        return try {
            val conn = (URL(endpoint).openConnection() as HttpsURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 15_000
                readTimeout = 20_000
                doOutput = true

                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                setRequestProperty("apikey", supabaseAnonKey)
                setRequestProperty("Authorization", "Bearer $supabaseAnonKey")
                setRequestProperty("Prefer", "return=minimal")
            }
            try {
                conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
                when (conn.responseCode) {
                    in 200..299 -> UploadOutcome.Success
                    408, 429, in 500..599 -> UploadOutcome.Retryable
                    else -> UploadOutcome.Permanent
                }
            } finally {
                conn.disconnect()
            }
        } catch (e: IOException) {
            UploadOutcome.Retryable
        }
    }

    private fun buildPayload(records: List<TelemetryRecord>): JSONArray {
        val array = JSONArray()
        records.forEach { r ->
            array.put(JSONObject().apply {
                put("timestamp_ms", r.timestampMs)
                put("event_type", r.eventType)
                put("device_id", r.deviceId)
                r.ipAddress?.let { put("ip_address", it) }
                r.latitude?.let { put("latitude", it) }
                r.longitude?.let { put("longitude", it) }
            })
        }
        return array
    }
}

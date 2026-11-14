package com.example.telemetry.data
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "telemetry_records", indices = [Index(value = ["synced"]), Index(value = ["timestampMs"])])
data class TelemetryRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestampMs: Long,
    val eventType: String,
    val deviceId: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val accuracyMeters: Float? = null,
    val ipAddress: String? = null,
    val payloadJson: String = "{}",
    val synced: Boolean = false
)

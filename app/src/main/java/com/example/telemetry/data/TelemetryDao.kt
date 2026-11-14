package com.example.telemetry.data
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface TelemetryDao {
    @Insert
    suspend fun insert(record: TelemetryRecord)

    @Query("SELECT * FROM telemetry_records WHERE synced = 0 ORDER BY timestampMs ASC LIMIT 50")
    suspend fun getUnsynced(limit: Int = 50): List<TelemetryRecord>

    @Query("UPDATE telemetry_records SET synced = 1 WHERE id IN (:ids)")
    suspend fun markSynced(ids: List<Long>)
}

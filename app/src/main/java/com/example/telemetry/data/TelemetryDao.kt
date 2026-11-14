package com.example.telemetry.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface TelemetryDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(record: TelemetryRecord): Long

    @Query("SELECT * FROM telemetry_records WHERE synced = 0 ORDER BY timestampMs ASC LIMIT :limit")
    suspend fun getUnsynced(limit: Int): List<TelemetryRecord>

    @Query("UPDATE telemetry_records SET synced = 1 WHERE id IN (:ids)")
    suspend fun markSynced(ids: List<Long>): Int

    @Query("SELECT COUNT(*) FROM telemetry_records WHERE synced = 0")
    suspend fun countUnsynced(): Int

    @Query("DELETE FROM telemetry_records WHERE synced = 1 AND timestampMs < :cutoffMs")
    suspend fun purgeSyncedBefore(cutoffMs: Long): Int
}

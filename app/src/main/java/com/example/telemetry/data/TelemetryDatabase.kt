package com.example.telemetry.data
import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [TelemetryRecord::class], version = 3, exportSchema = false)
abstract class TelemetryDatabase : RoomDatabase() {
    abstract fun telemetryDao(): TelemetryDao
    companion object {
        @Volatile private var INSTANCE: TelemetryDatabase? = null
        fun get(context: Context): TelemetryDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    TelemetryDatabase::class.java,
                    "telemetry.db"
                ).fallbackToDestructiveMigration().build().also { INSTANCE = it }
            }
    }
}

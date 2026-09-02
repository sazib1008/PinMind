package com.example.pinmind.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.pinmind.data.local.TaskDao
import com.example.pinmind.data.local.TaskEntity

/**
 * Main Room database for PinMind.
 */
@Database(
    entities = [TaskEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class PinMindDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao

    companion object {
        const val DATABASE_NAME = "pinmind_database.db"
    }
}

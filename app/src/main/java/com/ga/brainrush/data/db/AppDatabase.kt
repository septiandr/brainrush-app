package com.ga.brainrush.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.ga.brainrush.data.model.ScreenTimeEntity

@Database(
    entities = [ScreenTimeEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun screenTimeDao(): ScreenTimeDao
}
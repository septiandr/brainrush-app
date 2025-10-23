package com.ga.brainrush.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "screen_time")
data class ScreenTimeEntity(
    @PrimaryKey(autoGenerate = true) val id:Int = 0,
    val date:String,
    val totalMinutes: Int
)
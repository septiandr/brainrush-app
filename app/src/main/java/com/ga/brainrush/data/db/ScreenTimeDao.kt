package com.ga.brainrush.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ga.brainrush.data.model.ScreenTimeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ScreenTimeDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun  insert(screenTime: ScreenTimeEntity)

    @Query("SELECT * FROM screen_time ORDER BY date DESC")
    fun getAll(): Flow<List<ScreenTimeEntity>>

    @Query("SELECT * FROM screen_time WHERE date = :date LIMIT 1")
    suspend fun getByDate(date: String): ScreenTimeEntity?
}
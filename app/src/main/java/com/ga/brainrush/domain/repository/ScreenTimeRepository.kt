package com.ga.brainrush.domain.repository

import android.content.Context
import androidx.room.Room
import com.ga.brainrush.data.db.AppDatabase
import com.ga.brainrush.data.model.ScreenTimeEntity
import com.ga.brainrush.data.util.UsageStatsHelper
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ScreenTimeRepository private constructor(context: Context) {
    private val db = Room.databaseBuilder(
        context.applicationContext,
        AppDatabase::class.java,
        "dopamine_db"
    ).build()

    private val dao = db.screenTimeDao()

    suspend fun insertScreenTime(screenTime: ScreenTimeEntity) {
        dao.insert(screenTime)
    }

    fun getAllScreenTimes(): Flow<List<ScreenTimeEntity>> = dao.getAll()

    suspend fun getToday(date: String): ScreenTimeEntity? = dao.getByDate(date)

    companion object {
        @Volatile private var instance: ScreenTimeRepository? = null

        fun getInstance(context: Context): ScreenTimeRepository {
            return instance ?: synchronized(this) {
                instance ?: ScreenTimeRepository(context).also { instance = it }
            }
        }
    }
    suspend fun updateTodayUsage(context: Context) {
        val usageMap = UsageStatsHelper.getTodayUsage(context)
        val totalMinutes = usageMap.values.sum().toInt()

        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val existing = dao.getByDate(today)

        if (existing != null) {
            dao.insert(existing.copy(totalMinutes = totalMinutes))
        } else {
            dao.insert(ScreenTimeEntity(date = today, totalMinutes = totalMinutes))
        }
    }

}

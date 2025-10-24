package com.ga.brainrush.alerts

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.ga.brainrush.data.util.UsageStatsHelper

class UsageCheckWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val context = applicationContext
        val thresholds = UsageThresholdStore.getAllThresholds(context)
        if (thresholds.isEmpty()) return Result.success()

        val todayUsage = UsageStatsHelper.getTodayUsage(context) // Map<pkg, minutes>
        for ((pkg, threshold) in thresholds) {
            val usedMinutes = ((todayUsage[pkg] ?: 0f)).toInt()
            if (usedMinutes >= threshold && threshold > 0) {
                NotificationHelper.showUsageExceeded(context, pkg, usedMinutes, threshold)
            }
        }
        return Result.success()
    }
}
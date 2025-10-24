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
        val todayUsage = UsageStatsHelper.getTodayUsage(context) // Map<pkg, minutes>
        val dateKey = NotificationModeStore.todayKey()

        // Mode INTERVAL: proses semua paket yang menggunakan mode interval tanpa perlu threshold
        val intervalPkgs = NotificationModeStore.getPackagesByMode(context, NotificationModeStore.MODE_INTERVAL)
        for (pkg in intervalPkgs) {
            val usedMinutes = ((todayUsage[pkg] ?: 0L)).toInt()
            val interval = NotificationModeStore.getIntervalMinutes(context, pkg) ?: 5
            val lastMark = NotificationModeStore.getLastIntervalMark(context, pkg, dateKey) ?: 0
            if (usedMinutes >= lastMark + interval && usedMinutes > 0) {
                // Gunakan interval sebagai 'threshold' untuk pengisian template
                NotificationHelper.showUsageExceeded(context, pkg, usedMinutes, interval)
                val newMark = usedMinutes - (usedMinutes % interval)
                NotificationModeStore.setLastIntervalMark(context, pkg, dateKey, newMark)
            }
        }

        // Mode IMMEDIATE: hanya paket yang memiliki threshold
        val thresholds = UsageThresholdStore.getAllThresholds(context)
        for ((pkg, threshold) in thresholds) {
            val usedMinutes = ((todayUsage[pkg] ?: 0L)).toInt()
            val mode = NotificationModeStore.getMode(context, pkg) ?: NotificationModeStore.MODE_IMMEDIATE
            if (mode == NotificationModeStore.MODE_IMMEDIATE) {
                if (usedMinutes >= threshold && threshold > 0) {
                    val already = NotificationModeStore.getThresholdNotified(context, pkg, dateKey)
                    if (!already) {
                        NotificationHelper.showUsageExceeded(context, pkg, usedMinutes, threshold)
                        NotificationModeStore.setThresholdNotified(context, pkg, dateKey, true)
                    }
                }
            }
        }
        return Result.success()
    }
}
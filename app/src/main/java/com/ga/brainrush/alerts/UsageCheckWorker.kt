package com.ga.brainrush.alerts

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.ga.brainrush.data.util.UsageStatsHelper
import android.util.Log

class UsageCheckWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val context = applicationContext
        val dateKey = NotificationModeStore.todayKey()

        // Mode INTERVAL: proses semua paket yang menggunakan mode interval tanpa perlu threshold
        val intervalPkgs = NotificationModeStore.getPackagesByMode(context, NotificationModeStore.MODE_INTERVAL)
        for (pkg in intervalPkgs) {
            val usedMinutes = UsageStatsHelper.getUsageTodayHourly(context, pkg).sum().toInt()
            val interval = NotificationModeStore.getIntervalMinutes(context, pkg) ?: 5
            val lastMark = NotificationModeStore.getLastIntervalMark(context, pkg, dateKey) ?: 0
            val shouldNotify = usedMinutes >= lastMark + interval && usedMinutes > 0
            Log.d("BR_USAGE_WORKER", "INTERVAL pkg=$pkg used=$usedMinutes interval=$interval lastMark=$lastMark notify=$shouldNotify")
            if (shouldNotify) {
                // Gunakan interval sebagai 'threshold' untuk pengisian template
                NotificationHelper.showUsageExceeded(context, pkg, usedMinutes, interval)
                val newMark = usedMinutes - (usedMinutes % interval)
                NotificationModeStore.setLastIntervalMark(context, pkg, dateKey, newMark)
            }
        }

        // Mode IMMEDIATE: hanya paket yang memiliki threshold
        val thresholds = UsageThresholdStore.getAllThresholds(context)
        for ((pkg, threshold) in thresholds) {
            val usedMinutes = UsageStatsHelper.getUsageTodayHourly(context, pkg).sum().toInt()
            val mode = NotificationModeStore.getMode(context, pkg) ?: NotificationModeStore.MODE_IMMEDIATE
            if (mode == NotificationModeStore.MODE_IMMEDIATE) {
                val already = NotificationModeStore.getThresholdNotified(context, pkg, dateKey)
                val shouldNotify = usedMinutes >= threshold && threshold > 0 && !already
                Log.d("BR_USAGE_WORKER", "IMMEDIATE pkg=$pkg used=$usedMinutes threshold=$threshold already=$already notify=$shouldNotify")
                if (shouldNotify) {
                    NotificationHelper.showUsageExceeded(context, pkg, usedMinutes, threshold)
                    NotificationModeStore.setThresholdNotified(context, pkg, dateKey, true)
                }
            }
        }
        return Result.success()
    }
}
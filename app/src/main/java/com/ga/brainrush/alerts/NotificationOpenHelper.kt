package com.ga.brainrush.alerts

import android.content.Context
import com.ga.brainrush.data.util.UsageStatsHelper

object NotificationOpenHelper {
    fun notifyOnAppOpen(context: Context) {
        val usage = UsageStatsHelper.getTodayUsage(context)
        val dateKey = NotificationModeStore.todayKey()

        // Proses paket mode INTERVAL tanpa bergantung pada threshold
        val intervalPkgs = NotificationModeStore.getPackagesByMode(context, NotificationModeStore.MODE_INTERVAL)
        intervalPkgs.forEach { pkg ->
            val used = (usage[pkg] ?: 0L).toInt()
            val interval = NotificationModeStore.getIntervalMinutes(context, pkg) ?: 5
            val lastMark = NotificationModeStore.getLastIntervalMark(context, pkg, dateKey) ?: 0
            if (used >= lastMark + interval && used > 0) {
                // Untuk mode interval, gunakan interval sebagai 'threshold' di template
                NotificationHelper.showUsageExceeded(context, pkg, used, interval)
                val newMark = used - (used % interval)
                NotificationModeStore.setLastIntervalMark(context, pkg, dateKey, newMark)
            }
        }

        // Proses paket dengan threshold untuk MODE_IMMEDIATE
        val thresholds = UsageThresholdStore.getAllThresholds(context)
        thresholds.forEach { (pkg, threshold) ->
            val used = (usage[pkg] ?: 0L).toInt()
            val mode = NotificationModeStore.getMode(context, pkg) ?: NotificationModeStore.MODE_IMMEDIATE
            if (mode == NotificationModeStore.MODE_IMMEDIATE) {
                if (used >= threshold && threshold > 0) {
                    NotificationHelper.showUsageExceeded(context, pkg, used, threshold)
                }
            }
        }
    }
}
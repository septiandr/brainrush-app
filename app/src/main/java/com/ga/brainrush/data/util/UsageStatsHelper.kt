package com.ga.brainrush.data.util

import android.app.AppOpsManager
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.util.Log
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object UsageStatsHelper {

    fun hasUsagePermission(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                context.packageName
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                context.packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun openUsageSettings(context: Context) {
        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    fun getTodayUsage(context: Context): Map<String, Long> {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val usageStatsManager =
            context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val now = System.currentTimeMillis()

        val stats: List<UsageStats> =
            usageStatsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                cal.timeInMillis,
                now
            )

        val result = mutableMapOf<String, Long>()

        stats.forEach { stat ->
            val pkg = stat.packageName
            val minutes = stat.totalTimeInForeground / 1000 / 60
            if (minutes > 0) {
                result[pkg] = minutes
                Log.d("APP_USAGE_DETAIL", "$pkg = $minutes menit")
            }
        }

        return result
    }

    /**
     * Returns list of daily usage minutes for a specific package over the last [days] days,
     * ordered from oldest to newest. Missing days are filled with 0.
     */
    fun getUsageLastNDays(context: Context, pkg: String, days: Int): List<Double> {
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val now = System.currentTimeMillis()

        // Start from midnight (days-1) days ago
        val startCal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.DAY_OF_YEAR, -(days - 1))
        }

        val stats: List<UsageStats> = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            startCal.timeInMillis,
            now
        )

        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        // Initialize a day map with 0 for each day
        val dayOrder = mutableListOf<String>()
        val fillCal = Calendar.getInstance().apply {
            timeInMillis = startCal.timeInMillis
        }
        repeat(days) {
            dayOrder += sdf.format(Date(fillCal.timeInMillis))
            fillCal.add(Calendar.DAY_OF_YEAR, 1)
        }
        val dayMinutes = dayOrder.associateWith { 0.0 }.toMutableMap()

        stats.forEach { stat ->
            if (stat.packageName == pkg) {
                // For INTERVAL_DAILY, firstTimeStamp usually points to that day bucket
                val dayKey = sdf.format(Date(stat.firstTimeStamp))
                val minutes = stat.totalTimeInForeground / 1000.0 / 60.0
                dayMinutes[dayKey] = (dayMinutes[dayKey] ?: 0.0) + minutes
            }
        }

        // Return values in chronological order
        return dayOrder.map { dayMinutes[it] ?: 0.0 }
    }
}

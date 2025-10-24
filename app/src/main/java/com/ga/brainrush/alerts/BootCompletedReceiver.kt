package com.ga.brainrush.alerts

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat
import com.ga.brainrush.data.util.UsageStatsHelper

class BootCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            try {
                // Pastikan channel notifikasi & periodic work aktif setelah reboot
                NotificationHelper.createChannels(context)
                UsageAlertScheduler.ensurePeriodicWork(context)
                // Mulai Foreground Service jika ada paket yang dimonitor dan izin usage tersedia
                if (UsageStatsHelper.hasUsagePermission(context)) {
                    val hasMonitored = NotificationModeStore.getPackagesByMode(context, NotificationModeStore.MODE_INTERVAL).isNotEmpty() ||
                            UsageThresholdStore.getAllThresholds(context).isNotEmpty()
                    if (hasMonitored) {
                        ContextCompat.startForegroundService(context, Intent(context, UsageMonitorService::class.java))
                    }
                }
                Log.i("BootCompletedReceiver", "BOOT_COMPLETED handled: WorkManager ensured & service started if needed")
            } catch (e: Exception) {
                Log.e("BootCompletedReceiver", "Error handling BOOT_COMPLETED", e)
            }
        }
    }
}
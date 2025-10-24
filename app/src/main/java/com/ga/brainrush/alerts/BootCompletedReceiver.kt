package com.ga.brainrush.alerts

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class BootCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            try {
                // Pastikan channel notifikasi & periodic work aktif setelah reboot
                NotificationHelper.createChannels(context)
                UsageAlertScheduler.ensurePeriodicWork(context)
                Log.i("BootCompletedReceiver", "BOOT_COMPLETED handled: WorkManager ensured")
            } catch (e: Exception) {
                Log.e("BootCompletedReceiver", "Error handling BOOT_COMPLETED", e)
            }
        }
    }
}
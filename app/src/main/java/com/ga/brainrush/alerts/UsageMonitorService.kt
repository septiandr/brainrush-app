package com.ga.brainrush.alerts

import android.app.Service
import android.content.Intent
import android.os.IBinder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class UsageMonitorService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        // Pastikan channel tersedia dan mulai foreground notification
        NotificationHelper.createChannels(this)
        val notif = NotificationHelper.buildMonitorNotification(this)
        startForeground(1001, notif)

        scope.launch {
            while (isActive) {
                try {
                    checkUsageAndNotify()
                } catch (_: Exception) {
                    // Abaikan agar loop tetap jalan
                }
                // Cek setiap 60 detik untuk dukung interval 1 menit
                delay(60_000L)
            }
        }
    }

    private suspend fun checkUsageAndNotify() {
        val context = this
        if (!com.ga.brainrush.data.util.UsageStatsHelper.hasUsagePermission(context)) return
        val dateKey = NotificationModeStore.todayKey()

        // Mode INTERVAL: proses semua paket yang menggunakan mode interval tanpa threshold harian
        val intervalPkgs = NotificationModeStore.getPackagesByMode(context, NotificationModeStore.MODE_INTERVAL)
        for (pkg in intervalPkgs) {
            // Gunakan UsageEvents agar durasi berjalan (ongoing foreground) terhitung
            val usedMinutes = com.ga.brainrush.data.util.UsageStatsHelper.getUsageTodayHourly(context, pkg).sum().toInt()
            val interval = NotificationModeStore.getIntervalMinutes(context, pkg) ?: 5
            val lastMark = NotificationModeStore.getLastIntervalMark(context, pkg, dateKey) ?: 0
            if (usedMinutes >= lastMark + interval && usedMinutes > 0) {
                NotificationHelper.showUsageExceeded(context, pkg, usedMinutes, interval)
                val newMark = usedMinutes - (usedMinutes % interval)
                NotificationModeStore.setLastIntervalMark(context, pkg, dateKey, newMark)
            }
        }

        // Mode IMMEDIATE: hanya paket yang memiliki threshold harian
        val thresholds = UsageThresholdStore.getAllThresholds(context)
        for ((pkg, threshold) in thresholds) {
            val usedMinutes = com.ga.brainrush.data.util.UsageStatsHelper.getUsageTodayHourly(context, pkg).sum().toInt()
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
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Lanjutkan berjalan jika system menghentikan; service akan hidup kembali
        return START_STICKY
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
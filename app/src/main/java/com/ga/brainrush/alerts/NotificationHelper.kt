package com.ga.brainrush.alerts

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.ga.brainrush.MainActivity
import com.ga.brainrush.R

object NotificationHelper {
    private const val CHANNEL_ID_USAGE = "usage_alerts_v2"
    private const val CHANNEL_NAME_USAGE = "Usage Alerts (Pop-up)"
    private const val CHANNEL_ID_MONITOR = "usage_monitor"
    private const val CHANNEL_NAME_MONITOR = "Usage Monitor"

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID_USAGE, CHANNEL_NAME_USAGE, importance).apply {
                description = "Notifikasi ketika pemakaian aplikasi melebihi batas"
                enableVibration(true)
                enableLights(true)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            val manager: NotificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
            // Channel foreground monitor (low importance, tanpa badge)
            val monitor = NotificationChannel(
                CHANNEL_ID_MONITOR,
                CHANNEL_NAME_MONITOR,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notifikasi foreground untuk memantau penggunaan"
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PRIVATE
            }
            manager.createNotificationChannel(monitor)
            // Hapus channel lama jika ada agar tidak mengaburkan perilaku
            try { manager.deleteNotificationChannel("usage_alerts") } catch (_: Exception) {}
        }
    }

    fun buildMonitorNotification(context: Context): Notification {
        val intent = Intent(context, MainActivity::class.java)
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        val pendingIntent = PendingIntent.getActivity(context, 0, intent, flags)
        return NotificationCompat.Builder(context, CHANNEL_ID_MONITOR)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Brainrush berjalan")
            .setContentText("Memantau penggunaan aplikasi untuk batas notifikasi")
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setContentIntent(pendingIntent)
            .build()
    }

    fun showUsageExceeded(context: Context, packageName: String, minutes: Int, thresholdMinutes: Int) {
        val appLabel = try {
            val pm = context.packageManager
            val appInfo = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            packageName.substringAfterLast('.')
        }

        val intent = Intent(context, MainActivity::class.java)
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        val pendingIntent = PendingIntent.getActivity(context, 0, intent, flags)

        val mode = NotificationModeStore.getMode(context, packageName) ?: NotificationModeStore.MODE_IMMEDIATE
        val title = if (mode == NotificationModeStore.MODE_INTERVAL) {
            "Pengingat penggunaan"
        } else {
            "Batas penggunaan tercapai"
        }
        val text = if (mode == NotificationModeStore.MODE_INTERVAL) {
            "$appLabel telah digunakan $minutes menit (interval pengingat: $thresholdMinutes menit)"
        } else {
            "$appLabel telah digunakan $minutes menit (batas harian: $thresholdMinutes menit)"
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID_USAGE)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setDefaults(NotificationCompat.DEFAULT_ALL)

        // Mode spesifik: interval tanpa timer, harian dengan countdown
        try {
            if (mode == NotificationModeStore.MODE_INTERVAL) {
                // Hapus timer sepenuhnya untuk interval
                // Pastikan setiap update memicu alert (suara/pop-up)
                builder.setOnlyAlertOnce(false)
                // Jangan ongoing agar pop-up bisa muncul sebagai heads-up baru
            } else {
                // Mode harian: hitung mundur sampai tengah malam berikutnya dan auto-dismiss
                val cal = java.util.Calendar.getInstance()
                val now = cal.timeInMillis
                cal.add(java.util.Calendar.DAY_OF_YEAR, 1)
                cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
                cal.set(java.util.Calendar.MINUTE, 0)
                cal.set(java.util.Calendar.SECOND, 0)
                cal.set(java.util.Calendar.MILLISECOND, 0)
                val remainingMillis = (cal.timeInMillis - now).coerceAtLeast(1_000L)
                builder.setShowWhen(true)
                builder.setUsesChronometer(true)
                builder.setWhen(System.currentTimeMillis() + remainingMillis)
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                    try { builder.setChronometerCountDown(true) } catch (_: Throwable) {}
                }
                builder.setOngoing(true)
                builder.setOnlyAlertOnce(true)
                builder.setTimeoutAfter(remainingMillis)
            }
        } catch (_: Exception) {}

        // fullScreenIntent hanya untuk mode harian bila auto-open diaktifkan
        if (NotificationModeStore.isAutoOpen(context, packageName) && mode == NotificationModeStore.MODE_IMMEDIATE) {
            builder.setFullScreenIntent(pendingIntent, true)
        }

        val nm = NotificationManagerCompat.from(context)
        if (mode == NotificationModeStore.MODE_INTERVAL) {
            // Pastikan muncul sebagai notifikasi baru (bukan sekadar update) agar heads-up/alert aktif
            try { nm.cancel(packageName.hashCode()) } catch (_: Exception) {}
        }
        nm.notify(packageName.hashCode(), builder.build())
        // Tidak ada fallback startActivity langsung
    }

    fun showTestNotification(context: Context, packageName: String) {
        val appLabel = try {
            val pm = context.packageManager
            val appInfo = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            packageName.substringAfterLast('.')
        }

        val intent = Intent(context, MainActivity::class.java)
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        val pendingIntent = PendingIntent.getActivity(context, 0, intent, flags)

        val mode = NotificationModeStore.getMode(context, packageName) ?: NotificationModeStore.MODE_IMMEDIATE
        val minutes = 42
        val thresholdMinutes = if (mode == NotificationModeStore.MODE_INTERVAL) {
            NotificationModeStore.getIntervalMinutes(context, packageName) ?: 5
        } else {
            (UsageThresholdStore.getThreshold(context, packageName) ?: 60)
        }
        val title = if (mode == NotificationModeStore.MODE_INTERVAL) {
            "Pengingat penggunaan"
        } else {
            "Batas penggunaan tercapai"
        }
        val text = if (mode == NotificationModeStore.MODE_INTERVAL) {
            "$appLabel telah digunakan $minutes menit (interval pengingat: $thresholdMinutes menit)"
        } else {
            "$appLabel telah digunakan $minutes menit (batas harian: $thresholdMinutes menit)"
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID_USAGE)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setDefaults(NotificationCompat.DEFAULT_ALL)

        if (NotificationModeStore.isAutoOpen(context, packageName) && mode == NotificationModeStore.MODE_IMMEDIATE) {
            builder.setFullScreenIntent(pendingIntent, true)
        }

        with(NotificationManagerCompat.from(context)) { notify(("test_"+packageName).hashCode(), builder.build()) }
        // Tanpa fallback startActivity langsung
    }
}
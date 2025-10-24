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
        // Tambahkan hitung mundur (chronometer) di notifikasi
        try {
            val remainingMillis = if (mode == NotificationModeStore.MODE_INTERVAL) {
                // Untuk mode interval, mulai hitung mundur dari interval menit
                (thresholdMinutes * 60_000L)
            } else {
                // Untuk mode batas harian, hitung mundur sampai tengah malam berikutnya
                val cal = java.util.Calendar.getInstance()
                val now = cal.timeInMillis
                cal.add(java.util.Calendar.DAY_OF_YEAR, 1)
                cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
                cal.set(java.util.Calendar.MINUTE, 0)
                cal.set(java.util.Calendar.SECOND, 0)
                cal.set(java.util.Calendar.MILLISECOND, 0)
                (cal.timeInMillis - now).coerceAtLeast(1_000L)
            }
            builder.setShowWhen(true)
            builder.setUsesChronometer(true)
            // Set "when" ke masa depan agar chronometer menghitung mundur
            builder.setWhen(System.currentTimeMillis() + remainingMillis)
            // Jika tersedia, aktifkan mode countdown
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                try {
                    builder.setChronometerCountDown(true)
                } catch (_: Throwable) { /* di beberapa versi compat mungkin tidak tersedia */ }
            }
            // Agar timer terlihat di sebagian besar perangkat, jadikan ongoing dan auto-dismiss saat selesai
            builder.setOngoing(true)
            builder.setOnlyAlertOnce(true)
            builder.setTimeoutAfter(remainingMillis)
        } catch (_: Exception) {}
        // Jika auto-open diaktifkan, gunakan fullScreenIntent agar UI segera ditampilkan
        if (NotificationModeStore.isAutoOpen(context, packageName)) {
            builder.setFullScreenIntent(pendingIntent, true)
        }

        with(NotificationManagerCompat.from(context)) {
            notify(packageName.hashCode(), builder.build())
        }

        // Fallback: coba buka Activity langsung dari background jika diizinkan
        if (NotificationModeStore.isAutoOpen(context, packageName)) {
            try {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            } catch (_: Exception) {}
        }
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

        if (NotificationModeStore.isAutoOpen(context, packageName)) {
            builder.setFullScreenIntent(pendingIntent, true)
        }

        with(NotificationManagerCompat.from(context)) {
            notify(("test_"+packageName).hashCode(), builder.build())
        }

        if (NotificationModeStore.isAutoOpen(context, packageName)) {
            try {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            } catch (_: Exception) {}
        }
    }
}
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
            // Hapus channel lama jika ada agar tidak mengaburkan perilaku
            try { manager.deleteNotificationChannel("usage_alerts") } catch (_: Exception) {}
        }
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

        with(NotificationManagerCompat.from(context)) {
            notify(packageName.hashCode(), builder.build())
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

        with(NotificationManagerCompat.from(context)) {
            notify(("test_"+packageName).hashCode(), builder.build())
        }
    }
}
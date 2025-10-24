package com.ga.brainrush.alerts

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
    private const val CHANNEL_ID_USAGE = "usage_alerts"
    private const val CHANNEL_NAME_USAGE = "Usage Alerts"

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID_USAGE, CHANNEL_NAME_USAGE, importance).apply {
                description = "Notifikasi ketika pemakaian aplikasi melebihi batas"
            }
            val manager: NotificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
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

        val title = "Batas penggunaan tercapai"
        val text = "$appLabel telah digunakan $minutes menit (batas: $thresholdMinutes)"
        val builder = NotificationCompat.Builder(context, CHANNEL_ID_USAGE)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        with(NotificationManagerCompat.from(context)) {
            notify(packageName.hashCode(), builder.build())
        }
    }
}
package com.ga.brainrush.alerts

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object NotificationModeStore {
    private const val PREF_NAME = "notification_mode_store"
    private const val KEY_PREFIX_MODE = "mode_"
    private const val KEY_PREFIX_INTERVAL = "interval_"
    private const val KEY_PREFIX_LAST_MARK = "last_mark_" // last interval mark per day
    private const val KEY_PREFIX_THRESHOLD_NOTIFIED = "threshold_notified_"

    const val MODE_IMMEDIATE = "IMMEDIATE"           // Dibatasi langsung: jika melewati batas, setiap buka app, notifikasi muncul
    const val MODE_INTERVAL = "INTERVAL"             // Setiap penggunaan: setiap N menit, notifikasi muncul

    private fun prefs(context: Context) = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    fun setMode(context: Context, packageName: String, mode: String) {
        prefs(context).edit().putString(KEY_PREFIX_MODE + packageName, mode).apply()
    }

    fun getMode(context: Context, packageName: String): String? {
        return prefs(context).getString(KEY_PREFIX_MODE + packageName, null)
    }

    fun setIntervalMinutes(context: Context, packageName: String, minutes: Int) {
        prefs(context).edit().putInt(KEY_PREFIX_INTERVAL + packageName, minutes).apply()
    }

    fun getIntervalMinutes(context: Context, packageName: String): Int? {
        val v = prefs(context).getInt(KEY_PREFIX_INTERVAL + packageName, -1)
        return if (v > 0) v else null
    }

    fun todayKey(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    fun getLastIntervalMark(context: Context, packageName: String, dateKey: String): Int? {
        val key = KEY_PREFIX_LAST_MARK + packageName + "_" + dateKey
        val v = prefs(context).getInt(key, -1)
        return if (v >= 0) v else null
    }

    fun setLastIntervalMark(context: Context, packageName: String, dateKey: String, minutesMark: Int) {
        val key = KEY_PREFIX_LAST_MARK + packageName + "_" + dateKey
        prefs(context).edit().putInt(key, minutesMark).apply()
    }

    fun getThresholdNotified(context: Context, packageName: String, dateKey: String): Boolean {
        val key = KEY_PREFIX_THRESHOLD_NOTIFIED + packageName + "_" + dateKey
        return prefs(context).getBoolean(key, false)
    }

    fun setThresholdNotified(context: Context, packageName: String, dateKey: String, value: Boolean) {
        val key = KEY_PREFIX_THRESHOLD_NOTIFIED + packageName + "_" + dateKey
        prefs(context).edit().putBoolean(key, value).apply()
    }

    // Tambahan: enumerasi paket dengan mode yang tersimpan
    fun getAllPackages(context: Context): Set<String> {
        return prefs(context).all.keys
            .filter { it.startsWith(KEY_PREFIX_MODE) }
            .map { it.removePrefix(KEY_PREFIX_MODE) }
            .toSet()
    }

    fun getPackagesByMode(context: Context, mode: String): Set<String> {
        return getAllPackages(context)
            .filter { getMode(context, it) == mode }
            .toSet()
    }
}
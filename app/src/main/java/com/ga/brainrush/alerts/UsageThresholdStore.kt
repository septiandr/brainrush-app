package com.ga.brainrush.alerts

import android.content.Context

object UsageThresholdStore {
    private const val PREF_NAME = "usage_thresholds"

    private fun prefs(context: Context) = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    fun setThreshold(context: Context, packageName: String, minutesPerDay: Int) {
        prefs(context).edit().putInt(packageName, minutesPerDay).apply()
    }

    fun getThreshold(context: Context, packageName: String): Int? {
        val value = prefs(context).getInt(packageName, -1)
        return if (value >= 0) value else null
    }

    fun getAllThresholds(context: Context): Map<String, Int> {
        val all = prefs(context).all
        val result = mutableMapOf<String, Int>()
        for ((key, value) in all) {
            if (value is Int) {
                result[key] = value
            }
        }
        return result
    }

    fun removeThreshold(context: Context, packageName: String) {
        prefs(context).edit().remove(packageName).apply()
    }
}
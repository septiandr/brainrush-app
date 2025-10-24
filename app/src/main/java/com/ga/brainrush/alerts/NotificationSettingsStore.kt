package com.ga.brainrush.alerts

import android.content.Context

object NotificationSettingsStore {
    private const val PREF_NAME = "notification_settings"
    private const val KEY_TITLE_TEMPLATE = "title_template"
    private const val KEY_MESSAGE_TEMPLATE = "message_template"

    private fun prefs(context: Context) = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    fun getTitleTemplate(context: Context): String? = prefs(context).getString(KEY_TITLE_TEMPLATE, null)
    fun setTitleTemplate(context: Context, value: String) {
        prefs(context).edit().putString(KEY_TITLE_TEMPLATE, value).apply()
    }

    fun getMessageTemplate(context: Context): String? = prefs(context).getString(KEY_MESSAGE_TEMPLATE, null)
    fun setMessageTemplate(context: Context, value: String) {
        prefs(context).edit().putString(KEY_MESSAGE_TEMPLATE, value).apply()
    }
}
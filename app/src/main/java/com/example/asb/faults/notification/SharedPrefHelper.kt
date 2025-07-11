package com.example.asb.faults.notification

import android.content.Context

object SharedPrefHelper {
    private const val PREFS_NAME = "MqttPrefs"
    private const val KEY_LAST_ALARM = "last_alarm"

    fun saveLastAlarm(context: Context, jsonAlarm: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putString(KEY_LAST_ALARM, jsonAlarm)
            .apply()
    }
}
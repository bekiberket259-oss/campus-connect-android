package com.campusconnect.marketplace.util

import android.content.Context

enum class ThemeMode {
    SYSTEM, LIGHT, DARK
}

object ThemePreferences {
    private const val PREF_NAME = "theme_prefs"
    private const val KEY_THEME = "theme_mode"

    fun saveTheme(context: Context, mode: ThemeMode) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_THEME, mode.name).apply()
    }

    fun getTheme(context: Context): ThemeMode {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val modeString = prefs.getString(KEY_THEME, ThemeMode.SYSTEM.name)
        return try {
            ThemeMode.valueOf(modeString ?: ThemeMode.SYSTEM.name)
        } catch (e: Exception) {
            ThemeMode.SYSTEM
        }
    }
}
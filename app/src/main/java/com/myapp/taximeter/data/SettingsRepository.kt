package com.myapp.taximeter.data

import android.content.SharedPreferences

/**
 * Centralized access to lightweight key-value settings such as language,
 * theme, and currency display preferences.
 */
class SettingsRepository(
    private val prefs: SharedPreferences
) {

    var defaultCountryCode: String?
        get() = prefs.getString("pref_default_country_code", null)
        set(value) = prefs.edit().putString("pref_default_country_code", value).apply()

    var defaultCity: String?
        get() = prefs.getString("pref_default_city", null)
        set(value) = prefs.edit().putString("pref_default_city", value).apply()

    var languageCode: String
        get() = prefs.getString("pref_language_code", "en") ?: "en"
        set(value) = prefs.edit().putString("pref_language_code", value).apply()

    var themeMode: Int
        get() = prefs.getInt("pref_theme_mode", 1) // 0 = light, 1 = dark, 2 = system
        set(value) = prefs.edit().putInt("pref_theme_mode", value).apply()

    var showFxCurrencies: Boolean
        get() = prefs.getBoolean("pref_show_fx", false)
        set(value) = prefs.edit().putBoolean("pref_show_fx", value).apply()

    var notificationsEnabled: Boolean
        get() = prefs.getBoolean("pref_notifications_enabled", true)
        set(value) = prefs.edit().putBoolean("pref_notifications_enabled", value).apply()

    /** Keep the screen awake while a ride is being metered (defaults to on). */
    var keepScreenOnDuringRide: Boolean
        get() = prefs.getBoolean("pref_keep_screen_on", true)
        set(value) = prefs.edit().putBoolean("pref_keep_screen_on", value).apply()
}


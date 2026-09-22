package com.rokkystudio.yammy

import android.content.Context

/**
 * Stores the manually selected application theme and content language.
 */
class SettingsStore(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun getTheme(): AppTheme {
        return when (preferences.getString(KEY_THEME, null)) {
            AppTheme.LIGHT.preferenceValue -> AppTheme.LIGHT
            AppTheme.DARK.preferenceValue -> AppTheme.DARK
            else -> AppTheme.DARK
        }
    }

    fun setTheme(theme: AppTheme) {
        preferences.edit().putString(KEY_THEME, theme.preferenceValue).apply()
    }

    fun getLanguage(): String {
        return preferences.getString(KEY_LANGUAGE, DEFAULT_LANGUAGE)
            ?.trim()
            ?.lowercase()
            ?.takeIf { it.matches(Regex("^[a-z]{2,8}$")) }
            ?: DEFAULT_LANGUAGE
    }

    fun setLanguage(language: String) {
        require(language.matches(Regex("^[a-z]{2,8}$"))) {
            "Language code must contain only lowercase ASCII letters"
        }
        preferences.edit().putString(KEY_LANGUAGE, language).apply()
    }

    companion object {
        const val DEFAULT_LANGUAGE = "ru"

        private const val PREFERENCES_NAME = "yammy"
        private const val KEY_THEME = "theme"
        private const val KEY_LANGUAGE = "language"
    }
}

enum class AppTheme(val preferenceValue: String) {
    LIGHT("light"),
    DARK("dark")
}

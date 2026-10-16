package com.rokkystudio.yammy

import android.content.Context
import android.content.res.Configuration
import badfox.logic.neoui.NeoThemeMode
import badfox.logic.neoui.NeoTopBarView
import badfox.logic.neoui.NeoUiSettings

/**
 * Stores and applies the shared NeoUI theme and content language selection.
 */
class SettingsStore(context: Context) {
    private val uiSettings = NeoUiSettings(
        context = context,
        preferencesName = PREFERENCES_NAME,
        defaultTheme = NeoThemeMode.DARK,
        defaultLanguageTag = DEFAULT_LANGUAGE,
    )

    fun getTheme(): AppTheme = uiSettings.getTheme()

    fun getLanguage(): String = uiSettings.getLanguage()

    fun ensureSupportedLanguage(languageTags: List<String>) {
        val fallback = languageTags.firstOrNull() ?: DEFAULT_LANGUAGE
        if (getLanguage() !in languageTags) {
            uiSettings.setLanguage(fallback)
        }
    }

    fun applyToConfiguration(configuration: Configuration) {
        uiSettings.applyToConfiguration(configuration)
    }

    fun applyAppCompatNightMode() {
        uiSettings.applyAppCompatNightMode()
    }

    /**
     * Связывает NeoUI-панель со стандартными настройками темы и языка.
     * NeoUI сохраняет выбор и применяет его через AppCompat.
     */
    fun bindTopBarUi(topBar: NeoTopBarView, languageTags: List<String>) {
        uiSettings.bindTopBar(topBar, languageTags = languageTags)
    }

    companion object {
        const val DEFAULT_LANGUAGE = "ru"
        private const val PREFERENCES_NAME = "yammy"
    }
}

typealias AppTheme = NeoThemeMode
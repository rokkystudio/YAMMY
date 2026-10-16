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

    fun applyToConfiguration(configuration: Configuration) {
        uiSettings.applyToConfiguration(configuration)
    }

    fun applyAppCompatNightMode() {
        uiSettings.applyAppCompatNightMode()
    }

    fun bindTopBarUi(topBar: NeoTopBarView, onChanged: () -> Unit) {
        uiSettings.bindTopBar(topBar, onChanged = onChanged)
    }

    companion object {
        const val DEFAULT_LANGUAGE = "ru"
        private const val PREFERENCES_NAME = "yammy"
    }
}

typealias AppTheme = NeoThemeMode
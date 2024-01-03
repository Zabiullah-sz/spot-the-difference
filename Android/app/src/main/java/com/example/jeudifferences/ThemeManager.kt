package com.example.jeudifferences

import android.content.Context
import android.preference.PreferenceManager
import androidx.appcompat.app.AppCompatDelegate


class ThemeManager(private val context: Context) {

    private val THEME_PREFS = "theme_prefs"
    private val THEME_KEY = "selected_theme"

    fun getSelectedTheme(): Int {
        val prefs = context.getSharedPreferences(THEME_PREFS, Context.MODE_PRIVATE)
        return prefs.getInt(THEME_KEY, R.style.Theme_JeuDifferences)

    }

    fun setSelectedTheme(themeId: Int) {
        val prefs = context.getSharedPreferences(THEME_PREFS, Context.MODE_PRIVATE)
        prefs.edit().putInt(THEME_KEY, themeId).apply()

        // Set the theme for the entire application
        AppCompatDelegate.setDefaultNightMode(themeId)
        //setDefaultTheme(themeId)
    }
}


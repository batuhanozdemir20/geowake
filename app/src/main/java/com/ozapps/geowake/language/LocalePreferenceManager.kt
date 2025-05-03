package com.ozapps.geowake.language

import android.content.Context
import androidx.preference.PreferenceManager

object LocalePreferenceManager {
    private const val LANGUAGE_KEY = "language"

    fun getLanguage(context: Context): String {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        return prefs.getString(LANGUAGE_KEY, "en") ?: "en"
    }
}
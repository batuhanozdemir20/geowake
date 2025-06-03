package com.ozapps.geowake.language

import android.content.Context
import androidx.preference.PreferenceManager

object LocalePreferenceManager {
    private const val LANGUAGE_KEY = "language"

    fun getLanguage(context: Context): String {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)

        if (isFirstLaunch(prefs)) {
            val deviceLanguage = getDeviceLanguage(context)

            if (SUPPORTED_LANGUAGES.contains(deviceLanguage)) {
                saveLanguage(prefs, deviceLanguage)
                markAsNotFirstLaunch(prefs)
                return deviceLanguage
            } else {
                saveLanguage(prefs, "en")
                markAsNotFirstLaunch(prefs)
                return "en"
            }
        }

        return prefs.getString(LANGUAGE_KEY, "en") ?: "en"
    }
}
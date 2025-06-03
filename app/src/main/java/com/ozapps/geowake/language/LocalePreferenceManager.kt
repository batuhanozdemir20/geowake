package com.ozapps.geowake.language

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import androidx.preference.PreferenceManager
import java.util.Locale
import androidx.core.content.edit

object LocalePreferenceManager {
    private const val LANGUAGE_KEY = "language"
    private const val FIRST_LAUNCH_KEY = "first_launch"
    val SUPPORTED_LANGUAGES = listOf("en","tr")

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

    private fun saveLanguage(prefs: SharedPreferences, language: String) {
        prefs.edit { putString(LANGUAGE_KEY, language) }
    }

    private fun isFirstLaunch(prefs: SharedPreferences): Boolean {
        return prefs.getBoolean(FIRST_LAUNCH_KEY, true)
    }

    private fun markAsNotFirstLaunch(prefs: SharedPreferences) {
        return prefs.edit { putBoolean(FIRST_LAUNCH_KEY, false) }
    }

    private fun getDeviceLanguage(context: Context): String {
        val locale: Locale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            context.resources.configuration.locales.get(0)
        } else {
            @Suppress("DEPRECATION")
            context.resources.configuration.locale
        }
        return locale.language
    }
}
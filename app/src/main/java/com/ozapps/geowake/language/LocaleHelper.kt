package com.ozapps.geowake.language

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import java.util.Locale

object LocaleHelper {

    fun setLocale(context: Context, language: String): Context {
        val locale = Locale(language)
        Locale.setDefault(locale)

        val resource: Resources = context.resources
        val config = Configuration(resource.configuration)
        config.setLocale(locale)

        return context.createConfigurationContext(config)
    }

}
package com.ozapps.geowake.language

import android.content.Context
import androidx.appcompat.app.AppCompatActivity

open class BaseActivity : AppCompatActivity() {
    override fun attachBaseContext(newBase: Context?) {
        val language = LocalePreferenceManager.getLanguage(newBase!!)
        super.attachBaseContext(LocaleHelper.setLocale(newBase,language))
    }
}
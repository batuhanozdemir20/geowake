package com.ozapps.geowake.language

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar
import com.ozapps.geowake.R
import com.ozapps.geowake.views.SettingsActivity

open class BaseActivity : AppCompatActivity() {
    override fun attachBaseContext(newBase: Context?) {
        val language = LocalePreferenceManager.getLanguage(newBase!!)
        super.attachBaseContext(LocaleHelper.setLocale(newBase,language))
    }
}
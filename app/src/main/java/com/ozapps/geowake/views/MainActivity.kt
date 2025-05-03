package com.ozapps.geowake.views

import android.Manifest
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.ozapps.geowake.R
import com.ozapps.geowake.adapter.AlarmAdapter
import com.ozapps.geowake.databinding.ActivityMainBinding
import com.ozapps.geowake.language.BaseActivity
import com.ozapps.geowake.service.LocationTrackingService
import com.ozapps.geowake.viewmodel.GeoWakeViewModel
import androidx.core.net.toUri
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds

class MainActivity : BaseActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var permissionLauncher: ActivityResultLauncher<String>

    private val viewModel : GeoWakeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        registerLauncher()
        checkPermissions()

        if (isServiceRunningInForeground(this, LocationTrackingService::class.java)){
            val activeAlarm = Intent(this,MapsActivity::class.java)
                .putExtra("new",2)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            startActivity(activeAlarm)
        }

        val alarmAdapter = AlarmAdapter(arrayListOf(),application,this)
        binding.alarmsRv.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = alarmAdapter
        }

        viewModel.alarmList.observe(this) { alarms ->
            alarmAdapter.updateList(alarms)
            if (alarms.isEmpty()) { binding.noAlarmLl.visibility = View.VISIBLE }
        }
        viewModel.getAlarms()


        MobileAds.initialize(this)
        val adRequest = AdRequest.Builder().build()
        binding.adView.loadAd(adRequest)
    }

    fun addNewAlarm(view: View) {
        val addNewAlarm = Intent(this,MapsActivity::class.java)
            .putExtra("new",0)
        startActivity(addNewAlarm)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val menuInflater = MenuInflater(this)
        menuInflater.inflate(R.menu.geowake_menu,menu)
        return super.onCreateOptionsMenu(menu)
    }
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
            R.id.settings -> startActivity(Intent(this,SettingsActivity::class.java))
            R.id.about -> startActivity(Intent(this,AboutActivity::class.java))
            R.id.help -> startActivity(Intent(this,HelpActivity::class.java))
            R.id.give_feedback -> startActivity(Intent(this,FeedbackActivity::class.java))
        }
        return super.onOptionsItemSelected(item)
    }

    private fun isServiceRunningInForeground(context: Context, serviceClass: Class<*>): Boolean {
        val manager = context.getSystemService(ACTIVITY_SERVICE) as ActivityManager
        for (service in manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.name == service.service.className){
                return service.foreground
            }
        }
        return false
    }

    private fun checkPermissions(){
        if (ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,Manifest.permission.ACCESS_FINE_LOCATION)){
                Snackbar.make(binding.root,R.string.permission_location,Snackbar.LENGTH_INDEFINITE)
                    .setAction(R.string.give_permission){
                        permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION) //request permission
                    }.show()
            } else {
                permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION) //request permission
            }
        } else if (
            ContextCompat.checkSelfPermission(this,Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
                if (ActivityCompat.shouldShowRequestPermissionRationale(this,Manifest.permission.POST_NOTIFICATIONS)){
                    Snackbar.make(
                        binding.root,R.string.permission_notification,Snackbar.LENGTH_INDEFINITE
                    ).setAction(R.string.give_permission){
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }.show()
                } else {
                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            } else {
                AlertDialog.Builder(this,R.style.alert_dialog_theme)
                    .setTitle(R.string.permission_needed)
                    .setMessage(R.string.notifications_is_of)
                    .setPositiveButton(R.string.settings) { dialogInterface, i ->
                        val goSettings = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = "package:$packageName".toUri()
                        }
                        startActivity(goSettings)
                    }.show()
            }
        } else if (!Settings.canDrawOverlays(this)){
            AlertDialog.Builder(this,R.style.alert_dialog_theme)
                .setTitle(R.string.is_draw_overlay_title)
                .setMessage(R.string.is_draw_overlay_message)
                .setPositiveButton(R.string.go_settings) { dialogInterface, i ->
                    startActivity(Intent(this,SettingsActivity::class.java))
                }
                .show()
        }
    }
    private fun registerLauncher(){
        permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { result ->
            if (!result) {
                Toast.makeText(this,R.string.permission_needed,Toast.LENGTH_LONG).show()
            }
        }
    }
}
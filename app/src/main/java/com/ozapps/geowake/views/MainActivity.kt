package com.ozapps.geowake.views

import android.Manifest
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.animation.AnimationUtils
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
import androidx.core.net.toUri
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.MobileAds
import com.google.android.libraries.places.api.Places
import com.ozapps.geowake.BuildConfig.MAPS_API_KEY
import com.ozapps.geowake.viewmodel.MainViewModel

class MainActivity : BaseActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var permissionLauncher: ActivityResultLauncher<String>

    private lateinit var trackingPref: SharedPreferences
    private lateinit var alarmAdapter: AlarmAdapter
    private val viewModel : MainViewModel by viewModels()

    private var trackingID = 0
    private var isAnAlarmActive = false

    private val swipeCallBack = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT) {
        override fun onMove(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            target: RecyclerView.ViewHolder
        ): Boolean {
            return true
        }
        override fun onSwiped(
            viewHolder: RecyclerView.ViewHolder,
            direction: Int
        ) {
            val layoutPosition = viewHolder.layoutPosition
            val swipedAlarm = alarmAdapter.alarms[layoutPosition]
            AlertDialog.Builder(this@MainActivity,R.style.alert_dialog_theme)
                .setTitle(R.string.delete_alarm_title)
                .setMessage(R.string.are_you_sure)
                .setPositiveButton(R.string.yes) { _, _ ->
                    if (isAnAlarmActive && swipedAlarm.id == trackingID) {
                        AlertDialog.Builder(this@MainActivity,R.style.alert_dialog_theme)
                            .setTitle(R.string.cant_delete)
                            .setPositiveButton(getString(R.string.ok),null)
                            .show()
                    } else {
                        viewModel.deleteAlarm(swipedAlarm)
                        alarmAdapter.notifyItemChanged(layoutPosition)
                    }
                }
                .setNegativeButton(R.string.no,null)
                .setOnDismissListener {
                    alarmAdapter.notifyItemChanged(layoutPosition)
                }
                .show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        registerLauncher()
        checkPermissions()
        initializePlaces()
        isServiceRunningInForeground(this, LocationTrackingService::class.java)

        trackingPref = getSharedPreferences("com.ozapps.geowake", MODE_PRIVATE)
        trackingID = trackingPref.getInt("tracking_alarm_id",0)

        alarmAdapter = AlarmAdapter(this)
        binding.alarmsRv.layoutManager = LinearLayoutManager(this@MainActivity)
        binding.alarmsRv.adapter = alarmAdapter

        ItemTouchHelper(swipeCallBack).attachToRecyclerView(binding.alarmsRv)

        subscribeToObservers()

        MobileAds.initialize(this)
        val adRequest = AdRequest.Builder().build()
        binding.adView.loadAd(adRequest)
    }

    private fun subscribeToObservers(){
        viewModel.alarmList.observe(this) { alarms ->
            alarmAdapter.alarms = alarms
            if (alarms.isEmpty() && !isAnAlarmActive) { // Show no alarm image
                binding.noAlarmLl.visibility = View.VISIBLE
            }
        }
        viewModel.isAlarmActive.observe(this) { isActive ->
            isAnAlarmActive = isActive
            if (isActive && trackingID == 0) { // Show unsaved alarm
                binding.activeAlarmRl.visibility = View.VISIBLE
                val alarmName = trackingPref.getString("tracking_alarm_name","")
                val distance = trackingPref.getInt("tracking_alarm_distance",500)
                alarmName?.let {
                    binding.activeAlarmNameTv.text = it.ifEmpty {
                        getString(R.string.your_destination)
                    }
                }
                binding.activeAlarmDistanceTv.text = "${distance}m"
            } else {
                binding.activeAlarmRl.visibility = View.GONE
            }
        }
    }

    fun goToMap(view: View) {
        val new = when(view.id) {
            R.id.add_alarm_fab -> 0
            R.id.active_alarm_rl -> 2
            else -> 1
        }
        val goToMap = Intent(this, MapsActivity::class.java)
            .putExtra("new",new)
        view.startAnimation(AnimationUtils.loadAnimation(this,R.anim.button_click))
        startActivity(goToMap)
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
            R.id.rate_me -> {
                val goPlayStore = Intent(Intent.ACTION_VIEW, "https://play.google.com/store/apps/details?id=$packageName".toUri())
                goPlayStore.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY or Intent.FLAG_ACTIVITY_NEW_DOCUMENT or Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
                startActivity(goPlayStore)
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun isServiceRunningInForeground(context: Context, serviceClass: Class<*>) {
        val manager = context.getSystemService(ACTIVITY_SERVICE) as ActivityManager
        for (service in manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.name == service.service.className){
                viewModel.changeActiveAlarm(true)
            }
        }
    }
    private fun initializePlaces() {
        if (!Places.isInitialized()) {
            Places.initialize(applicationContext,MAPS_API_KEY)
        }
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
        } else if (ContextCompat.checkSelfPermission(this,Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED){
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
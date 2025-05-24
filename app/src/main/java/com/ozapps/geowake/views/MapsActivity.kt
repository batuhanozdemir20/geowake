package com.ozapps.geowake.views

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.EditText
import android.widget.PopupWindow
import android.widget.RelativeLayout
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.snackbar.Snackbar
import com.ozapps.geowake.R
import com.ozapps.geowake.databinding.ActivityMapsBinding
import com.ozapps.geowake.language.BaseActivity
import com.ozapps.geowake.roomdb.LocationAlarm
import com.ozapps.geowake.service.LocationTrackingService
import com.ozapps.geowake.viewmodel.GeoWakeViewModel
import androidx.core.content.edit
import androidx.core.widget.addTextChangedListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

class MapsActivity : BaseActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityMapsBinding
    private lateinit var permissionLauncher: ActivityResultLauncher<String>

    private val viewModel : GeoWakeViewModel by viewModels<GeoWakeViewModel>()
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var serviceIntent: Intent

    private lateinit var trackingPref: SharedPreferences
    private lateinit var settingsPrefs: SharedPreferences

    private lateinit var currentAlarm: LocationAlarm
    private var markerLatLng = LatLng(0.0,0.0)
    private var defaultDistance = 100
    private var new = 0

    private var handler = Handler(Looper.getMainLooper())
    private var hint1 = true
    private var hint2 = false

    private lateinit var alarmDetailsView: View
    private lateinit var alarmSetAlert: AlertDialog.Builder

    private var mInterstitialAd: InterstitialAd? = null
    private var TAG = "GeoWakeInterstitial"

    private lateinit var buttonEnterAnim: Animation
    private lateinit var buttonClickAnim: Animation

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setTitle(R.string.app_name)

        registerLauncher()
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        serviceIntent = Intent(this, LocationTrackingService::class.java)
        trackingPref = getSharedPreferences("com.ozapps.geowake", MODE_PRIVATE)
        settingsPrefs = PreferenceManager.getDefaultSharedPreferences(this)
        defaultDistance = settingsPrefs.getString("default_distance","500")!!.toInt()
        new = intent.getIntExtra("new",0)
        hint1 = settingsPrefs.getBoolean("hint",true)
        buttonEnterAnim = AnimationUtils.loadAnimation(this,R.anim.button_enter)
        buttonClickAnim = AnimationUtils.loadAnimation(this,R.anim.button_click)
        /*
        new == 0 -> new alarm
        new == 1 -> saved alarm
        new == 2 -> active alarm
        */

        viewModel.savedAlarm.observe(this){ alarm ->
            currentAlarm = alarm
            markerLatLng = LatLng(currentAlarm.latitude,currentAlarm.longitude)
        }

        when (new) {
            0 -> { // yeni alarm
                currentAlarm = LocationAlarm(null,0.0,0.0,null)
            }
            1 -> { // kayıtlı alarm
                val id = intent.getIntExtra("alarm_id",0)
                viewModel.getAlarmById(id)
            }
            2 -> { // aktif alarm
                currentAlarm = LocationAlarm(
                    trackingPref.getString("tracking_alarm_name",null),
                    trackingPref.getFloat("tracking_alarm_latitude",0.0f).toDouble(),
                    trackingPref.getFloat("tracking_alarm_longitude",0.0f).toDouble(),
                    trackingPref.getInt("tracking_alarm_distance",defaultDistance)
                )
                markerLatLng = LatLng(currentAlarm.latitude,currentAlarm.longitude)
            }
        }

        loadInterstitialAd()

        alarmSetAlert = AlertDialog.Builder(this,R.style.alert_dialog_theme)
            .setMessage(R.string.alarm_will_set_title)
            .setPositiveButton(R.string.ok,null)
            .setOnDismissListener {
                val reset = intent
                    .putExtra("new",2)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                startActivity(reset)
                Toast.makeText(this, R.string.alarm_is_set, Toast.LENGTH_LONG).show()
                trackingPref.edit() {
                    putString("tracking_alarm_name", currentAlarm.locationName)
                    putFloat("tracking_alarm_latitude", currentAlarm.latitude.toFloat())
                    putFloat("tracking_alarm_longitude", currentAlarm.longitude.toFloat())
                    putInt("tracking_alarm_distance", currentAlarm.distance ?: defaultDistance)
                }
                serviceIntent.apply {
                    putExtra("tracking_alarm_name",currentAlarm.locationName)
                    putExtra("tracking_alarm_latitude",currentAlarm.latitude)
                    putExtra("tracking_alarm_longitude",currentAlarm.longitude)
                    putExtra("tracking_alarm_distance",currentAlarm.distance ?: defaultDistance)
                }
                ContextCompat.startForegroundService(this,serviceIntent)

                showInterstitialAdIfReady()
            }
    } // onCreate END

    @SuppressLint("MissingPermission")
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.uiSettings.setAllGesturesEnabled(false)

        if (!isLocationEnabled(this)){
            AlertDialog.Builder(this,R.style.alert_dialog_theme)
                .setTitle(R.string.open_location_title)
                .setMessage(R.string.open_location_message)
                .setPositiveButton(R.string.open_location_ok) { dialogInterface, i ->
                    val goLocationServices = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                    startActivity(goLocationServices)
                }.show()
        }
        /*
        else {
            fusedLocationProviderClient.lastLocation.addOnSuccessListener { lastLocation ->
                val lastLocationLatLng = LatLng(lastLocation.latitude,lastLocation.longitude)
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(lastLocationLatLng,15f))
            }
        }
        */
        checkPermissions {
            mMap.uiSettings.setAllGesturesEnabled(true)
            mMap.isMyLocationEnabled = true
            if (hint1) {
                handler.postDelayed({
                    showPopup(1)
                },2000)
            }
        }

        when (new) {
            0 -> { // new alarm
                mMap.uiSettings.isMapToolbarEnabled = false
                mMap.clear()
                binding.apply {
                    setAlarmBt.visibility = View.GONE
                    alarmStopFab.visibility = View.GONE
                    editStartButtons.visibility = View.GONE
                    focusMarkerIb.visibility = View.GONE
                }
            }
            1 -> { // saved alarm
                viewModel.savedAlarm.observe(this){
                    binding.apply {
                        editStartButtons.visibility = View.VISIBLE
                        focusMarkerIb.visibility = View.VISIBLE
                        deleteIb.visibility = View.VISIBLE
                    }
                    mMap.addMarker(MarkerOptions().position(markerLatLng).title(currentAlarm.locationName))
                    mMap.addCircle(
                        CircleOptions()
                            .center(markerLatLng)
                            .radius((currentAlarm.distance ?: defaultDistance).toDouble())
                            .strokeColor(Color.RED)
                            .fillColor(0x22FF0000)
                            .strokeWidth(5f)
                    )
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(markerLatLng,15f))
                }
                binding.editStartButtons.startAnimation(buttonEnterAnim)
                binding.deleteIb.startAnimation(buttonEnterAnim)
            }
            2 -> { // active alarm
                binding.alarmStopFab.visibility = View.VISIBLE
                binding.focusMarkerIb.visibility = View.VISIBLE
                mMap.addMarker(MarkerOptions().position(markerLatLng).title(currentAlarm.locationName))
                mMap.addCircle(
                    CircleOptions()
                        .center(markerLatLng)
                        .radius((currentAlarm.distance ?: defaultDistance).toDouble())
                        .strokeColor(Color.RED)
                        .fillColor(0x22FF0000)
                        .strokeWidth(5f)
                )
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(markerLatLng,15f))
                binding.alarmStopFab.startAnimation(buttonEnterAnim)

                val firstAlarm = settingsPrefs.getBoolean("first_alarm",true)
                println("first alarm: $firstAlarm")
                if (firstAlarm){
                    handler.postDelayed({
                        AlertDialog.Builder(this)
                            .setTitle(R.string.reminder_title)
                            .setMessage(R.string.reminder_message)
                            .setPositiveButton(R.string.ok,null)
                            .setOnDismissListener {
                                settingsPrefs.edit { putBoolean("first_alarm",false) }
                            }
                            .show()
                    },1500)
                }
            }
        }
        val hideTips = settingsPrefs.getBoolean("hide_tips",false)
        if (hideTips) { binding.hintIb.visibility = View.INVISIBLE }
        /*
        settingsPrefs.edit {
            remove("first_alarm")
            remove("hint")
        }*/

        mMap.setOnMapLongClickListener { markedLocation ->
            if (new == 0) {
                markerLatLng = markedLocation
                currentAlarm.latitude = markerLatLng.latitude
                currentAlarm.longitude = markerLatLng.longitude
                mMap.clear()
                mMap.addMarker(MarkerOptions().position(markerLatLng))
                binding.setAlarmBt.visibility = View.VISIBLE
                binding.setAlarmBt.startAnimation(buttonEnterAnim)
            }
        }
    }  // onMapReady END

    private fun loadInterstitialAd() {
        val adRequest = AdRequest.Builder().build()

        InterstitialAd.load(
            this,
            getString(R.string.maps_activity_ad_interstitial_id),
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    mInterstitialAd = ad
                    Log.d(TAG,"Ad was loaded.")
                }

                override fun onAdFailedToLoad(p0: LoadAdError) {
                    mInterstitialAd = null
                    Log.d(TAG,p0.toString())
                }
            }
        )
    }
    private fun showInterstitialAdIfReady(){
        var adCounter = settingsPrefs.getInt("adCounter",0)
        println("Ad Counter: $adCounter")
        if (adCounter == 2){
            if (mInterstitialAd != null){
                mInterstitialAd?.show(this)
            } else {
                Log.d(TAG,"The interstitial ad wasn't ready yet.")
                loadInterstitialAd()
            }
            adCounter = 0
        } else {
            adCounter++
        }
        settingsPrefs.edit { putInt("adCounter", adCounter) }
    }

    fun deleteAlarm(view: View){
        view.startAnimation(buttonClickAnim)
        AlertDialog.Builder(this,R.style.alert_dialog_theme)
            .setTitle(R.string.delete_alarm_title)
            .setMessage(R.string.are_you_sure)
            .setPositiveButton(R.string.yes) { _, _ ->
                viewModel.deleteAlarm(currentAlarm)
                finish()
                startActivity(Intent(this,MainActivity::class.java))
            }
            .setNegativeButton(R.string.no,null)
            .show()
    }
    fun setAlarm(view: View){
        checkPermissions {
            view.startAnimation(buttonClickAnim)
            when (view.id) {
                R.id.set_alarm_bt -> showBottomSheetDialog()
                R.id.edit_bt -> showBottomSheetDialog()
                R.id.start_bt -> alarmSetAlert.show()
            }
        }
    }
    fun stopAlarm(view: View){
        view.startAnimation(buttonClickAnim)
        AlertDialog.Builder(this,R.style.alert_dialog_theme)
            .setTitle(R.string.stop_alarm_title)
            .setMessage(R.string.stop_alarm_message)
            .setPositiveButton(R.string.yes) { dialogInterface, i ->
                Toast.makeText(this, R.string.stop_alarm_toast, Toast.LENGTH_LONG).show()
                stopService(serviceIntent)
                trackingPref.edit { clear() }
                finish()
                startActivity(Intent(this,MainActivity::class.java))
            }.setNegativeButton(R.string.no, null)
            .show()
    }
    fun focusMarker(view: View){
        view.startAnimation(buttonClickAnim)
        if (markerLatLng != LatLng(0.0,0.0)){
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(markerLatLng,15f),500,null)
        }
    }
    fun showHints(view: View){
        showPopup(1)
    }

    private fun showPopup(index: Int){
        val popupView1 = layoutInflater.inflate(R.layout.popup_1,null)
        val popupView2 = layoutInflater.inflate(R.layout.popup_2,null)
        val width = ViewGroup.LayoutParams.MATCH_PARENT
        val height = ViewGroup.LayoutParams.WRAP_CONTENT
        val focusable = true

        val enterAnim = AnimationUtils.loadAnimation(this,R.anim.popup_enter)
        val exitAnim = AnimationUtils.loadAnimation(this,R.anim.popup_exit)
        popupView1.startAnimation(enterAnim)
        popupView2.startAnimation(enterAnim)

        val touchRl = popupView1.findViewById<RelativeLayout>(R.id.touch_hint_rl)
        val markerRl = popupView1.findViewById<RelativeLayout>(R.id.marker_hint_rl)
        val stopRl = popupView1.findViewById<RelativeLayout>(R.id.stop_hint_rl)
        val startRl = popupView1.findViewById<RelativeLayout>(R.id.start_hint_rl)
        val editRl = popupView1.findViewById<RelativeLayout>(R.id.edit_hint_rl)
        val deleteRl = popupView1.findViewById<RelativeLayout>(R.id.delete_hint_rl)
        val okTv1 = popupView1.findViewById<TextView>(R.id.ok_tv)
        val okTv2 = popupView2.findViewById<TextView>(R.id.ok_tv_2)

        when (new) {
            0 -> {
                markerRl.visibility = View.GONE
                stopRl.visibility = View.GONE
                startRl.visibility = View.GONE
                editRl.visibility = View.GONE
                deleteRl.visibility = View.GONE
            }
            1 -> {
                touchRl.visibility = View.GONE
                stopRl.visibility = View.GONE
            }
            2 -> {
                touchRl.visibility = View.GONE
                startRl.visibility = View.GONE
                editRl.visibility = View.GONE
                deleteRl.visibility = View.GONE
            }
        }

        var popupWindow = PopupWindow(popupView1,width, height, focusable)

        fun dismissPopup(index: Int) {
            when (index){
                1 ->popupView1.startAnimation(exitAnim)
                2 ->popupView2.startAnimation(exitAnim)
            }
            handler.postDelayed({
                popupWindow.dismiss()
            },exitAnim.duration)
        }

        okTv1.setOnClickListener { dismissPopup(1) }
        okTv2.setOnClickListener { dismissPopup(2) }

        binding.root.post {
            when (index) {
                1 -> {
                    popupWindow = PopupWindow(popupView1,width, height, focusable)
                    popupWindow.showAtLocation(binding.mapsMain, Gravity.CENTER, 0, 0)
                    hint1 = false
                    hint2 = true
                }
                2 -> {
                    popupWindow = PopupWindow(popupView2,width, height, focusable)
                    popupWindow.showAtLocation(alarmDetailsView, Gravity.CENTER, 0, 0)
                    hint2 = false
                }
            }
        }
        settingsPrefs.edit { putBoolean("hint", false) }
    }

    @SuppressLint("UseSwitchCompatOrMaterialCode")
    private fun showBottomSheetDialog(){
        alarmDetailsView = layoutInflater.inflate(R.layout.alarm_details,null)
        alarmDetailsView.startAnimation(AnimationUtils.loadAnimation(this,R.anim.bsd_enter))
        val bottomSheetDialog = BottomSheetDialog(this,R.style.alarmDetailsDialogTheme)
        bottomSheetDialog.setContentView(alarmDetailsView)
        bottomSheetDialog.behavior.state = BottomSheetBehavior.STATE_COLLAPSED
        val locationNameET = alarmDetailsView.findViewById<EditText>(R.id.location_name_et)
        val distanceET = alarmDetailsView.findViewById<EditText>(R.id.distance_et)
        val oneTimeSW = alarmDetailsView.findViewById<Switch>(R.id.one_time_sw)
        val saveBT = alarmDetailsView.findViewById<Button>(R.id.save_bt)

        if (markerLatLng != LatLng(0.0,0.0)){
            val marker = LatLng(markerLatLng.latitude - 0.01,markerLatLng.longitude)
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(marker,14f),500,null)
        }

        if (new == 1) {
            locationNameET.setText(currentAlarm.locationName)
            currentAlarm.distance?.let {
                distanceET.setText(it.toString())
            }
            oneTimeSW.visibility = View.GONE
        }

        distanceET.addTextChangedListener {
            mMap.clear()
            mMap.addMarker(MarkerOptions().position(markerLatLng))
            if (!it.isNullOrEmpty()){
                mMap.addCircle(
                    CircleOptions()
                        .center(markerLatLng)
                        .radius(it.toString().toDouble())
                        .strokeColor(Color.RED)
                        .fillColor(0x22FF0000)
                        .strokeWidth(5f)
                )
            }

        }

        oneTimeSW.setOnCheckedChangeListener { compoundButton, ischecked ->
            if (ischecked){
                Toast.makeText(this,R.string.one_time_toast,Toast.LENGTH_LONG).show()
                saveBT.setText(R.string.save_bt_run)
            } else {
                saveBT.setText(R.string.save_bt)
            }
        }

        saveBT.setOnClickListener {
            it.startAnimation(buttonClickAnim)
            val locationName = locationNameET.text.toString()
            val distance = distanceET.text.toString().toIntOrNull()
            val isOneTime = oneTimeSW.isChecked

            currentAlarm.locationName = locationName
            currentAlarm.distance = distance

            if (!isOneTime) {
                if (locationName.isEmpty()){
                    Toast.makeText(this,R.string.enter_location_name,Toast.LENGTH_LONG).show()
                    return@setOnClickListener
                } else if (new == 1) {
                    viewModel.updateAlarm(currentAlarm)
                } else {
                    viewModel.saveAlarm(currentAlarm)
                }
            }

            alarmSetAlert.show()
            bottomSheetDialog.dismiss()
        }

        bottomSheetDialog.show()
        if (hint2) {
            handler.postDelayed({
                showPopup(2)
            },1000)
        }
    }

    private fun isLocationEnabled(context: Context): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    private fun checkPermissions(permissionGrantedFunction:() -> Unit){
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)){
                Snackbar.make(binding.root,R.string.permission_location, Snackbar.LENGTH_INDEFINITE)
                    .setAction(R.string.give_permission){
                        permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION) //request permission
                    }.show()
            } else {
                permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION) //request permission
            }
        } else if (
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.POST_NOTIFICATIONS)){
                    Snackbar.make(
                        binding.root,R.string.permission_notification, Snackbar.LENGTH_INDEFINITE
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
                            data = Uri.parse("package:$packageName")
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
                .setNegativeButton(R.string.cancel) { dialogInterface, i ->
                    finish()
                }
                .setOnDismissListener { finish() }
                .show()
        } else { //permission granted
            permissionGrantedFunction()
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
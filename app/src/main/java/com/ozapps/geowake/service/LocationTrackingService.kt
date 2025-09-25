package com.ozapps.geowake.service

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.IBinder
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.preference.PreferenceManager
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.model.LatLng
import com.ozapps.geowake.R
import com.ozapps.geowake.roomdb.LocationAlarm
import com.ozapps.geowake.views.AlarmActivity
import com.ozapps.geowake.views.MapsActivity
import androidx.core.content.edit
import com.ozapps.geowake.util.FormatDistance
import com.ozapps.geowake.repository.AlarmRepository

class LocationTrackingService : Service() {
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    private lateinit var notificationManager: NotificationManagerCompat
    private lateinit var serviceNotification: Notification
    private lateinit var alarmNotification: Notification
    private lateinit var servicePI: PendingIntent

    private lateinit var settingsPrefs: SharedPreferences
    private lateinit var currentAlarm: LocationAlarm
    private lateinit var destinationLatLng: LatLng
    private var isAlarmTriggered = false

    override fun onCreate() {
        super.onCreate()
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        settingsPrefs = PreferenceManager.getDefaultSharedPreferences(this)
        destinationLatLng = LatLng(0.0,0.0)
        currentAlarm = LocationAlarm(null,0.0,0.0,null)
        notificationManager = NotificationManagerCompat.from(this)

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)
                for (location in locationResult.locations){
                    checkProximityToDestination(location, destinationLatLng)
                }
            }
        }
        createNotification()
        startForeground(11,serviceNotification)
        requestLocationUpdates()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        currentAlarm.apply {
            locationName = intent?.getStringExtra("tracking_alarm_name")
            latitude = intent?.getDoubleExtra("tracking_alarm_latitude",0.0)!!
            longitude = intent.getDoubleExtra("tracking_alarm_longitude",0.0)
            distance = intent.getIntExtra("tracking_alarm_distance",500)
        }
        destinationLatLng = LatLng(currentAlarm.latitude,currentAlarm.longitude)
        return START_STICKY
    }

    private fun checkProximityToDestination(currentLocation: Location, destination: LatLng) {
        val currentDistance = FloatArray(1)
        Location.distanceBetween(
            currentLocation.latitude,
            currentLocation.longitude,
            destination.latitude,
            destination.longitude,
            currentDistance
        )

        AlarmRepository.updateDistance(currentDistance[0])

        updateNotification(
            currentDistance[0],
            currentAlarm.locationName?.let {
                it.ifEmpty { getString(R.string.your_destination) }
            } ?: getString(R.string.your_destination)
        )

        if (currentDistance[0] < currentAlarm.distance!! && !isAlarmTriggered) {
            isAlarmTriggered = true
            val alarmIntent = Intent(this, AlarmActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
                putExtra("location_name",currentAlarm.locationName)
            }

            val alarmType = settingsPrefs.getString("alarm_type","full_screen")
            when (alarmType) {
                "full_screen" -> startActivity(alarmIntent)
                "notification" -> {
                    if (ActivityCompat.checkSelfPermission(
                            this,
                            Manifest.permission.POST_NOTIFICATIONS
                        ) != PackageManager.PERMISSION_GRANTED
                    ) { return }
                    notificationManager.notify(2, alarmNotification)
                }
            }

            val sharedPref = getSharedPreferences("com.ozapps.geowake", MODE_PRIVATE)
            sharedPref.edit { clear() }
        }
    }

    private fun updateNotification(distance: Float, locationName: String) {
        val message = FormatDistance.metersToKm(distance)
        val notification = NotificationCompat.Builder(this,"location_service")
            .setContentTitle(locationName)
            .setContentText(message)
            .setSmallIcon(R.drawable.geowake_icon)
            .setContentIntent(servicePI)
            .setOngoing(true)
            .build()
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) { return }
        notificationManager.notify(11,notification)
    }
    private fun createNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                "location_service",
                "Location Service",
                NotificationManager.IMPORTANCE_LOW)
            val alarmChannel = NotificationChannel(
                "alarm_channel",
                "Alarm Notification",
                NotificationManager.IMPORTANCE_HIGH
            ).apply { description = "Channel for alarm notification" }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(serviceChannel)
            notificationManager.createNotificationChannel(alarmChannel)
        }


        val serviceIntent = Intent(this, MapsActivity::class.java)
            .putExtra("state",2)
        servicePI = PendingIntent.getActivity(
            this,
            1,
            serviceIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val alarmNotificationIntent = Intent(this, MapsActivity::class.java)
        val alarmNotificationPI = PendingIntent.getActivity(
            this,
            2,
            alarmNotificationIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        serviceNotification = NotificationCompat.Builder(this, "location_service")
            .setContentTitle(getString(R.string.geowake_running))
            .setContentText(getString(R.string.tracking_location))
            .setSmallIcon(R.drawable.geowake_icon)
            .setContentIntent(servicePI)
            .build()

        alarmNotification = NotificationCompat.Builder(this,"alarm_channel")
            .setContentTitle(getString(R.string.title_alarm))
            .setContentText(getString(R.string.reached_destination))
            .setSmallIcon(R.drawable.geowake_icon)
            .setContentIntent(alarmNotificationPI)
            .setPriority(Notification.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
    }

    @SuppressLint("MissingPermission")
    private fun requestLocationUpdates(){
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY,5000)
            .setMinUpdateDistanceMeters(10f)
            .setMinUpdateIntervalMillis(2000)
            .build()

        fusedLocationProviderClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            mainLooper
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        fusedLocationProviderClient.removeLocationUpdates(locationCallback)
    }
    override fun onBind(p0: Intent?): IBinder? = null
}
package com.ozapps.geowake.views

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.View
import android.view.animation.AnimationUtils
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.preference.PreferenceManager
import com.ozapps.geowake.R
import com.ozapps.geowake.databinding.ActivityAlarmBinding
import com.ozapps.geowake.databinding.ActivityMainBinding
import com.ozapps.geowake.language.BaseActivity
import com.ozapps.geowake.service.LocationTrackingService

class AlarmActivity : BaseActivity() {
    private lateinit var binding: ActivityAlarmBinding
    private lateinit var mediaPlayer: MediaPlayer
    private lateinit var vibrator: Vibrator
    private lateinit var serviceIntent: Intent

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAlarmBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        supportActionBar?.hide()

        val locationName = intent.getStringExtra("location_name")
        val distance = intent.getIntExtra("distance",200)

        val silentMode = PreferenceManager.getDefaultSharedPreferences(this).getBoolean("silent",false)

        val defaultAlarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        mediaPlayer = MediaPlayer.create(this,defaultAlarmUri)
        if (!silentMode){
            mediaPlayer.isLooping = true
            mediaPlayer.start()
        }
        vibrator = this.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (vibrator.hasVibrator() && Build.VERSION.SDK_INT >= 26){
            val vibrationEffect = VibrationEffect.createWaveform(longArrayOf(0, 500, 500, 1000),0)
            vibrator.vibrate(vibrationEffect)
        } else {
            println("This device does not a vibration motor")
        }

        serviceIntent = Intent(this, LocationTrackingService::class.java)

        val left = getString(R.string.left)
        binding.alarmMessage.text = locationName
        binding.distanceMessage.text = distance.toString() + "m " + left
    }

    fun stopAlarm(view: View){
        view.startAnimation(AnimationUtils.loadAnimation(this,R.anim.button_click))
        if (mediaPlayer.isPlaying){
            mediaPlayer.stop()
            mediaPlayer.release()
        }
        vibrator.cancel()
        stopService(serviceIntent)
        finish()
    }
}
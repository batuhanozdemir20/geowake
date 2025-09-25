package com.ozapps.geowake.views

import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.View
import android.view.animation.AnimationUtils
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.preference.PreferenceManager
import com.ozapps.geowake.R
import com.ozapps.geowake.util.FormatDistance
import com.ozapps.geowake.databinding.ActivityAlarmBinding
import com.ozapps.geowake.language.BaseActivity
import com.ozapps.geowake.service.LocationTrackingService
import com.ozapps.geowake.viewmodel.AlarmViewModel

class AlarmActivity : BaseActivity() {
    private lateinit var binding: ActivityAlarmBinding
    private lateinit var mediaPlayer: MediaPlayer
    private lateinit var vibrator: Vibrator
    private lateinit var servisIntent: Intent
    private val viewModel: AlarmViewModel by viewModels()

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
        servisIntent = Intent(this, LocationTrackingService::class.java)

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

        binding.alarmMessage.text = locationName?.let {
            it.ifEmpty { getString(R.string.your_destination) }
        } ?: getString(R.string.your_destination)

        viewModel.distanceFromService.observe(this) { distance ->
            binding.distanceTv.text = FormatDistance.metersToKmWithoutType(distance)

            binding.distanceTypeTv.text = if (distance < 1000f) {
                "m"
            } else {
                "km"
            }
        }
    }

    fun stopTheAlarm(view: View){
        view.startAnimation(AnimationUtils.loadAnimation(this,R.anim.button_click))
        if (mediaPlayer.isPlaying){
            mediaPlayer.stop()
            mediaPlayer.release()
        }
        vibrator.cancel()
        stopService(servisIntent)
        finish()
    }
}
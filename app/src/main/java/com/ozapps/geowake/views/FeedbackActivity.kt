package com.ozapps.geowake.views

import android.content.DialogInterface
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.ozapps.geowake.R
import com.ozapps.geowake.databinding.ActivityFeedbackBinding
import com.ozapps.geowake.language.BaseActivity
import com.ozapps.geowake.viewmodel.FeedbackViewModel

class FeedbackActivity : BaseActivity() {
    private lateinit var binding: ActivityFeedbackBinding
    private val viewModel: FeedbackViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFeedbackBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setTitle(R.string.give_feedback)

        viewModel.feedbackStatus.observe(this) { isSuccess ->
            if (isSuccess) {
                Toast.makeText(this,getString(R.string.thanks_feedback),Toast.LENGTH_SHORT).show()
                binding.feedbackInput.setText("")
            } else {
                Toast.makeText(this, getString(R.string.fail_feedback), Toast.LENGTH_SHORT).show()
            }
        }

        AlertDialog.Builder(this)
            .setTitle(R.string.reminder_title)
            .setMessage(R.string.internet_reminder_message)
            .setPositiveButton(getString(R.string.ok),null)
            .show()
    }

    fun submitFeedback(view: View){
        val feedbackText = binding.feedbackInput.text.toString()
        view.startAnimation(AnimationUtils.loadAnimation(this,R.anim.button_click))

        val sentAlert = AlertDialog.Builder(this)
            .setTitle(R.string.sent_feedback)
            .setMessage(R.string.are_you_sure)
            .setPositiveButton(R.string.yes) { _, _ ->
                viewModel.submitFeedback(feedbackText)
                binding.feedbackInput.text.clear()
            }.setNegativeButton(R.string.no, null)

        if (feedbackText.isEmpty()){
            Toast.makeText(this,getString(R.string.enter_feedback),Toast.LENGTH_LONG).show()
            Log.d("Feedback",getString(R.string.enter_feedback))
        } else {
            sentAlert.show()
            Log.d("Feedback",feedbackText)
        }
    }
}
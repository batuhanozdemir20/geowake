package com.ozapps.geowake.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.firebase.Firebase
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.firestore

class FeedbackViewModel(application: Application) : AndroidViewModel(application) {
    private val db = Firebase.firestore

    private val _feedbackStatus = MutableLiveData<Boolean>()
    val feedbackStatus: LiveData<Boolean> = _feedbackStatus

    fun submitFeedback(message: String) {
        val feedback = hashMapOf(
            "message" to message,
            "timestamp" to FieldValue.serverTimestamp()
        )

        db.collection("feedbacks")
            .add(feedback)
            .addOnSuccessListener {
                _feedbackStatus.postValue(true)
                println("Thanks for your feedback!")
            }
            .addOnFailureListener { e ->
                Log.w("GeoWakeViewModel","Error adding feedback", e)
                _feedbackStatus.postValue(false)
                println("Geri bildirim gönderilemedi")
            }
    }
}
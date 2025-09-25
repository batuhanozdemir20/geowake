package com.ozapps.geowake.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

object AlarmRepository {
    private val _distance = MutableLiveData<Float>()
    val distance: LiveData<Float> = _distance

    fun updateDistance(distance: Float) {
        _distance.value = distance
    }
}
package com.ozapps.geowake.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.ozapps.geowake.repository.AlarmRepository

class AlarmViewModel(): ViewModel() {
    val distanceFromService: LiveData<Float> = AlarmRepository.distance
}
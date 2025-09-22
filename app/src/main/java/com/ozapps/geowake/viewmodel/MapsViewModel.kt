package com.ozapps.geowake.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import com.google.android.gms.maps.model.LatLng
import com.ozapps.geowake.Util.AlarmState
import com.ozapps.geowake.roomdb.LocationAlarm
import com.ozapps.geowake.roomdb.LocationAlarmDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MapsViewModel(application: Application) : AndroidViewModel(application) {
    private val db = Room.databaseBuilder(
        getApplication(),
        LocationAlarmDatabase::class.java,
        "Alarms"
    ).build()
    private val alarmDao = db.locationAlarmDao()

    private val _alarm = MutableLiveData<LocationAlarm>()
    val alarm: LiveData<LocationAlarm> = _alarm
    private val _marker = MutableLiveData<LatLng>()
    val marker: LiveData<LatLng> = _marker

    private val _state = MutableLiveData<AlarmState>()
    val state: LiveData<AlarmState> = _state

    fun getAlarmById(alarmId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val alarm = alarmDao.getAlarmById(alarmId)
            alarm?.let {
                _alarm.postValue(it)
            }?: println("the alarm not found")
        }
    }

    fun getLatestAlarm() {
        viewModelScope.launch(Dispatchers.IO) {
            delay(500)
            val alarm = alarmDao.getLatestAlarm()
            _alarm.postValue(alarm)
        }
    }

    fun saveAlarm(alarm: LocationAlarm) {
        viewModelScope.launch(Dispatchers.IO) {
            alarmDao.insert(alarm)
        }
    }

    fun updateAlarm(alarm: LocationAlarm) {
        viewModelScope.launch(Dispatchers.IO) {
            alarmDao.update(alarm)
        }
    }

    fun deleteAlarm(alarm: LocationAlarm) {
        viewModelScope.launch(Dispatchers.IO) {
            alarmDao.delete(alarm)
        }
    }

    fun setCurrentAlarm(alarm: LocationAlarm) {
        _alarm.value = alarm
    }

    fun setStateFromIntent(state: Int) {
        when(state) {
            0 -> _state.value = AlarmState.NEW
            1 -> _state.value = AlarmState.SAVED
            2 -> _state.value = AlarmState.ACTIVE
        }
    }

}
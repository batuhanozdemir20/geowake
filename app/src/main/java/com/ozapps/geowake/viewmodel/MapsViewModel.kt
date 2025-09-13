package com.ozapps.geowake.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import com.ozapps.geowake.roomdb.LocationAlarm
import com.ozapps.geowake.roomdb.LocationAlarmDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MapsViewModel(application: Application) : AndroidViewModel(application) {
    private val db = Room.databaseBuilder(
        getApplication(),
        LocationAlarmDatabase::class.java,
        "Alarms"
    ).build()
    private val alarmDao = db.locationAlarmDao()

    private val _savedAlarm = MutableLiveData<LocationAlarm>()
    val savedAlarm: LiveData<LocationAlarm> = _savedAlarm
    private val _isAlarmActive = MutableLiveData<Boolean>()
    val isAlarmActive: LiveData<Boolean> = _isAlarmActive

    fun getAlarmById(alarmId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val alarm = alarmDao.getAlarmById(alarmId)
            alarm?.let {
                _savedAlarm.postValue(it)
            }?: println("the alarm not found")
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

}
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

class MainViewModel(application: Application): AndroidViewModel(application) {

    private val db = Room.databaseBuilder(
        getApplication(),
        LocationAlarmDatabase::class.java,
        "Alarms"
    ).build()
    private val alarmDao = db.locationAlarmDao()

    private val _alarmList = MutableLiveData<List<LocationAlarm>>()
    val alarmList: LiveData<List<LocationAlarm>> = _alarmList

    private val _isAlarmActive = MutableLiveData<Boolean>()
    val isAlarmActive: LiveData<Boolean> = _isAlarmActive

    init {
        getAlarms()
    }

    fun getAlarms(){
        viewModelScope.launch(Dispatchers.IO) {
            val alarms = alarmDao.getAlarms()
            _alarmList.postValue(alarms)
        }
    }

    fun deleteAlarm(alarm: LocationAlarm) {
        viewModelScope.launch(Dispatchers.IO) {
            alarmDao.delete(alarm)
            getAlarms()
        }
    }

}
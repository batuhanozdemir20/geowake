package com.ozapps.geowake.roomdb

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [LocationAlarm::class], version = 1)
abstract class LocationAlarmDatabase: RoomDatabase() {
    abstract fun locationAlarmDao(): LocationAlarmDao
}
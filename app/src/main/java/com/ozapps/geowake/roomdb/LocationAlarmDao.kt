package com.ozapps.geowake.roomdb

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface LocationAlarmDao {
    @Insert
    suspend fun insert(alarm: LocationAlarm)

    @Update
    suspend fun update(alarm: LocationAlarm)

    @Delete
    suspend fun delete(alarm: LocationAlarm)

    @Query("SELECT * FROM locationalarm")
    fun getAlarms(): List<LocationAlarm>

    @Query("SELECT * FROM locationalarm WHERE id = :id")
    fun getAlarmById(id: Int): LocationAlarm?

    @Query("SELECT * FROM locationalarm ORDER BY id DESC LIMIT 1")
    fun getLatestAlarm(): LocationAlarm
}
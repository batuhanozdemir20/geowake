package com.ozapps.geowake.roomdb

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class LocationAlarm (
    @ColumnInfo("name")
    var locationName: String?,

    @ColumnInfo("latitude")
    var latitude: Double,

    @ColumnInfo("longitude")
    var longitude: Double,

    @ColumnInfo("distance")
    var distance: Int?
) {
    @PrimaryKey(true)
    var id: Int = 0
}
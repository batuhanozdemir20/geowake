package com.ozapps.geowake.util

import kotlin.math.roundToInt

object FormatDistance {

    fun metersToKm(meters: Float): String {
        return if (meters >= 1000f) {
            val kilometers = meters / 1000f
            val roundedKm = (kilometers * 10).roundToInt() / 10f
            if (roundedKm == roundedKm.toInt().toFloat()) {
                "${roundedKm.toInt()}km"
            } else {
                "${roundedKm}km"
            }
        } else {
            "${meters.roundToInt()}m"
        }
    }

    fun metersToKmWithoutType(meters: Float): String {
        return if (meters >= 1000f) {
            val kilometers = meters / 1000f
            val roundedKm = (kilometers * 10).roundToInt() / 10f
            if (roundedKm == roundedKm.toInt().toFloat()) {
                roundedKm.toInt().toString()
            } else {
                roundedKm.toString()
            }
        } else {
            meters.roundToInt().toString()
        }
    }

}
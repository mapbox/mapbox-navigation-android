package com.mapbox.navigation.testing.utils

import com.mapbox.geojson.Point
import kotlin.math.abs

class ApproximateCoordinates(
    val latitude: Double,
    val longitude: Double,
    private val tolerance: Double,
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ApproximateCoordinates

        if (abs(latitude - other.latitude) > tolerance) return false
        if (abs(longitude - other.longitude) > tolerance) return false

        return true
    }

    override fun hashCode(): Int = 0

    override fun toString(): String {
        return "ApproximateCoordinates(" +
            "latitude=$latitude, " +
            "longitude=$longitude, " +
            "tolerance=$tolerance" +
            ")"
    }
}

fun Point.toApproximateCoordinates(tolerance: Double): ApproximateCoordinates {
    return ApproximateCoordinates(latitude(), longitude(), tolerance)
}

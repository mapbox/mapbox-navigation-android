package com.mapbox.navigation.ui.androidauto.navigation

import androidx.car.app.model.Distance
import com.mapbox.navigation.base.formatter.Rounding
import com.mapbox.navigation.base.formatter.UnitType
import com.mapbox.navigation.core.formatter.MapboxDistanceUtil
import com.mapbox.navigation.core.internal.formatter.DistanceFormattingData
import com.mapbox.navigation.core.internal.formatter.formattingData
import com.mapbox.turf.TurfConstants
import java.util.Locale

/**
 * Internal class to make the object [CarDistanceFormatter] easier to unit test.
 *
 * Rounds distances with [MapboxDistanceUtil], so the car shows the same value and unit as the
 * phone, and maps the unit and its decimals to the matching [Distance] unit.
 */
internal class CarDistanceFormatterDelegate(
    val unitType: UnitType,
    @Rounding.Increment val roundingIncrement: Int,
    val locale: Locale,
) {

    fun carDistance(distanceMeters: Double): Distance {
        val formatted = MapboxDistanceUtil.formattingData(
            distanceMeters,
            roundingIncrement,
            unitType,
            locale,
        )
        return Distance.create(formatted.distance, carDistanceUnit(formatted))
    }

    private fun carDistanceUnit(formatted: DistanceFormattingData): Int {
        val showsDecimal = formatted.fractionDigits > 0
        return when (formatted.turfDistanceUnit) {
            TurfConstants.UNIT_KILOMETERS ->
                if (showsDecimal) Distance.UNIT_KILOMETERS_P1 else Distance.UNIT_KILOMETERS
            TurfConstants.UNIT_MILES ->
                if (showsDecimal) Distance.UNIT_MILES_P1 else Distance.UNIT_MILES
            TurfConstants.UNIT_FEET -> Distance.UNIT_FEET
            TurfConstants.UNIT_YARDS -> Distance.UNIT_YARDS
            else -> Distance.UNIT_METERS
        }
    }
}

package com.mapbox.navigation.ui.androidauto.navigation

import androidx.annotation.VisibleForTesting
import androidx.car.app.model.Distance
import com.mapbox.navigation.base.formatter.Rounding
import com.mapbox.navigation.base.formatter.UnitType
import com.mapbox.turf.TurfConstants
import com.mapbox.turf.TurfConversion
import kotlin.math.roundToInt

/**
 * Internal class to make the object [CarDistanceFormatter] easier to unit test.
 */
internal class CarDistanceFormatterDelegate(
    val unitType: UnitType,
    @Rounding.Increment val roundingIncrement: Int,
) {

    fun carDistance(
        distanceMeters: Double,
    ): Distance = when (unitType) {
        UnitType.IMPERIAL -> carDistanceImperial(distanceMeters, roundingIncrement)
        UnitType.METRIC -> carDistanceMetric(distanceMeters, roundingIncrement)
    }

    private fun carDistanceImperial(distanceMeters: Double, roundingIncrement: Int): Distance {
        return when (distanceMeters) {
            !in 0.0..Double.MAX_VALUE -> {
                Distance.create(0.0, Distance.UNIT_FEET)
            }
            in 0.0..smallDistanceMeters -> {
                val roundedDistance = formatDistanceAndSuffixForSmallUnit(
                    distanceMeters,
                    roundingIncrement,
                    TurfConstants.UNIT_FEET,
                )
                Distance.create(roundedDistance.toDouble(), Distance.UNIT_FEET)
            }
            in smallDistanceMeters..mediumDistanceMeters -> {
                Distance.create(distanceMeters.metersToMiles(), Distance.UNIT_MILES_P1)
            }
            else -> {
                Distance.create(distanceMeters.metersToMiles(), Distance.UNIT_MILES)
            }
        }
    }

    private fun carDistanceMetric(distanceMeters: Double, roundingIncrement: Int): Distance {
        return when (distanceMeters) {
            !in 0.0..Double.MAX_VALUE -> {
                Distance.create(0.0, Distance.UNIT_METERS)
            }
            in 0.0..smallDistanceMeters -> {
                val roundedDistance = formatDistanceAndSuffixForSmallUnit(
                    distanceMeters,
                    roundingIncrement,
                    TurfConstants.UNIT_METERS,
                )
                Distance.create(roundedDistance.toDouble(), Distance.UNIT_METERS)
            }
            in smallDistanceMeters..mediumDistanceMeters -> {
                Distance.create(distanceMeters.metersToKilometers(), Distance.UNIT_KILOMETERS_P1)
            }
            else -> {
                Distance.create(distanceMeters.metersToKilometers(), Distance.UNIT_KILOMETERS)
            }
        }
    }

    private fun formatDistanceAndSuffixForSmallUnit(
        distance: Double,
        roundingIncrement: Int,
        roundingDistanceUnit: String,
    ): Int {
        if (distance < 0) {
            return 0
        }

        val distanceUnit = TurfConversion.convertLength(
            distance,
            TurfConstants.UNIT_METERS,
            roundingDistanceUnit,
        )

        val roundedValue = if (roundingIncrement > 0) {
            val roundedDistance = distanceUnit.roundToInt()
            if (roundedDistance < roundingIncrement) {
                roundingIncrement
            } else {
                roundedDistance / roundingIncrement * roundingIncrement
            }
        } else {
            distance.roundToInt()
        }

        return roundedValue
    }

    private fun Double.metersToMiles() = this * MILES_PER_METER
    private fun Double.metersToKilometers() = this * KILOMETERS_PER_METER

    internal companion object {
        private const val MILES_PER_METER = 0.000621371
        private const val KILOMETERS_PER_METER = 0.001

        @VisibleForTesting
        internal const val smallDistanceMeters = 400.0

        @VisibleForTesting
        internal const val mediumDistanceMeters = 10000.0
    }
}

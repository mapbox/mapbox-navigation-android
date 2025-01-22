package com.mapbox.navigation.tripdata.speedlimit.model

import com.mapbox.navigation.base.formatter.UnitType
import com.mapbox.navigation.base.speed.model.SpeedUnit
import com.mapbox.navigation.ui.base.formatter.ValueFormatter
import kotlin.math.roundToInt

/**
 * Formats posted speed limit and current speed data to [Int] for displaying to a user.
 */
class PostedAndCurrentSpeedFormatter : ValueFormatter<SpeedData, Int> {

    /**
     * Formats posted speed limit and current speed data.
     *
     * @param update a state containing [SpeedData]
     *
     * @return a formatted string
     */
    override fun format(update: SpeedData): Int {
        return getPostedSpeedLimit(update.speed, update.fromUnit, update.toUnit)
    }

    private fun getPostedSpeedLimit(
        speed: Double,
        fromUnit: SpeedUnit,
        toUnit: UnitType,
    ): Int {
        return when (fromUnit) {
            SpeedUnit.METERS_PER_SECOND -> {
                when (toUnit) {
                    UnitType.IMPERIAL -> speed.metersPerSecondToMilesPerHour()
                    UnitType.METRIC -> speed.metersPerSecondToKilometersPerHour()
                }.roundToInt()
            }
            SpeedUnit.KILOMETERS_PER_HOUR -> {
                when (toUnit) {
                    UnitType.IMPERIAL -> 5 * (speed.kilometersToMiles() / 5).roundToInt()
                    UnitType.METRIC -> speed.roundToInt()
                }
            }
            SpeedUnit.MILES_PER_HOUR -> {
                when (toUnit) {
                    UnitType.IMPERIAL -> speed.roundToInt()
                    UnitType.METRIC -> 5 * (speed.milesToKilometers() / 5).roundToInt()
                }
            }
        }
    }

    private fun Double.metersToKilometers() = this / METERS_IN_KILOMETER

    private fun Double.kilometersToMiles() = this / KILOMETERS_IN_MILE

    private fun Double.milesToKilometers() = this * KILOMETERS_IN_MILE

    private fun Double.metersToMiles() = metersToKilometers().kilometersToMiles()

    private fun Double.metersPerSecondToKilometersPerHour() = metersToKilometers() * SECONDS_IN_HOUR

    private fun Double.metersPerSecondToMilesPerHour() = metersToMiles() * SECONDS_IN_HOUR

    private companion object {
        private const val METERS_IN_KILOMETER = 1000.0
        private const val KILOMETERS_IN_MILE = 1.609
        private const val SECONDS_IN_HOUR = 3600
    }
}

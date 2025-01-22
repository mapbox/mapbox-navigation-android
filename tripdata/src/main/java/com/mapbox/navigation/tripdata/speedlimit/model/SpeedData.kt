package com.mapbox.navigation.tripdata.speedlimit.model

import com.mapbox.navigation.base.formatter.UnitType
import com.mapbox.navigation.base.speed.model.SpeedUnit

/**
 * Defines the [speed] that needs to be formatted using [PostedAndCurrentSpeedFormatter], [fromUnit]
 * [toUnit]
 *
 * @param speed can either be posted or current speed
 * @param fromUnit should be meters/sec for current speed and kilometers/hour for posted speed
 * @param toUnit can either be [UnitType.METRIC] or [UnitType.IMPERIAL]
 */
class SpeedData internal constructor(
    val speed: Double,
    val fromUnit: SpeedUnit,
    val toUnit: UnitType,
) {

    /**
     * Regenerate whenever a change is made
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SpeedData

        if (speed != other.speed) return false
        if (fromUnit != other.fromUnit) return false
        if (toUnit != other.toUnit) return false

        return true
    }

    /**
     * Regenerate whenever a change is made
     */
    override fun hashCode(): Int {
        var result = speed.hashCode()
        result = 31 * result + fromUnit.hashCode()
        result = 31 * result + toUnit.hashCode()
        return result
    }

    /**
     * Regenerate whenever a change is made
     */
    override fun toString(): String {
        return "SpeedData(speed=$speed, fromUnit=$fromUnit, toUnit=$toUnit)"
    }
}

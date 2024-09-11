package com.mapbox.navigation.core.formatter

import com.mapbox.navigation.base.formatter.UnitType

/**
 * Represents a distance that has been formatted and potentially rounded for display purposes.
 *
 * @param distance the calculated distance value
 * @param distanceAsString a rounded string representation of the distance. For example a distance value of 19.3121 might result in a string value of '19'
 * @param distanceSuffix a suffix that goes with the string value of the distance like 'km' for kilometers, 'ft' for foot/feet, 'mi' for miles etc. depending on the Locale and the UnitType
 * @param unitType indicates if the values represented are metric or imperial
 */
class FormattedDistanceData internal constructor(
    val distance: Double,
    val distanceAsString: String,
    val distanceSuffix: String,
    val unitType: UnitType,
) {

    /**
     * Indicates whether some other object is "equal to" this one.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as FormattedDistanceData

        if (distance != other.distance) return false
        if (distanceAsString != other.distanceAsString) return false
        if (distanceSuffix != other.distanceSuffix) return false
        if (unitType != other.unitType) return false

        return true
    }

    /**
     * Returns a hash code value for the object.
     */
    override fun hashCode(): Int {
        var result = distance.hashCode()
        result = 31 * result + distanceAsString.hashCode()
        result = 31 * result + distanceSuffix.hashCode()
        result = 31 * result + unitType.hashCode()
        return result
    }

    /**
     * The toString implementation
     */
    override fun toString(): String {
        return "FormattedDistanceData(" +
            "distance=$distance, " +
            "distanceAsString=$distanceAsString, " +
            "distanceSuffix=$distanceSuffix, " +
            "unitType=$unitType" +
            ")"
    }
}

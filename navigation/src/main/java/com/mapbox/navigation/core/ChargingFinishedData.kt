package com.mapbox.navigation.core

import com.mapbox.navigation.base.ExperimentalMapboxNavigationAPI

/**
 * Describes the outcome of [MapboxNavigation.stopCharging].
 *
 * @param legChanged whether the next leg was started
 */
@ExperimentalMapboxNavigationAPI
class ChargingFinishedData internal constructor(val legChanged: Boolean) {

    /**
     * Indicates whether some other object is "equal to" this one.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ChargingFinishedData

        return legChanged == other.legChanged
    }

    /**
     * Returns a hash code value for the object.
     */
    override fun hashCode(): Int {
        return legChanged.hashCode()
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "ChargingFinishedData(legChanged=$legChanged)"
    }
}

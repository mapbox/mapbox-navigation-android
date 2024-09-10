package com.mapbox.navigation.core.history.model

import com.mapbox.common.location.Location

/**
 * Represents raw locations captured by and saved in history files.
 *
 * @param eventTimestamp timestamp of event seconds
 * @param location raw location
 */
class HistoryEventUpdateLocation internal constructor(
    override val eventTimestamp: Double,
    val location: Location,
) : HistoryEvent {

    /**
     * Regenerate whenever a change is made
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as HistoryEventUpdateLocation

        if (location != other.location) return false

        return true
    }

    /**
     * Regenerate whenever a change is made
     */
    override fun hashCode(): Int {
        return location.hashCode()
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "UpdateLocationHistoryEvent(" +
            "location=$location" +
            ")"
    }
}

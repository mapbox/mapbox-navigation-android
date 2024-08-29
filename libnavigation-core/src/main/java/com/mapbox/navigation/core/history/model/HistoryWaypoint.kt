package com.mapbox.navigation.core.history.model

import com.mapbox.api.directions.v5.models.RouteLeg
import com.mapbox.geojson.Point

/**
 * Represents a waypoint in the [HistoryEventSetRoute] event.
 *
 * @param point the location coordinate for this waypoint
 * @param isSilent indicates if this waypoint created separate [RouteLeg]s
 */
class HistoryWaypoint internal constructor(
    val point: Point,
    val isSilent: Boolean,
) {
    /**
     * Regenerate whenever a change is made
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as HistoryWaypoint

        if (point != other.point) return false
        if (isSilent != other.isSilent) return false

        return true
    }

    /**
     * Regenerate whenever a change is made
     */
    override fun hashCode(): Int {
        var result = point.hashCode()
        result = 31 * result + isSilent.hashCode()
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "ActiveGuidanceOptionsWaypoint(" +
            "point=$point, " +
            "isSilent=$isSilent" +
            ")"
    }
}

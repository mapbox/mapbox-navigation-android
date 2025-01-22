package com.mapbox.navigation.tripdata.maneuver.model

import com.mapbox.navigation.base.formatter.DistanceFormatter
import com.mapbox.navigation.ui.utils.internal.ifNonNull
import kotlin.math.abs

/**
 * Data structure representing distance associated with the step. Can be either distance
 * remaining to finish the step or total step distance.
 * @property distanceFormatter DistanceFormatter to format the distance with proper units.
 * @property totalDistance Double
 * @property distanceRemaining Double
 */
class StepDistance internal constructor(
    val distanceFormatter: DistanceFormatter,
    val totalDistance: Double,
    var distanceRemaining: Double?,
) {

    /**
     * Regenerate whenever a change is made
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as StepDistance

        if (totalDistance.notEqualDelta(other.totalDistance)) return false
        if (distanceRemaining.notEqualDelta(other.distanceRemaining)) return false

        return true
    }

    /**
     * Regenerate whenever a change is made
     */
    override fun hashCode(): Int {
        var result = totalDistance.hashCode()
        result = 31 * result + distanceRemaining.hashCode()
        return result
    }

    private fun Double?.notEqualDelta(other: Double?): Boolean {
        return ifNonNull(this, other) { d1, d2 ->
            abs(d1 / d2 - 1) > 0.1
        } ?: false
    }
}

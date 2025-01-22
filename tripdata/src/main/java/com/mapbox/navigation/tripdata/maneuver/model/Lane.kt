package com.mapbox.navigation.tripdata.maneuver.model

/**
 * A simplified data structure that holds all the possible lanes for upcoming lane guidance
 * and the active direction pointing to which lane to take.
 * @property allLanes List<LaneIndicator>
 */

class Lane internal constructor(
    val allLanes: List<LaneIndicator> = listOf(),
) {

    /**
     * Regenerate whenever a change is made
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Lane

        if (allLanes != other.allLanes) return false

        return true
    }

    /**
     * Regenerate whenever a change is made
     */
    override fun hashCode(): Int {
        return allLanes.hashCode()
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "Lane(allLanes=$allLanes)"
    }
}

package com.mapbox.navigation.ui.maneuver.model

import com.mapbox.api.directions.v5.models.ManeuverModifier

/**
 * A simplified data structure that holds all the possible lanes for upcoming lane guidance
 * and the active direction pointing to which lane to take.
 * @property allLanes List<LaneIndicator>
 * @property activeDirection String? One of [ManeuverModifier]
 */

class Lane internal constructor(
    val allLanes: List<LaneIndicator> = listOf(),
    val activeDirection: String? = null
) {

    /**
     * Regenerate whenever a change is made
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Lane

        if (allLanes != other.allLanes) return false
        if (activeDirection != other.activeDirection) return false

        return true
    }

    /**
     * Regenerate whenever a change is made
     */
    override fun hashCode(): Int {
        var result = allLanes.hashCode()
        result = 31 * result + (activeDirection?.hashCode() ?: 0)
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "Lane(allLanes=$allLanes, activeDirection=$activeDirection)"
    }
}

package com.mapbox.navigation.ui.base.model.maneuver

import com.mapbox.api.directions.v5.models.ManeuverModifier

/**
 * A simplified data structure that holds all the possible lanes for upcoming lane guidance
 * and the active direction pointing to which lane to take.
 * @property allLanes List<LaneIndicator>
 * @property activeDirection String? One of [ManeuverModifier]
 */

class Lane private constructor(
    val allLanes: List<LaneIndicator>,
    val activeDirection: String?
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

    /**
     * @return builder matching the one used to create this instance
     */
    fun toBuilder(): Builder {
        return Builder()
            .allLanes(allLanes)
            .activeDirection(activeDirection)
    }

    /**
     * Build a new [Lane]
     * @property allLanes List<LaneIndicator>
     * @property activeDirection String?
     */
    class Builder {
        private var allLanes: List<LaneIndicator> = listOf()
        private var activeDirection: String? = null

        /**
         * apply allLanes to the Builder.
         * @param allLanes List<LaneIndicator>
         * @return Builder
         */
        fun allLanes(allLanes: List<LaneIndicator>): Builder =
            apply { this.allLanes = allLanes }

        /**
         * apply activeDirection to the Builder.
         * @param activeDirection String?
         * @return Builder
         */
        fun activeDirection(activeDirection: String?): Builder =
            apply { this.activeDirection = activeDirection }

        /**
         * Build the [Lane]
         * @return Lane
         */
        fun build(): Lane {
            return Lane(
                allLanes,
                activeDirection
            )
        }
    }
}

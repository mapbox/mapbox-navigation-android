package com.mapbox.navigation.ui.base.model.maneuver

import com.mapbox.api.directions.v5.models.BannerComponents

/**
 * "sub": {
 *      "components": [
 *          {
 *              "active_direction": "left",
 *              "active": true,
 *              "directions": [
 *                  "left"
 *              ],
 *              "type": "lane",
 *              "text": ""
 *          },
 *          {
 *              "active": false,
 *              "directions": [
 *                  "left"
 *              ],
 *              "type": "lane",
 *              "text": ""
 *          }
 *      ],
 *      "text": ""
 * }
 *
 * A simplified data structure containing [BannerComponents.active] and list of [BannerComponents.directions].
 * @property isActive Boolean informs whether the lane is active.
 * @property directions List<String> informs about all the possible directions a particular lane can take.
 */

class LaneIndicator private constructor(
    val isActive: Boolean,
    val directions: List<String>
) {

    /**
     * Regenerate whenever a change is made
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as LaneIndicator

        if (isActive != other.isActive) return false
        if (directions != other.directions) return false

        return true
    }

    /**
     * Regenerate whenever a change is made
     */
    override fun hashCode(): Int {
        var result = isActive.hashCode()
        result = 31 * result + directions.hashCode()
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "LaneIndicator(isActive=$isActive, directions=$directions)"
    }

    /**
     * @return builder matching the one used to create this instance
     */
    fun toBuilder(): Builder {
        return Builder()
            .isActive(isActive)
            .directions(directions)
    }

    /**
     * Build a new [LaneIndicator]
     * @property isActive Boolean
     * @property directions List<String>
     */
    class Builder {
        private var isActive: Boolean = false
        private var directions: List<String> = listOf()

        /**
         * apply isActive to the Builder.
         * @param isActive String
         * @return Builder
         */
        fun isActive(isActive: Boolean): Builder =
            apply { this.isActive = isActive }

        /**
         * apply directions to the Builder.
         * @param directions List<String>
         * @return Builder
         */
        fun directions(directions: List<String>): Builder =
            apply { this.directions = directions }

        /**
         * Build the [LaneIndicator]
         * @return LaneIndicator
         */
        fun build(): LaneIndicator {
            return LaneIndicator(
                isActive,
                directions
            )
        }
    }
}

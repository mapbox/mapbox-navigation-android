package com.mapbox.navigation.tripdata.maneuver.model

import com.mapbox.api.directions.v5.models.BannerComponents
import com.mapbox.api.directions.v5.models.BannerText
import com.mapbox.api.directions.v5.models.LegStep

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
 * @property isActive Boolean indicates if that lane can be used to complete the upcoming maneuver.
 * @property drivingSide String indicates the driving side. The value is obtained from [BannerText.drivingSide].
 * However, if null the value determination falls back to [LegStep.drivingSide]
 * @property directions List<String> informs about all the possible directions a particular lane can take.
 * @property activeDirection String shows which of the lane's [directions] is applicable to the current
 * route, when there is more than one. Only available for `mapbox/driving` profile. For other profiles
 * the activeDirection falls back to [BannerText.modifier]
 */
class LaneIndicator private constructor(
    val isActive: Boolean,
    val drivingSide: String,
    val directions: List<String>,
    val activeDirection: String? = null,
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
        if (drivingSide != other.drivingSide) return false
        if (activeDirection != other.activeDirection) return false

        return true
    }

    /**
     * Regenerate whenever a change is made
     */
    override fun hashCode(): Int {
        var result = isActive.hashCode()
        result = 31 * result + directions.hashCode()
        result = 31 * result + drivingSide.hashCode()
        result = 31 * result + activeDirection.hashCode()
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "LaneIndicator(" +
            "isActive=$isActive, " +
            "directions=$directions, " +
            "drivingSide=$drivingSide, " +
            "activeDirection=$activeDirection" +
            ")"
    }

    /**
     * @return builder matching the one used to create this instance
     */
    fun toBuilder(): Builder {
        return Builder()
            .isActive(isActive)
            .directions(directions)
            .drivingSide(drivingSide)
            .activeDirection(activeDirection)
    }

    /**
     * Build a new [LaneIndicator]
     * @property isActive Boolean
     * @property directions List<String>
     */
    class Builder {
        private var isActive: Boolean = false
        private var directions: List<String> = listOf()
        private var drivingSide: String = ""
        private var activeDirection: String? = null

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
         * apply drivingSide to the Builder.
         * @param drivingSide String
         * @return Builder
         */
        fun drivingSide(drivingSide: String): Builder =
            apply { this.drivingSide = drivingSide }

        /**
         * apply activeDirection to the Builder.
         * @param activeDirection String
         * @return Builder
         */
        fun activeDirection(activeDirection: String?): Builder =
            apply { this.activeDirection = activeDirection }

        /**
         * Build the [LaneIndicator]
         * @return LaneIndicator
         */
        fun build(): LaneIndicator {
            return LaneIndicator(
                isActive,
                drivingSide,
                directions,
                activeDirection,
            )
        }
    }
}

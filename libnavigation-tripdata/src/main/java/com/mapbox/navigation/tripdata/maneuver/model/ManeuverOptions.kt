package com.mapbox.navigation.tripdata.maneuver.model

import com.mapbox.api.directions.v5.models.BannerInstructions
import com.mapbox.navigation.tripdata.maneuver.api.MapboxManeuverApi

/**
 * Specifies options for parsing [Maneuver] data in the [MapboxManeuverApi].
 *
 * @param filterDuplicateManeuvers guidance instructions returned by the Mapbox Directions API
 * often have instructions on a route duplicated to control
 * the timing of when to notify the user about details of an upcoming maneuver.
 * For example, there can a "left turn" [BannerInstructions] available 1000m and 300m before a turn, where only the latter also contains lane information.
 * By setting this flag to `true`, you can filter out those duplicates which improves the presentation in, for example, a scrolling list.
 *
 * The [MapboxManeuverApi] will ensure that no information is lost and the current maneuver is always up-to-date.
 * It's only the upcoming duplicates that are continuously filtered out.
 *
 * This option defaults to `true`.
 */
class ManeuverOptions private constructor(
    val filterDuplicateManeuvers: Boolean,
) {

    /**
     * @return the [Builder] that created the [ManeuverOptions]
     */
    fun toBuilder(): Builder = Builder()
        .filterDuplicateManeuvers(filterDuplicateManeuvers)

    /**
     * Regenerate whenever a change is made
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ManeuverOptions

        if (filterDuplicateManeuvers != other.filterDuplicateManeuvers) return false

        return true
    }

    /**
     * Regenerate whenever a change is made
     */
    override fun hashCode(): Int {
        return filterDuplicateManeuvers.hashCode()
    }

    /**
     * Regenerate whenever a change is made
     */
    override fun toString(): String {
        return "ManeuverOptions(filterDuplicateManeuvers=$filterDuplicateManeuvers)"
    }

    /**
     * Builder of [ManeuverOptions]
     */
    class Builder {

        private var filterDuplicateManeuvers = true

        /**
         * Guidance instructions returned by the Mapbox Directions API
         * often have instructions on a route duplicated to control
         * the timing of when to notify the user about details of an upcoming maneuver.
         * For example, there can a "left turn" [BannerInstructions] available 1000m and 300m before a turn, where only the latter also contains lane information.
         * By setting this flag to `true`, you can filter out those duplicates which improves the presentation in, for example, a scrolling list.
         *
         * The [MapboxManeuverApi] will ensure that no information is lost and the current maneuver is always up-to-date.
         * It's only the upcoming duplicates that are continuously filtered out.
         *
         * This option defaults to `true`.
         *
         * @param filterDuplicateManeuvers true/false to filter duplicate maneuvers or not
         * @return Builder
         */
        fun filterDuplicateManeuvers(filterDuplicateManeuvers: Boolean): Builder = apply {
            this.filterDuplicateManeuvers = filterDuplicateManeuvers
        }

        /**
         * Build a new instance of [ManeuverOptions]
         *
         * @return ManeuverOptions
         */
        fun build() = ManeuverOptions(
            filterDuplicateManeuvers = filterDuplicateManeuvers,
        )
    }
}

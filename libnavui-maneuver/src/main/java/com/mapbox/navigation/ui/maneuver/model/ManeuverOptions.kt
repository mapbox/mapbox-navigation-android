package com.mapbox.navigation.ui.maneuver.model

/**
 * Gives options to filter duplicate maneuvers
 *
 * @param filterDuplicateManeuvers filter duplicate maneuvers if true
 */
class ManeuverOptions private constructor(
    val filterDuplicateManeuvers: Boolean
) {

    /**
     * @return the [Builder] that created the [ManeuverOptions]
     */
    fun toBuilder() = Builder()
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
         * Set to true/false to filter duplicate maneuvers or not. Default value is true
         *
         * @param filterDuplicateManeuvers Boolean
         * @return Builder
         */
        fun filterDuplicateManeuvers(filterDuplicateManeuvers: Boolean) = apply {
            this.filterDuplicateManeuvers = filterDuplicateManeuvers
        }

        /**
         * Build a new instance of [ManeuverOptions]
         *
         * @return ManeuverOptions
         */
        fun build() = ManeuverOptions(
            filterDuplicateManeuvers = filterDuplicateManeuvers
        )
    }
}

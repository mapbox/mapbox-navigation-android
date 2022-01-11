package com.mapbox.navigation.ui.maneuver.model

import com.mapbox.api.directions.v5.models.BannerInstructions
import com.mapbox.navigation.ui.maneuver.api.MapboxManeuverApi

/**
 * Specifies options for parsing [Maneuver] data in the [MapboxManeuverApi].
 *
 * @param filterDuplicateManeuvers guidance instructions returned by the Mapbox Directions API
 * often have instructions on a route duplicated to control
 * the timing of when to notify the user about details of an upcoming maneuver.
 * For example, there can a "left turn" [BannerInstructions] available 1000m and 300m before a turn, where only the latter also contains lane information.
 * By setting this flag to `true`, you can filter out those duplicates which improves the presentation in, for example, a scrolling list.
 * @param mutcdExitProperties Specify the drawables you wish to render for an [ExitComponentNode] component contained
 * in a [Maneuver]. These properties would be applied to countries following MUTCD convention
 * @param viennaExitProperties Specify the drawables you wish to render for an [ExitComponentNode] component contained
 * in a [Maneuver]. These properties would be applied to countries following VIENNA convention
 *
 * The [MapboxManeuverApi] will ensure that no information is lost and the current maneuver is always up-to-date.
 * It's only the upcoming duplicates that are continuously filtered out.
 *
 * This option defaults to `true`.
 */
class ManeuverOptions private constructor(
    val filterDuplicateManeuvers: Boolean,
    val mutcdExitProperties: MapboxExitProperties.PropertiesMutcd,
    val viennaExitProperties: MapboxExitProperties.PropertiesVienna,
) {

    /**
     * @return the [Builder] that created the [ManeuverOptions]
     */
    fun toBuilder() = Builder()
        .filterDuplicateManeuvers(filterDuplicateManeuvers)
        .mutcdExitProperties(mutcdExitProperties)
        .viennaExitProperties(viennaExitProperties)

    /**
     * Regenerate whenever a change is made
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ManeuverOptions

        if (filterDuplicateManeuvers != other.filterDuplicateManeuvers) return false
        if (mutcdExitProperties != other.mutcdExitProperties) return false
        if (viennaExitProperties != other.viennaExitProperties) return false

        return true
    }

    /**
     * Regenerate whenever a change is made
     */
    override fun hashCode(): Int {
        var result = filterDuplicateManeuvers.hashCode()
        result = 31 * result + mutcdExitProperties.hashCode()
        result = 31 * result + viennaExitProperties.hashCode()
        return result
    }

    /**
     * Regenerate whenever a change is made
     */
    override fun toString(): String {
        return "ManeuverOptions(" +
            "filterDuplicateManeuvers=$filterDuplicateManeuvers, " +
            "mutcdExitProperties=$mutcdExitProperties, " +
            "viennaExitProperties=$viennaExitProperties" +
            ")"
    }

    /**
     * Builder of [ManeuverOptions]
     */
    class Builder {

        private var filterDuplicateManeuvers = true
        private var mutcdExitProperties = MapboxExitProperties.PropertiesMutcd()
        private var viennaExitProperties = MapboxExitProperties.PropertiesVienna()

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
        fun filterDuplicateManeuvers(filterDuplicateManeuvers: Boolean) = apply {
            this.filterDuplicateManeuvers = filterDuplicateManeuvers
        }

        /**
         * Specify the drawables you wish to render for an [ExitComponentNode] component contained
         * in a [Maneuver]. These properties would be applied to countries following MUTCD
         * convention
         *
         * @param mutcdExitProperties settings to exit properties
         * @return Builder
         */
        fun mutcdExitProperties(
            mutcdExitProperties: MapboxExitProperties.PropertiesMutcd
        ) = apply {
            this.mutcdExitProperties = mutcdExitProperties
        }

        /**
         * Specify the drawables you wish to render for an [ExitComponentNode] component contained
         * in a [Maneuver]. These properties would be applied to countries following MUTCD
         * convention
         *
         * @param viennaExitProperties settings to exit properties
         * @return Builder
         */
        fun viennaExitProperties(
            viennaExitProperties: MapboxExitProperties.PropertiesVienna
        ) = apply {
            this.viennaExitProperties = viennaExitProperties
        }

        /**
         * Build a new instance of [ManeuverOptions]
         *
         * @return ManeuverOptions
         */
        fun build() = ManeuverOptions(
            filterDuplicateManeuvers = filterDuplicateManeuvers,
            mutcdExitProperties = mutcdExitProperties,
            viennaExitProperties = viennaExitProperties
        )
    }
}

package com.mapbox.navigation.ui.maneuver.model

import com.mapbox.navigation.ui.maneuver.view.MapboxPrimaryManeuver
import com.mapbox.navigation.ui.maneuver.view.MapboxSecondaryManeuver
import com.mapbox.navigation.ui.maneuver.view.MapboxSubManeuver

/**
 * Specifies options for rendering different components in a [Maneuver].
 *
 * @param primaryManeuverOptions to style [MapboxPrimaryManeuver]
 * @param secondaryManeuverOptions to style [MapboxSecondaryManeuver]
 * @param subManeuverOptions to style [MapboxSubManeuver]
 */
class ManeuverViewOptions private constructor(
    val primaryManeuverOptions: ManeuverPrimaryOptions,
    val secondaryManeuverOptions: ManeuverSecondaryOptions,
    val subManeuverOptions: ManeuverSubOptions
) {

    /**
     * @return the [Builder] that created the [ManeuverViewOptions]
     */
    fun toBuilder() = Builder()
        .primaryManeuverOptions(primaryManeuverOptions)
        .secondaryManeuverOptions(secondaryManeuverOptions)
        .subManeuverOptions(subManeuverOptions)

    /**
     * Regenerate whenever a change is made
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ManeuverViewOptions

        if (primaryManeuverOptions != other.primaryManeuverOptions) return false
        if (secondaryManeuverOptions != other.secondaryManeuverOptions) return false
        if (subManeuverOptions != other.subManeuverOptions) return false

        return true
    }

    /**
     * Regenerate whenever a change is made
     */
    override fun hashCode(): Int {
        var result = primaryManeuverOptions.hashCode()
        result = 31 * result + secondaryManeuverOptions.hashCode()
        result = 31 * result + subManeuverOptions.hashCode()
        return result
    }

    /**
     * Regenerate whenever a change is made
     */
    override fun toString(): String {
        return "ManeuverViewOptions(" +
            "primaryManeuverOptions=$primaryManeuverOptions, " +
            "secondaryManeuverOptions=$secondaryManeuverOptions, " +
            "subManeuverOptions=$subManeuverOptions" +
            ")"
    }

    /**
     * Builder of [ManeuverViewOptions]
     */
    class Builder {

        private var primaryManeuverOptions = ManeuverPrimaryOptions.Builder().build()
        private var secondaryManeuverOptions = ManeuverSecondaryOptions.Builder().build()
        private var subManeuverOptions = ManeuverSubOptions.Builder().build()

        /**
         * Allows you to style [MapboxPrimaryManeuver].
         *
         * @param primaryManeuverOptions text settings
         * @return Builder
         */
        fun primaryManeuverOptions(primaryManeuverOptions: ManeuverPrimaryOptions) = apply {
            this.primaryManeuverOptions = primaryManeuverOptions
        }

        /**
         * Allows you to style [MapboxSecondaryManeuver].
         *
         * @param secondaryManeuverOptions text settings
         * @return Builder
         */
        fun secondaryManeuverOptions(secondaryManeuverOptions: ManeuverSecondaryOptions) = apply {
            this.secondaryManeuverOptions = secondaryManeuverOptions
        }

        /**
         * Allows you to style [MapboxSubManeuver].
         *
         * @param subManeuverOptions text settings
         * @return Builder
         */
        fun subManeuverOptions(subManeuverOptions: ManeuverSubOptions) = apply {
            this.subManeuverOptions = subManeuverOptions
        }

        /**
         * Build a new instance of [ManeuverViewOptions]
         *
         * @return ManeuverOptions
         */
        fun build() = ManeuverViewOptions(
            primaryManeuverOptions = primaryManeuverOptions,
            secondaryManeuverOptions = secondaryManeuverOptions,
            subManeuverOptions = subManeuverOptions
        )
    }
}

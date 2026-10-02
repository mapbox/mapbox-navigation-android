package com.mapbox.navigation.ui.androidauto.ui.maneuver.model

import androidx.annotation.StyleRes
import androidx.core.widget.TextViewCompat

/**
 * Specifies options for styling [MapboxExitText].
 *
 * @param textAppearance change the text appearance of exit text.
 * @see [TextViewCompat.setTextAppearance]
 * @param mutcdExitProperties defines the appearance of the exit drawables for countries following
 * MUTCD convention
 * @param viennaExitProperties defines the appearance of the exit drawables for countries following
 * VIENNA convention
 */
class ManeuverExitOptions private constructor(
    @StyleRes val textAppearance: Int,
    val mutcdExitProperties: MapboxExitProperties.PropertiesMutcd,
    val viennaExitProperties: MapboxExitProperties.PropertiesVienna,
) {

    /**
     * @return the [Builder] that created the [ManeuverExitOptions]
     */
    fun toBuilder() = Builder()
        .textAppearance(textAppearance)
        .mutcdExitProperties(mutcdExitProperties)
        .viennaExitProperties(viennaExitProperties)

    /**
     * Regenerate whenever a change is made
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ManeuverExitOptions

        if (textAppearance != other.textAppearance) return false
        if (mutcdExitProperties != other.mutcdExitProperties) return false
        if (viennaExitProperties != other.viennaExitProperties) return false

        return true
    }

    /**
     * Regenerate whenever a change is made
     */
    override fun hashCode(): Int {
        var result = textAppearance.hashCode()
        result = 31 * result + mutcdExitProperties.hashCode()
        result = 31 * result + viennaExitProperties.hashCode()
        return result
    }

    /**
     * Regenerate whenever a change is made
     */
    override fun toString(): String {
        return "ManeuverExitOptions(" +
            "textAppearance=$textAppearance, " +
            "mutcdExitProperties=$mutcdExitProperties, " +
            "viennaExitProperties=$viennaExitProperties" +
            ")"
    }

    /**
     * Builder of [ManeuverExitOptions]
     */
    class Builder {

        private var textAppearance = 0
        private var mutcdExitProperties = MapboxExitProperties.PropertiesMutcd()
        private var viennaExitProperties = MapboxExitProperties.PropertiesVienna()

        /**
         * Allows you to change the text appearance of [PrimaryManeuver], [SecondaryManeuver]
         * and [SubManeuver].
         *
         * @see [TextViewCompat.setTextAppearance]
         * @param textAppearance text settings
         * @return Builder
         */
        fun textAppearance(@StyleRes textAppearance: Int): Builder = apply {
            this.textAppearance = textAppearance
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
            mutcdExitProperties: MapboxExitProperties.PropertiesMutcd,
        ): Builder = apply {
            this.mutcdExitProperties = mutcdExitProperties
        }

        /**
         * Specify the drawables you wish to render for an [ExitComponentNode] component contained
         * in a [Maneuver]. These properties would be applied to countries following VIENNA
         * convention
         *
         * @param viennaExitProperties settings to exit properties
         * @return Builder
         */
        fun viennaExitProperties(
            viennaExitProperties: MapboxExitProperties.PropertiesVienna,
        ): Builder = apply {
            this.viennaExitProperties = viennaExitProperties
        }

        /**
         * Build a new instance of [ManeuverExitOptions]
         *
         * @return ManeuverExitOptions
         */
        fun build() = ManeuverExitOptions(
            textAppearance = textAppearance,
            mutcdExitProperties = mutcdExitProperties,
            viennaExitProperties = viennaExitProperties,
        )
    }
}

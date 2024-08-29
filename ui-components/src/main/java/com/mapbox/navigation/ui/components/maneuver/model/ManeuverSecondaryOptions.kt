package com.mapbox.navigation.ui.components.maneuver.model

import androidx.annotation.StyleRes
import androidx.core.widget.TextViewCompat
import com.mapbox.navigation.ui.components.R

/**
 * Specifies options for styling [MapboxSecondaryManeuver].
 *
 * @param textAppearance change the text appearance of secondary maneuver.
 * @see [TextViewCompat.setTextAppearance]
 * @param exitOptions options to style [MapboxExitText] in [MapboxSecondaryManeuver]
 */
class ManeuverSecondaryOptions private constructor(
    @StyleRes val textAppearance: Int,
    val exitOptions: ManeuverExitOptions,
) {

    /**
     * @return the [Builder] that created the [ManeuverSecondaryOptions]
     */
    fun toBuilder(): Builder = Builder()
        .textAppearance(textAppearance)
        .exitOptions(exitOptions)

    /**
     * Regenerate whenever a change is made
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ManeuverSecondaryOptions

        if (textAppearance != other.textAppearance) return false
        if (exitOptions != other.exitOptions) return false

        return true
    }

    /**
     * Regenerate whenever a change is made
     */
    override fun hashCode(): Int {
        var result = textAppearance.hashCode()
        result = 31 * result + exitOptions.hashCode()
        return result
    }

    /**
     * Regenerate whenever a change is made
     */
    override fun toString(): String {
        return "ManeuverSecondaryOptions(" +
            "textAppearance=$textAppearance, " +
            "exitOptions=$exitOptions" +
            ")"
    }

    /**
     * Builder of [ManeuverSecondaryOptions]
     */
    class Builder {

        private var textAppearance = R.style.MapboxStyleSecondaryManeuver
        private var exitOptions = ManeuverExitOptions
            .Builder()
            .textAppearance(R.style.MapboxStyleExitTextForSecondary)
            .build()

        /**
         * Allows you to change the text appearance of [SecondaryManeuver]
         *
         * @see [TextViewCompat.setTextAppearance]
         * @param textAppearance text settings
         * @return Builder
         */
        fun textAppearance(@StyleRes textAppearance: Int): Builder = apply {
            this.textAppearance = textAppearance
        }

        /**
         * Allows you to specify the options for styling of [MapboxExitText] in
         * [MapboxSecondaryManeuver]
         *
         * @param exitOptions settings to exit properties
         * @return Builder
         */
        fun exitOptions(exitOptions: ManeuverExitOptions): Builder = apply {
            this.exitOptions = exitOptions
        }

        /**
         * Build a new instance of [ManeuverSecondaryOptions]
         *
         * @return ManeuverSecondaryOptions
         */
        fun build() = ManeuverSecondaryOptions(
            textAppearance = textAppearance,
            exitOptions = exitOptions,
        )
    }
}

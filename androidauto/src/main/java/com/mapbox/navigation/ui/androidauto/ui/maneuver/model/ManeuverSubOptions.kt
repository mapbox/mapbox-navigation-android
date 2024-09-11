package com.mapbox.navigation.ui.androidauto.ui.maneuver.model

import androidx.annotation.StyleRes
import androidx.core.widget.TextViewCompat
import com.mapbox.navigation.ui.androidauto.R

/**
 * Specifies options for styling [MapboxSubManeuver].
 *
 * @param textAppearance change the text appearance of sub maneuver.
 * @see [TextViewCompat.setTextAppearance]
 * @param exitOptions options to style [MapboxExitText] in [MapboxSubManeuver]
 */
class ManeuverSubOptions private constructor(
    @StyleRes val textAppearance: Int,
    val exitOptions: ManeuverExitOptions,
) {

    /**
     * @return the [Builder] that created the [ManeuverSubOptions]
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

        other as ManeuverSubOptions

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
        return "ManeuverSubOptions(" +
            "textAppearance=$textAppearance, " +
            "exitOptions=$exitOptions" +
            ")"
    }

    /**
     * Builder of [ManeuverSubOptions]
     */
    class Builder {

        private var textAppearance = R.style.MapboxStyleSubManeuver
        private var exitOptions = ManeuverExitOptions.Builder()
            .textAppearance(R.style.MapboxStyleExitTextForSub)
            .build()

        /**
         * Allows you to change the text appearance of [SubManeuver]
         *
         * @see [TextViewCompat.setTextAppearance]
         * @param textAppearance text settings
         * @return Builder
         */
        fun textAppearance(@StyleRes textAppearance: Int): Builder = apply {
            this.textAppearance = textAppearance
        }

        /**
         * Allows you to specify the options for styling of [MapboxExitText] in [MapboxSubManeuver]
         *
         * @param exitOptions settings to exit properties
         * @return Builder
         */
        fun exitOptions(exitOptions: ManeuverExitOptions): Builder = apply {
            this.exitOptions = exitOptions
        }

        /**
         * Build a new instance of [ManeuverSubOptions]
         *
         * @return ManeuverSecondaryOptions
         */
        fun build() = ManeuverSubOptions(
            textAppearance = textAppearance,
            exitOptions = exitOptions,
        )
    }
}

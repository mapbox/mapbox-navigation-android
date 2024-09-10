package com.mapbox.navigation.ui.androidauto.ui.maneuver.model

import androidx.annotation.StyleRes
import androidx.core.widget.TextViewCompat
import com.mapbox.navigation.ui.androidauto.R

/**
 * Specifies options for styling [MapboxPrimaryManeuver].
 *
 * @param textAppearance change the text appearance of primary maneuver.
 * @see [TextViewCompat.setTextAppearance]
 * @param exitOptions options to style [MapboxExitText] in [MapboxPrimaryManeuver]
 */
class ManeuverPrimaryOptions private constructor(
    @StyleRes val textAppearance: Int,
    val exitOptions: ManeuverExitOptions,
) {

    /**
     * @return the [Builder] that created the [ManeuverPrimaryOptions]
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

        other as ManeuverPrimaryOptions

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
        return "ManeuverPrimaryOptions(" +
            "textAppearance=$textAppearance, " +
            "exitOptions=$exitOptions" +
            ")"
    }

    /**
     * Builder of [ManeuverPrimaryOptions]
     */
    class Builder {

        private var textAppearance = R.style.MapboxStylePrimaryManeuver
        private var exitOptions = ManeuverExitOptions
            .Builder()
            .textAppearance(R.style.MapboxStyleExitTextForPrimary)
            .build()

        /**
         * Allows you to change the text appearance of [PrimaryManeuver]
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
         * [MapboxPrimaryManeuver]
         *
         * @param exitOptions settings to exit properties
         * @return Builder
         */
        fun exitOptions(exitOptions: ManeuverExitOptions): Builder = apply {
            this.exitOptions = exitOptions
        }

        /**
         * Build a new instance of [ManeuverPrimaryOptions]
         *
         * @return ManeuverPrimaryOptions
         */
        fun build() = ManeuverPrimaryOptions(
            textAppearance = textAppearance,
            exitOptions = exitOptions,
        )
    }
}

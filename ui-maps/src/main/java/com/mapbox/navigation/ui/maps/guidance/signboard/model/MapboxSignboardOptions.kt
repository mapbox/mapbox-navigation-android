package com.mapbox.navigation.ui.maps.guidance.signboard.model

import com.mapbox.navigation.ui.maps.guidance.signboard.api.MapboxSignboardApi

/**
 * A class that allows you to control style of signboards that will be generated using
 * [MapboxSignboardApi]
 * @property cssStyles String used to specify styling rules for the signboard svg. If not specified
 * it uses a default style.
 * @property desiredSignboardWidth Int used to calculate the height to maintain the aspect ratio.
 * If not specified it defaults to 600px.
 * @constructor
 */
class MapboxSignboardOptions private constructor(
    val cssStyles: String,
    val desiredSignboardWidth: Int,
) {

    /**
     * @return builder matching the one used to create this instance
     */
    fun toBuilder(): Builder = Builder().also {
        it.cssStyles(cssStyles)
        it.desiredSignboardWidth(desiredSignboardWidth)
    }

    /**
     * Regenerate whenever a change is made
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MapboxSignboardOptions

        if (cssStyles != other.cssStyles) return false
        if (desiredSignboardWidth != other.desiredSignboardWidth) return false

        return true
    }

    /**
     * Regenerate whenever a change is made
     */
    override fun hashCode(): Int {
        var result = cssStyles.hashCode()
        result = 31 * result + desiredSignboardWidth.hashCode()
        return result.hashCode()
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "MapboxSignboardOptions(" +
            "cssStyles=$cssStyles, " +
            "desiredSignboardWidth=$desiredSignboardWidth" +
            ")"
    }

    /**
     * Build a new [MapboxSignboardOptions]
     * @property cssStyles String builder for stylesheet for svg
     * @property desiredSignboardWidth builder for desired signboard width
     */
    class Builder {

        private var cssStyles: String =
            "text { font-family: Arial, Helvetica, sans-serif; font-size: 0.8em }"
        private var desiredSignboardWidth: Int = 600

        /**
         * apply css styles to the builder
         * @param cssStyles String
         * @return Builder
         */
        fun cssStyles(cssStyles: String): Builder =
            apply { this.cssStyles = cssStyles }

        /**
         * apply desired width of signboard to the builder
         * @param desiredSignboardWidth Int
         * @return Builder
         */
        fun desiredSignboardWidth(desiredSignboardWidth: Int): Builder =
            apply { this.desiredSignboardWidth = desiredSignboardWidth }

        /**
         * Build the [MapboxSignboardOptions]
         */
        fun build(): MapboxSignboardOptions {
            return MapboxSignboardOptions(
                cssStyles = cssStyles,
                desiredSignboardWidth = desiredSignboardWidth,
            )
        }
    }
}

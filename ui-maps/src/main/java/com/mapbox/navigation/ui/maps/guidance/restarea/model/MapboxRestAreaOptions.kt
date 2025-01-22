package com.mapbox.navigation.ui.maps.guidance.restarea.model

import com.mapbox.navigation.ui.maps.guidance.restarea.api.MapboxRestAreaApi

/**
 * A class that allows you to control style of service/parking area guide map that will be generated
 * using [MapboxRestAreaApi]
 * @property desiredGuideMapWidth Int used to calculate the height to maintain the aspect ratio.
 * @constructor
 */
class MapboxRestAreaOptions private constructor(
    val desiredGuideMapWidth: Int,
) {

    /**
     * @return builder matching the one used to create this instance
     */
    fun toBuilder(): Builder = Builder().also {
        it.desiredGuideMapWidth(desiredGuideMapWidth)
    }

    /**
     * Regenerate whenever a change is made
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MapboxRestAreaOptions

        if (desiredGuideMapWidth != other.desiredGuideMapWidth) return false

        return true
    }

    /**
     * Regenerate whenever a change is made
     */
    override fun hashCode(): Int {
        return desiredGuideMapWidth.hashCode()
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "MapboxRestAreaOptions(" +
            "desiredGuideMapWidth=$desiredGuideMapWidth" +
            ")"
    }

    /**
     * Build a new [MapboxRestAreaOptions]
     * @property desiredGuideMapWidth builder for desired sapa guide map width
     */
    class Builder {

        private var desiredGuideMapWidth: Int = 600

        /**
         * apply desired width of service/parking area guide map in pixels to the builder
         * @param desiredGuideMapWidth Int
         * @return Builder
         */
        fun desiredGuideMapWidth(desiredGuideMapWidth: Int): Builder =
            apply { this.desiredGuideMapWidth = desiredGuideMapWidth }

        /**
         * Build the [MapboxRestAreaOptions]
         */
        fun build(): MapboxRestAreaOptions {
            return MapboxRestAreaOptions(
                desiredGuideMapWidth = desiredGuideMapWidth,
            )
        }
    }
}

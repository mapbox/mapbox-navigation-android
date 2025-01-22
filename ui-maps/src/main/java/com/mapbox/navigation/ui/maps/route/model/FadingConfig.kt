package com.mapbox.navigation.ui.maps.route.model

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI

/**
 * Class used to configure fading of a map object based on zoom level.
 *
 * @param startFadingZoom zoom level when the fading out should start (meaning the object is still fully visible)
 * @param finishFadingZoom zoom level when the fading out should end (meaning the object is not visible)
 */
@ExperimentalPreviewMapboxNavigationAPI
class FadingConfig private constructor(
    val startFadingZoom: Double,
    val finishFadingZoom: Double,
) {

    /**
     * Indicates whether some other object is "equal to" this one.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as FadingConfig

        if (startFadingZoom != other.startFadingZoom) return false
        if (finishFadingZoom != other.finishFadingZoom) return false

        return true
    }

    /**
     * Returns a hash code value for the object.
     */
    override fun hashCode(): Int {
        var result = startFadingZoom.hashCode()
        result = 31 * result + finishFadingZoom.hashCode()
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "FadingConfig(" +
            "startFadingZoom=$startFadingZoom, " +
            "finishFadingZoom=$finishFadingZoom" +
            ")"
    }

    /**
     * A builder used to create an instance of [FadingConfig].
     *
     * @param startFadingZoom zoom level when the fading out should start (meaning the object is still fully visible)
     * @param finishFadingZoom zoom level when the fading out should end (meaning the object is not visible)
     */
    @ExperimentalPreviewMapboxNavigationAPI
    class Builder(
        private val startFadingZoom: Double,
        private val finishFadingZoom: Double,
    ) {

        /**
         * Creates an instance of [FadingConfig].
         *
         * @return [FadingConfig] object
         */
        fun build(): FadingConfig {
            return FadingConfig(startFadingZoom, finishFadingZoom)
        }
    }
}

package com.mapbox.navigation.ui.maps.route.line.model

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI

/**
 * Configuration for rendering a dashed route line.
 *
 * @param dashLength the length of each dash, in line-width units
 * @param dashGap the length of the gap between dashes, in line-width units
 */
@ExperimentalPreviewMapboxNavigationAPI
class LineDashConfig private constructor(
    val dashLength: Double,
    val dashGap: Double,
) {

    /**
     * Returns a list alternating [dashLength] and [dashGap], as expected by
     * `LineLayer.lineDasharray`.
     */
    internal fun toList(): List<Double> = listOf(dashLength, dashGap)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as LineDashConfig

        if (dashLength != other.dashLength) return false
        if (dashGap != other.dashGap) return false

        return true
    }

    override fun hashCode(): Int {
        var result = dashLength.hashCode()
        result = 31 * result + dashGap.hashCode()
        return result
    }

    override fun toString(): String {
        return "LineDashConfig(dashLength=$dashLength, dashGap=$dashGap)"
    }

    /**
     * A builder used to create an instance of [LineDashConfig].
     */
    @ExperimentalPreviewMapboxNavigationAPI
    class Builder {
        private var dashLength: Double = 0.0
        private var dashGap: Double = 0.0

        /**
         * The length of each dash, in line-width units.
         *
         * @param length dash length
         */
        fun dashLength(length: Double): Builder = apply {
            dashLength = length
        }

        /**
         * The length of the gap between dashes, in line-width units.
         *
         * @param gap dash gap length
         */
        fun dashGap(gap: Double): Builder = apply {
            dashGap = gap
        }

        /**
         * Creates an instance of [LineDashConfig].
         *
         * @return [LineDashConfig] object
         */
        fun build(): LineDashConfig = LineDashConfig(dashLength, dashGap)
    }
}

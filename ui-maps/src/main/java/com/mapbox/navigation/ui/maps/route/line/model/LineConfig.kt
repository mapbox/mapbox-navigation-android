package com.mapbox.navigation.ui.maps.route.line.model

import com.mapbox.maps.extension.style.layers.properties.generated.LineCap
import com.mapbox.maps.extension.style.layers.properties.generated.LineJoin
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI

/**
 * Configuration for rendering a route line.
 *
 * @param lineDashConfig Configuration for rendering a dashed route line. `null` to use solid line.
 * @param lineCap the line cap applied to the routes [LineLayer]. Defaults to [LineCap.ROUND].
 * @param lineJoin the line join applied to the routes [LineLayer]. Defaults to [LineJoin.ROUND].
 */
@ExperimentalPreviewMapboxNavigationAPI
class LineConfig private constructor(
    val lineDashConfig: LineDashConfig?,
    val lineCap: LineCap,
    val lineJoin: LineJoin,
) {

    /**
     * Returns a list alternating [dashLength] and [dashGap], as expected by
     * `LineLayer.lineDasharray`.
     */
    internal fun toDashArray(): List<Double> = (lineDashConfig?.toList() ?: emptyList())

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as LineConfig

        if (lineDashConfig != other.lineDashConfig) return false
        if (lineCap != other.lineCap) return false
        if (lineJoin != other.lineJoin) return false

        return true
    }

    override fun hashCode(): Int {
        var result = lineDashConfig?.hashCode() ?: 0
        result = 31 * result + lineCap.hashCode()
        result = 31 * result + lineJoin.hashCode()
        return result
    }

    override fun toString(): String {
        return "LineConfig(lineDashConfig=$lineDashConfig, " +
            "lineCap=$lineCap, " +
            "lineJoin=$lineJoin)"
    }

    /**
     * A builder used to create an instance of [LineConfig].
     */
    @ExperimentalPreviewMapboxNavigationAPI
    class Builder {
        private var lineDashConfig: LineDashConfig? = null
        private var lineCap: LineCap = LineCap.ROUND
        private var lineJoin: LineJoin = LineJoin.ROUND

        /**
         * The length of each dash, in line-width units.
         *
         */
        fun lineDashConfig(lineDashConfig: LineDashConfig?): Builder = apply {
            this.lineDashConfig = lineDashConfig
        }

        /**
         * The line cap applied to the [LineLayer]. Defaults to [LineCap.ROUND].
         *
         * @param lineCap line cap
         * @return the builder
         */
        fun lineCap(lineCap: LineCap): Builder = apply {
            this.lineCap = lineCap
        }

        /**
         * The line join applied to the [LineLayer]. Defaults to [LineJoin.ROUND].
         *
         * @param lineJoin line join
         * @return the builder
         */
        fun lineJoin(lineJoin: LineJoin): Builder = apply {
            this.lineJoin = lineJoin
        }

        /**
         * Creates an instance of [LineConfig].
         *
         * @return [LineConfig] object
         */
        fun build(): LineConfig = LineConfig(
            lineDashConfig,
            lineCap,
            lineJoin,
        )
    }
}

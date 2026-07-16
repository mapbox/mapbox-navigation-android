package com.mapbox.navigation.ui.maps.route.line.model

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI

/**
 * Class which is wrapping different layers line configurations.
 *
 * @param lineConfig configuration applied to route lines
 */
@ExperimentalPreviewMapboxNavigationAPI
class LineLayersConfigs private constructor(
    val lineConfig: LineConfig,
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as LineLayersConfigs

        if (lineConfig != other.lineConfig) return false

        return true
    }

    override fun hashCode(): Int {
        var result = lineConfig.hashCode()
        return result
    }

    override fun toString(): String {
        return "LineLayersConfigs(" +
            "lineConfig=$lineConfig, " +
            ")"
    }

    /**
     * A builder used to create an instance of [LineLayersConfigs].
     */
    @ExperimentalPreviewMapboxNavigationAPI
    class Builder {
        private var lineConfig: LineConfig = LineConfig.Builder().build()

        /**
         * The configuration which will be applied to the route lines [LineLayer].
         *
         * @param config route line config
         */
        fun lineConfig(config: LineConfig): Builder = apply {
            this.lineConfig = config
        }

        /**
         * Creates an instance of [LineLayersConfigs].
         *
         * @return [LineLayersConfigs] object
         */
        fun build(): LineLayersConfigs = LineLayersConfigs(
            lineConfig,
        )
    }
}

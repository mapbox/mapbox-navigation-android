package com.mapbox.navigation.core.fasterroute

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI

@ExperimentalPreviewMapboxNavigationAPI
data class FasterRouteOptions internal constructor(
    val maxAcceptableGeometrySimilarityToRejectedAlternatives: Double
) {
    class Builder {

        private var maxGeometrySimilarityToRejectedAlternatives = 0.5

        fun maxAcceptableGeometrySimilarityToRejectedAlternatives(value: Double): Builder {
            assert(value in 0.0..1.0) { "similarity should be a value between 0 and 1" }
            maxGeometrySimilarityToRejectedAlternatives = value
            return this
        }

        fun build() = FasterRouteOptions(maxGeometrySimilarityToRejectedAlternatives)
    }
}

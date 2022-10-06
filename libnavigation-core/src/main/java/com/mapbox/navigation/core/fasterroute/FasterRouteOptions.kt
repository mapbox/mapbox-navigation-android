package com.mapbox.navigation.core.fasterroute

import com.mapbox.navigation.base.ExperimentalMapboxNavigationAPI

@ExperimentalMapboxNavigationAPI
class FasterRouteOptions internal constructor(
    val maxGeometrySimilarityToRejectedAlternatives: Double
) {
    class Builder {

        private var maxGeometrySimilarityToRejectedAlternatives = 0.5

        fun maxGeometrySimilarityToRejectedAlternatives(value: Double) {
            assert(value in 0.0..1.0) { "similarity should be a value between 0 and 1" }
            maxGeometrySimilarityToRejectedAlternatives = value
        }

        fun build() = FasterRouteOptions(maxGeometrySimilarityToRejectedAlternatives)
    }
}

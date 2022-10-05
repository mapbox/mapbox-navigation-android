package com.mapbox.navigation.core.fasterroute

import com.mapbox.navigation.base.ExperimentalMapboxNavigationAPI

@ExperimentalMapboxNavigationAPI
class FasterRouteOptions internal constructor(
    val maxSimilarityToExistingRoute: Double
) {
    class Builder {

        private val maxSimilarityToExistingRoute = 0.5

        fun maxSimilarityToExistingRoute(value: Double) {
            assert(value in 0.0..1.0) { "similarity should be a value between 0 and 1" }
        }

        fun build() = FasterRouteOptions(maxSimilarityToExistingRoute)
    }
}


